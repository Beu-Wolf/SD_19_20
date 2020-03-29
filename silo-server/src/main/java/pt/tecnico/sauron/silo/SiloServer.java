package pt.tecnico.sauron.silo;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.sauron.silo.domain.Silo;

import java.io.IOException;

public class SiloServer {

    private final int port;
    private final Server server;
    private final Silo silo = new Silo();

    final BindableService controlImpl = new SiloControlServiceImpl();

    public SiloServer(int port){
        this(ServerBuilder.forPort(port), port);
    }
    /** Create a Silo server using serverBuilder as a base. */
    public SiloServer(ServerBuilder<?> serverBuilder, int port) {
        this.port = port;
        server = serverBuilder.addService(controlImpl).build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("Server started, listening on " + port);
    }

    public void awaitTermination() throws InterruptedException {
        server.awaitTermination();
    }
}
