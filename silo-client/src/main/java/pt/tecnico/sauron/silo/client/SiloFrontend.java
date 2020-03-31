package pt.tecnico.sauron.silo.client;

import com.google.type.LatLng;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.exceptions.*;
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
    private ReportServiceGrpc.ReportServiceBlockingStub reportBlockingStub;
    public static final Metadata.Key<String> METADATA_CAM_NAME = Metadata.Key.of("name", Metadata.ASCII_STRING_MARSHALLER);

    public SiloFrontend(String host, int port) {
        String target = host + ":" + port;
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.ctrlStub = ControlServiceGrpc.newBlockingStub(this.channel);
        this.reportStub = ReportServiceGrpc.newStub(this.channel);
        this.reportBlockingStub = ReportServiceGrpc.newBlockingStub(this.channel);
    }

    public void camJoin(CamDto cam) throws CameraAlreadyExistsException, CameraRegisterException {
        Silo.JoinRequest request = Silo.JoinRequest.newBuilder()
                .setCam(Silo.Cam.newBuilder()
                        .setName(cam.getName())
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(cam.getLat())
                                .setLongitude(cam.getLon())
                                .build())
                        .build())
                .build();

        try {
            this.reportBlockingStub.camJoin(request);
            System.out.println("Registered Successfully!");
        } catch(RuntimeException e) {
            Status status = Status.fromThrowable(e);
            if(status == Status.ALREADY_EXISTS) {
                throw new CameraAlreadyExistsException();
            }
            throw new CameraRegisterException();
        }
    }

    public CamDto camInfo(String name) throws CameraNotFoundException, CameraInfoException {
        Silo.InfoRequest request = Silo.InfoRequest.newBuilder().setName(name).build();
        Silo.InfoResponse response;
        try {
            response = this.reportBlockingStub.camInfo(request);
            LatLng coords = response.getCoords();
            return new CamDto(name, coords.getLatitude(), coords.getLongitude());
        } catch (RuntimeException e) {
            Status status = Status.fromThrowable(e);
            if(status == Status.NOT_FOUND) {
                throw new CameraNotFoundException();
            }
            throw new CameraInfoException();
        }
    }

    public void report(String name, List<ObservationDto> observations) throws ReportException {
        Metadata header = new Metadata();
        header.put(METADATA_CAM_NAME, name);
        this.reportStub = MetadataUtils.attachHeaders(this.reportStub, header);
        final CountDownLatch latch = new CountDownLatch(1);

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
                Silo.ObservationType observationType = getObservationType(observationDto);
                Silo.Observation observation = Silo.Observation.newBuilder()
                        .setObservationId(observationDto.getId())
                        .setType(observationType)
                        .build();
                requestObserver.onNext(observation);
                if(latch.getCount() == 0) {
                    return;
                }

                // As per the documentation for client side streaming in gRPC,
                // we should sleep for an amount of time between each call
                // to allow for the server to send an error if it happens
                Thread.sleep(10);
            }

            requestObserver.onCompleted();
            latch.await(10, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReportException(ErrorMessages.WAITING_THREAD_INTERRUPT);
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw new ReportException(e.toString());
        }
    }

    public ObservationDto track(ObservationDto.ObservationType type, String id) { return null; }

    // public void trackMatch(ObservationDto.ObservationType type, String query, Lambda)

    // public void trace(ObservationDto.ObservationType type, String id, Lambda)

    public String ctrlPing(String sentence) throws PingException{
        Silo.PingRequest request = Silo.PingRequest.newBuilder().setText(sentence).build();
        try {
            Silo.PingResponse response = this.ctrlStub.ping(request);
            return response.getText();
        } catch (StatusRuntimeException e) {
            throw new PingException(e.getStatus().getDescription());
        }
    }

    public void ctrlClear() {}

    public void ctrlInit() {}


    public Silo.ObservationType getObservationType(ObservationDto observationDto) {
        Silo.ObservationType observationType;
        ObservationDto.ObservationType type = observationDto.getType();


        switch (type) {
            case CAR:
                observationType = Silo.ObservationType.CAR;
                break;
            case PERSON:
                observationType = Silo.ObservationType.PERSON;
                break;
            default:
                observationType = Silo.ObservationType.UNSPEC;
        }
        return observationType;
    }

    public void shutdown() {
        this.channel.shutdown();
    }
}
