package pt.tecnico.sauron.silo;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class SiloServer {

    private final int port;
    private final Server server;
    private final Silo silo = new Silo();

    final BindableService controlImpl = new SiloControlServiceImpl(silo);
    final BindableService reportImpl = new SiloReportServiceImpl(silo);
    final BindableService queryImpl = new SiloQueryServiceImpl(silo);

    public SiloServer(int port){
        this(ServerBuilder.forPort(port), port);
    }
    /** Create a Silo server using serverBuilder as a base. */
    public SiloServer(ServerBuilder<?> serverBuilder, int port) {
        this.port = port;
        this.server = serverBuilder.addService(this.controlImpl)
                .addService(ServerInterceptors.intercept(this.reportImpl, new SiloReportServiceInterceptor()))
                .addService(this.queryImpl)
                .build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("Server started, listening on " + port);
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
            server.shutdown();
        }).start();

        server.awaitTermination();
    }
}