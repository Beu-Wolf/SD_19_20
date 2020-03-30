package pt.tecnico.sauron.silo.client;

import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.exceptions.InvalidArgumentException;
import pt.tecnico.sauron.silo.client.exceptions.TypeNotSupportedException;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

import java.util.List;

public class SiloFrontend {
    private ManagedChannel channel;
    private ControlServiceGrpc.ControlServiceBlockingStub ctrlStub;
    private ReportServiceGrpc.ReportServiceStub reportStub;
    public static final Metadata.Key<String> METADATA_CAM_NAME = Metadata.Key.of("name", Metadata.ASCII_STRING_MARSHALLER);

    public SiloFrontend(String host, int port) {
        String target = host + ":" + port;
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.ctrlStub = ControlServiceGrpc.newBlockingStub(this.channel);
        this.reportStub = ReportServiceGrpc.newStub(this.channel);
    }

    public void camJoin(CamDto cam) {}

    public CamDto camInfo(String name) { return null; }

    public void report(String name, List<ObservationDto> observations) throws TypeNotSupportedException, InvalidArgumentException{
        Metadata header = new Metadata();
        header.put(METADATA_CAM_NAME, name);
        reportStub = MetadataUtils.attachHeaders(reportStub, header);

        StreamObserver<Silo.ReportResponse> reportObserver =  new StreamObserver<>() {
            @Override
            public void onNext(Silo.ReportResponse reportResponse) { }
            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error while reporting!");
            }
            @Override
            public void onCompleted() {
                System.out.println("Successfully reported observations!");
            }
        };

        try {
            StreamObserver<Silo.Observation> observationObserver = reportStub.report(reportObserver);
            for (ObservationDto observationDto : observations) {
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
        } catch (StatusRuntimeException e) {
            if(e.getStatus() == Status.INVALID_ARGUMENT) {
                throw new InvalidArgumentException(e.getStatus().getDescription());
            }
        }


    }

    public ObservationDto track(ObservationDto.ObservationType type, String id) { return null; }

    // public void trackMatch(ObservationDto.ObservationType type, String query, Lambda)

    // public void trace(ObservationDto.ObservationType type, String id, Lambda)

    public String ctrlPing(String sentence) {
        Silo.PingRequest request = Silo.PingRequest.newBuilder().setText(sentence).build();
        Silo.PingResponse response = ctrlStub.ping(request);
        return response.getText();
    }

    public void ctrlClear() {}

    public void ctrlInit() {}

    public void shutdown() {
        channel.shutdown();
    }
}
