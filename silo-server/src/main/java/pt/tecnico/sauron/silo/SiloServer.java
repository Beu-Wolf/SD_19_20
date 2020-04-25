package pt.tecnico.sauron.silo;

import io.grpc.*;
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
    private final int NUM_REPLICAS = 3;

    private final int port;
    private final Server server;
    private final Silo silo = new Silo();
    private final int instance;

    // timer
    ScheduledFuture<?> scheduledFuture;

    // Gossip architecture specific structures
    private int[] replicaTS = new int[NUM_REPLICAS];
    private int[] valueTS = new int[NUM_REPLICAS];
    private ConcurrentLinkedDeque<String> executedOperations = new ConcurrentLinkedDeque<>();
    private int[][] timestampTable = new int[NUM_REPLICAS-1][NUM_REPLICAS];
    private LinkedList<LogEntry> updateLog = new LinkedList<>();

    private final ZKNaming zkNaming;

    final BindableService controlImpl = new SiloControlServiceImpl(silo);
    final BindableService reportImpl = new SiloReportServiceImpl(silo);
    final BindableService queryImpl = new SiloQueryServiceImpl(silo);
    final BindableService gossipImpl = new SiloGossipServiceImpl(silo);

    public SiloServer(int port, ZKNaming zkNaming, int instance){
        this(ServerBuilder.forPort(port), port, zkNaming, instance);
    }
    /** Create a Silo server using serverBuilder as a base. */
    public SiloServer(ServerBuilder<?> serverBuilder, int port, ZKNaming zkNaming, int instance) {
        this.port = port;
        this.server = serverBuilder.addService(this.controlImpl)
                .addService(ServerInterceptors.intercept(this.reportImpl, new SiloReportServiceInterceptor()))
                .addService(this.queryImpl)
                .addService(this.gossipImpl)
                .build();
        this.zkNaming = zkNaming;
        this.instance = instance;
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
            System.out.println("Sending gossip message!"); // debug
        };
        this.scheduledFuture = ses.scheduleAtFixedRate(gossip, 30, 30, TimeUnit.SECONDS); // change to be configurable
    }

    private void sendGossipMessage() {
        // find available connections
        try {
            for (ZKRecord record: zkNaming.listRecords(SERVER_PATH)) {
                if (getZKRecordInstance(record) != this.instance) {
                    // make stubs for each one
                    ManagedChannel channel = ManagedChannelBuilder.forTarget(record.getURI()).usePlaintext().build();
                    GossipServiceGrpc.GossipServiceBlockingStub gossipBlockingStub = GossipServiceGrpc.newBlockingStub(channel);

                    // constructMessage
                    Gossip.GossipRequest request = createGossipRequest();
                    // send gossipMessage
                    Gossip.GossipResponse response = gossipBlockingStub.gossip(request);
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

    private Gossip.GossipRequest createGossipRequest() {
        // Create Records from update Log
        // Create VecTimestamp

        LinkedList<Integer> replicaTS = convertIntArraytoVec(this.replicaTS);

        //add all Timestamps only works with iterables
        Gossip.VecTimestamp vecTimestamp = Gossip.VecTimestamp.newBuilder().addAllTimestamps(replicaTS).build();

        //LinkedList<Gossip.Record> listRecords = convertLogEntrytoRecord()

    }


    // ===================================================
    // CONVERT BETWEEN OBJECTS AND GRPC
    // ===================================================

    private LinkedList<Integer> convertIntArraytoVec(int[] timeStamp) {
        LinkedList<Integer> list = new LinkedList<>();
        for (int i : timeStamp) {
            list.add(i);
        }
        return list;
    }

}