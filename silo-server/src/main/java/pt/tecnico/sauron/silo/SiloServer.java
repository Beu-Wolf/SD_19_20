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
import java.util.Scanner;
import java.util.concurrent.*;

public class SiloServer {

    private final String SERVER_PATH = "/grpc/sauron/silo";

    private final int port;
    private final Server server;
    private final Silo silo = new Silo();

    // timer
    ScheduledFuture<?> scheduledFuture;

    // Gossip architecture specific structures
    private GossipStructures gossipStructures = new GossipStructures();

    private final ZKNaming zkNaming;

    final BindableService controlImpl = new SiloControlServiceImpl(silo, gossipStructures);
    final BindableService reportImpl = new SiloReportServiceImpl(silo, gossipStructures);
    final BindableService queryImpl = new SiloQueryServiceImpl(silo, gossipStructures);
    final BindableService gossipImpl = new SiloGossipServiceImpl(silo, gossipStructures);

    public SiloServer(int port, ZKNaming zkNaming, int instance){
        this(ServerBuilder.forPort(port), port, zkNaming, instance);
    }
    /** Create a Silo server using serverBuilder as a base. */
    public SiloServer(ServerBuilder<?> serverBuilder, int port, ZKNaming zkNaming, int instance) {
        this.port = port;
        this.server = serverBuilder.addService(this.controlImpl)
                .addService(this.reportImpl)
                .addService(this.queryImpl)
                .addService(this.gossipImpl)
                .build();
        this.zkNaming = zkNaming;
        gossipStructures.setInstance(instance);
    }

    public void start() throws IOException {
        server.start();
        System.out.println("Server started, listening on " + port);
        gossipMessageSchedule();
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
        this.scheduledFuture = ses.scheduleAtFixedRate(gossip, 30, 30, TimeUnit.SECONDS); // TODO change to be configurable
    }

    private void sendGossipMessage() {
        // find available connections
        int replicaInstance;
        try {
            for (ZKRecord record: zkNaming.listRecords(SERVER_PATH)) {
                if ( (replicaInstance = getZKRecordInstance(record)) != this.gossipStructures.getInstance()) {
                    System.out.println("Sending to " + replicaInstance);
                    // make stubs for each one
                    ManagedChannel channel = ManagedChannelBuilder.forTarget(record.getURI()).usePlaintext().build();
                    GossipServiceGrpc.GossipServiceBlockingStub gossipBlockingStub = GossipServiceGrpc.newBlockingStub(channel);

                    // constructMessage
                    Gossip.GossipRequest request = createGossipRequest(replicaInstance);
                    // send gossipMessage
                    try {
                        Gossip.GossipResponse response = gossipBlockingStub.gossip(request); // TODO do we need this response? Should we print something?
                    } catch (StatusRuntimeException e) {
                        Status status = Status.fromThrowable(e);
                        if (status.getCode() == Status.Code.UNAVAILABLE) {
                            System.out.println("Can't connect to " + replicaInstance);
                            continue;
                        }
                        throw new StatusRuntimeException(e.getStatus());
                    }
                }
            }
        } catch (ZKNamingException e) {
            System.out.println(e.getMessage()); // Is this the best policy?
        }
    }

    private int getZKRecordInstance(ZKRecord record) {
        String[] pathSplit = record.getPath().split("/");
        return Integer.parseInt(pathSplit[pathSplit.length-1]);
    }

    private Gossip.GossipRequest createGossipRequest(int replicaInstance) {

        pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp vecTimestamp =
                pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp.newBuilder().addAllTimestamps(gossipStructures.getReplicaTS().getValues()).build();

        // add all records
        LinkedList<Gossip.Record> listRecords = updatesToSend(replicaInstance);
        return Gossip.GossipRequest.newBuilder().addAllRecords(listRecords).setReplicaTimeStamp(vecTimestamp).setSenderId(this.gossipStructures.getInstance()).build();

    }

    private LinkedList<Gossip.Record> updatesToSend(int replicaInstance) {
        try {
            LinkedList<Gossip.Record> recordList = new LinkedList<>();
            for (LogEntry le : gossipStructures.getUpdateLog()) {
                // if the timestamp in the table is lower, we need to send the update
                System.out.println("TS: " + gossipStructures.getTimestampTable().get(replicaInstance-1));
                System.out.println("le: " + le.getTs());
                if (!gossipStructures.getTimestampTable().get(replicaInstance-1).greaterOrEqualThan(le.getTs()))
                    recordList.add(logEntryToRecord(le));
            }
            System.out.println("Update log is: " + this.gossipStructures.getUpdateLog());
            System.out.println("Sendind " + recordList.size() + " updates" );
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