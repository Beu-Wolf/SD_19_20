package pt.tecnico.sauron.silo.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

public class SiloFrontend {

    private final ManagedChannel _channel;
    private final ControlServiceGrpc.ControlServiceBlockingStub _ctrlStub;

    public SiloFrontend(String host, int port) {
        String target = host + ":" + port;
        _channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        _ctrlStub = ControlServiceGrpc.newBlockingStub(_channel);
    }

    public Silo.PingResponse ctrlPing(Silo.PingRequest request) {
        return _ctrlStub.ping(request);
    }

    public void shutdown() {
        _channel.shutdown();
    }
}
