package pt.tecnico.sauron.silo.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.exceptions.TypeNotSupportedException;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

import java.util.List;

public class SiloFrontend {
    private final ManagedChannel _channel;
    private final ControlServiceGrpc.ControlServiceBlockingStub _ctrlStub;
    private ReportServiceGrpc.ReportServiceStub _reportStub;
    public static final Metadata.Key<String> METADATA_CAM_NAME = Metadata.Key.of("name", Metadata.ASCII_STRING_MARSHALLER);

    public SiloFrontend(String host, int port) {
        String target = host + ":" + port;
        _channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        _ctrlStub = ControlServiceGrpc.newBlockingStub(_channel);
        _reportStub = ReportServiceGrpc.newStub(_channel);
    }

    public void camJoin(CamDto cam) {}

    public CamDto camInfo(String name) { return null; }

    public void report(String name, List<ObservationDto> observations) throws TypeNotSupportedException{
        Metadata header = new Metadata();
        header.put(METADATA_CAM_NAME, name);
        _reportStub = MetadataUtils.attachHeaders(_reportStub, header);

        StreamObserver<Silo.ReportResponse> reportObserver =  new StreamObserver<>() {
            @Override
            public void onNext(Silo.ReportResponse reportResponse) { }
            @Override
            public void onError(Throwable throwable) {}
            @Override
            public void onCompleted() {
                System.out.println("Successfully reported observations!");
            }
        };

        StreamObserver<Silo.Observation> observationObserver = _reportStub.report(reportObserver);

        for(ObservationDto observationDto : observations) {
            ObservationDto.ObservationType type = observationDto.getType();
            Silo.ObservationType observationType;
            switch (type) {
                case CAR:
                    observationType = Silo.ObservationType.CAR;
                    break;
                case PERSON:
                    observationType = Silo.ObservationType.PERSON;
                    break;
                default:
                    throw new TypeNotSupportedException();
            }

            Silo.Observation observation = Silo.Observation.newBuilder().setObservationId(observationDto.getId())
                    .setType(observationType).build();
            observationObserver.onNext(observation);
        }

        observationObserver.onCompleted();
    }

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
