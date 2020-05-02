package pt.tecnico.sauron.silo;

import io.grpc.*;
import pt.tecnico.sauron.silo.contract.exceptions.InvalidVectorTimestampException;
import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.grpc.Gossip;
import pt.tecnico.sauron.silo.grpc.GossipServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.io.IOException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.*;

public class SiloServer {

    private static final String SERVER_PATH = "/grpc/sauron/silo";
    private static final String GLOBAL_PROPERTIES = "/main.properties";
    private static final String REPLICA_PROPERTIES = "/server.properties";

    private static Properties gossipProperties = new Properties();


    private final int port;
    private final Server server;
    private final Silo silo = new Silo();
    private int messageInterval;

    // Timer to send gossip messages regularly
    ScheduledFuture<?> scheduledFuture;

    // Gossip architecture specific structures
    private GossipStructures gossipStructures;


    private final ZKNaming zkNaming;

    // gossip messages
    final BindableService gossipImpl;

    // silo services
    final BindableService controlImpl;
    final BindableService reportImpl;
    final BindableService queryImpl;

    public SiloServer(int port, ZKNaming zkNaming, int instance){
        this(ServerBuilder.forPort(port), port, zkNaming, instance);
    }

    // Create a Silo server using serverBuilder as a base
    public SiloServer(ServerBuilder<?> serverBuilder, int port, ZKNaming zkNaming, int instance) {
        this.port = port;
        this.zkNaming = zkNaming;

        try {
            gossipProperties.load(SiloServer.class.getResourceAsStream(REPLICA_PROPERTIES));
            this.messageInterval = Integer.parseInt(gossipProperties.getProperty("gossipMessageInterval"));
            gossipProperties.load(SiloServer.class.getResourceAsStream(GLOBAL_PROPERTIES));
            this.gossipStructures = new GossipStructures(Integer.parseInt(gossipProperties.getProperty("numReplicas")));
        } catch (IOException e) {
            System.out.println("Could not load properties file");
            this.messageInterval = 30;
            this.gossipStructures = new GossipStructures(3);
        }
        this.gossipStructures.setInstance(instance);
        // since the gossip structures can be manipulated in the constructor, the services can only be created here
        this.gossipImpl = new SiloGossipServiceImpl(this.silo, this.gossipStructures);
        this.controlImpl = new SiloControlServiceImpl(silo, gossipStructures);
        this.reportImpl = new SiloReportServiceImpl(silo, gossipStructures);
        this.queryImpl = new SiloQueryServiceImpl(silo, gossipStructures);

        this.server = serverBuilder.addService(this.controlImpl)
                .addService(this.reportImpl)
                .addService(this.queryImpl)
                .addService(this.gossipImpl)
                .build();

    }

    public void start() throws IOException {
        server.start();

        // schedule gossip message sending
        gossipMessageSchedule();

        System.out.println("Server started, listening on port " + port);
    }

    public void awaitTermination() throws InterruptedException {
        new Thread(()-> {
            System.out.println("Press enter to shutdown");
            try {
                new Scanner(System.in).nextLine();
            } catch (NoSuchElementException e) {
                System.out.println("Got EOF");
            }
            System.out.println("Shutting down");
            this.scheduledFuture.cancel(true);
            server.shutdown();
        }).start();

        server.awaitTermination();
    }

    private void gossipMessageSchedule() {
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        Runnable gossip = () -> {
            sendGossipMessage();
        };
        this.scheduledFuture = ses.scheduleAtFixedRate(gossip, this.messageInterval, this.messageInterval, TimeUnit.SECONDS);
    }

    private void sendGossipMessage() {
        // find available connections
        int replicaInstance;
        try {
            // send to all possible replicas. do not block waiting for one
            for (ZKRecord record: zkNaming.listRecords(SERVER_PATH)) {
                if ( (replicaInstance = getZKRecordInstance(record)) != this.gossipStructures.getInstance()) {
                    System.out.println("Sending to #" + replicaInstance);

                    // create stub for target replica
                    ManagedChannel channel = ManagedChannelBuilder.forTarget(record.getURI()).usePlaintext().build();
                    GossipServiceGrpc.GossipServiceBlockingStub gossipBlockingStub = GossipServiceGrpc.newBlockingStub(channel);

                    // create gossip message
                    Gossip.GossipRequest request = createGossipRequest(replicaInstance);
                    try {
                        // send gRPC gossip message
                        gossipBlockingStub.gossip(request);

                    } catch (StatusRuntimeException e) {
                        Status status = Status.fromThrowable(e);
                        if (status.getCode() == Status.Code.UNAVAILABLE) {
                            System.out.println("Could not connect to replica #" + replicaInstance);
                            continue;
                        }
                        throw new StatusRuntimeException(e.getStatus());
                    }
                }
            }
        } catch (ZKNamingException e) {
            System.err.println("Name Server error: " + e.getMessage());
        }
    }

    // receives record path and returns record instance number
    private int getZKRecordInstance(ZKRecord record) {
        // instance is the last member of the path
        String[] pathSplit = record.getPath().split("/");
        return Integer.parseInt(pathSplit[pathSplit.length-1]);
    }

    private Gossip.GossipRequest createGossipRequest(int replicaInstance) {

        pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp vecTimestamp =
            pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp.newBuilder()
                .addAllTimestamps(
                    gossipStructures
                    .getValueTS()
                    .getValues()
                ).build();

        // add records to send
        LinkedList<Gossip.Record> listRecords = updatesToSend(replicaInstance);
        return Gossip.GossipRequest.newBuilder().addAllRecords(listRecords).setReplicaTimeStamp(vecTimestamp).setSenderId(this.gossipStructures.getInstance()).build();
    }

    private LinkedList<Gossip.Record> updatesToSend(int replicaInstance) {
        try {
            LinkedList<Gossip.Record> recordList = new LinkedList<>();
            for (LogEntry le : gossipStructures.getUpdateLog()) {
                // if the timestamp in the table is lower, we need to send the update
                if (!gossipStructures.getTimestampTable().get(replicaInstance-1).greaterOrEqualThan(le.getTs()))
                    recordList.add(logEntryToRecord(le));
            }
            System.out.println("Sending " + recordList.size() + " updates" );
            return recordList;
        } catch (InvalidVectorTimestampException e) {
            System.out.println(e.getMessage());
        }
        return new LinkedList<>();
    }



    // ===================================================
    // CONVERT BETWEEN OBJECTS AND GRPC
    // ===================================================

    private Gossip.Record logEntryToRecord(LogEntry le) {
        Gossip.Record record = Gossip.Record.newBuilder().setOpId(le.getOpId())
                .setPrev(pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp.newBuilder()
                        .addAllTimestamps(le.getPrev().getValues()))
                .setTs(pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp.newBuilder()
                        .addAllTimestamps(le.getTs().getValues()))
                .setReplicaId(le.getReplicaId())
                .build();
        // to use one of, we need to check the instance of the command
        return le.getCommand().commandToGRPC(record);
    }

}