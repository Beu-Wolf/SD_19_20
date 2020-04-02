package pt.tecnico.sauron.silo;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.domain.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.domain.exceptions.SiloException;
import pt.tecnico.sauron.silo.domain.exceptions.SiloInvalidArgumentException;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

import java.time.Instant;

import static io.grpc.Status.INVALID_ARGUMENT;

public class SiloControlServiceImpl extends ControlServiceGrpc.ControlServiceImplBase {

    private pt.tecnico.sauron.silo.domain.Silo silo;

    public SiloControlServiceImpl(pt.tecnico.sauron.silo.domain.Silo silo) {
        this.silo = silo;
    }

    @Override
    public void ping(Silo.PingRequest request, StreamObserver<Silo.PingResponse> responseObserver) {
        String input = request.getText();

        if (input == null || input.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(ErrorMessages.BLANK_INPUT).asRuntimeException());
            return;
        }

        String output = "Hello " + input + "!";
        Silo.PingResponse response = Silo.PingResponse.newBuilder()
                .setText(output).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public StreamObserver<Silo.InitCamRequest> initCams(StreamObserver<Silo.InitResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(Silo.InitCamRequest request) {
                String name = request.getCam().getName();
                Coords coords = new Coords(request.getCam().getCoords().getLatitude(), request.getCam().getCoords().getLongitude());
                Cam cam = new Cam(name, coords);
                try {
                    silo.registerCam(cam);
                } catch(SiloException e) {
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error while reporting: " + Status.fromThrowable(throwable).getDescription());
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(pt.tecnico.sauron.silo.grpc.Silo.InitResponse.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<Silo.InitObservationRequest> initObservations(StreamObserver<Silo.InitResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(Silo.InitObservationRequest request) {
                try {
                    Cam cam = silo.getCam(request.getCam().getName());
                    Observation obs = GRPCToDomainObservation(request.getObservation());
                    Instant timestamp = Instant.ofEpochSecond(request.getTimestamp().getSeconds());

                    silo.registerObservation(new Report(cam, obs, timestamp));
                } catch(SiloException e) {
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error while reporting: " + Status.fromThrowable(throwable).getDescription());
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(pt.tecnico.sauron.silo.grpc.Silo.InitResponse.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };
    }

    private Observation GRPCToDomainObservation(Silo.ObservationType type, String id) throws SiloInvalidArgumentException {
        switch (type) {
            case PERSON:
                return new Person(id);
            case CAR:
                return new Car(id);
            default:
                throw new SiloInvalidArgumentException(ErrorMessages.UNIMPLEMENTED_OBSERVATION_TYPE);
        }
    }

    private Observation GRPCToDomainObservation(pt.tecnico.sauron.silo.grpc.Silo.Observation observation) throws SiloInvalidArgumentException {
        return GRPCToDomainObservation(observation.getType(), observation.getObservationId());
    }
}
