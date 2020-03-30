package pt.tecnico.sauron.silo.client;

import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.exceptions.TypeNotSupportedException;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    public void report(String name, List<ObservationDto> observations) throws TypeNotSupportedException, InterruptedException {
        Metadata header = new Metadata();
        header.put(METADATA_CAM_NAME, name);
        this.reportStub = MetadataUtils.attachHeaders(this.reportStub, header);
        final CountDownLatch latch = new CountDownLatch(1);

        System.out.println(observations.toString());

        StreamObserver<Silo.ReportResponse> responseObserver =  new StreamObserver<>() {
            @Override
            public void onNext(Silo.ReportResponse reportResponse) { }

            @Override
            public void onError(Throwable throwable) {
                Status status = Status.fromThrowable(throwable);
                if(status.getCode() == Status.Code.NOT_FOUND || status.getCode() == Status.Code.INVALID_ARGUMENT) {
                    System.err.println(status.getDescription());
                    latch.countDown();
                }
            }
            @Override
            public void onCompleted() {
                System.out.println("Successfully reported observations!");
                latch.countDown();
            }
        };
        StreamObserver<Silo.Observation> requestObserver = this.reportStub.report(responseObserver);
        try {
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
                System.out.println(observationDto.toString());

                Silo.Observation observation = Silo.Observation.newBuilder().setObservationId(observationDto.getId())
                        .setType(observationType).build();
                requestObserver.onNext(observation);
                Thread.sleep(10);
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            System.out.println("Error while sending reports" + e.toString());
        }
        requestObserver.onCompleted();
        latch.await(10, TimeUnit.SECONDS);



    }

    public ObservationDto track(ObservationDto.ObservationType type, String id) { return null; }

    // public void trackMatch(ObservationDto.ObservationType type, String query, Lambda)

    // public void trace(ObservationDto.ObservationType type, String id, Lambda)

    public String ctrlPing(String sentence) {
        Silo.PingRequest request = Silo.PingRequest.newBuilder().setText(sentence).build();
        Silo.PingResponse response = this.ctrlStub.ping(request);
        return response.getText();
    }

    public void ctrlClear() {}

    public void ctrlInit() {}

    public void shutdown() {
        this.channel.shutdown();
    }
}
