package pt.tecnico.sauron.silo.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

import java.util.List;

public class SiloFrontend {
    private final ManagedChannel _channel;
    private final ControlServiceGrpc.ControlServiceBlockingStub _ctrlStub;

    public SiloFrontend(String host, int port) {
        String target = host + ":" + port;
        _channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        _ctrlStub = ControlServiceGrpc.newBlockingStub(_channel);
    }

    public void camJoin(CamDto cam) {}

    public CamDto camInfo(String name) { return null; }

    public void report(String name, List<ObservationDto> observations) {}

    public ObservationDto track(ObservationDto.ObservationType type, String id) { return null; }

    // public void trackMatch(ObservationDto.ObservationType type, String query, Lambda)

    // public void trace(ObservationDto.ObservationType type, String id, Lambda)

    public String ctrlPing(String sentence) {
        Silo.PingRequest request = Silo.PingRequest.newBuilder().setText(sentence).build();
        Silo.PingResponse response = _ctrlStub.ping(request);
        return response.getText();
    }

    public void ctrlClear() {}

    public void ctrlInit() {}

    public void shutdown() {
        _channel.shutdown();
    }
}
