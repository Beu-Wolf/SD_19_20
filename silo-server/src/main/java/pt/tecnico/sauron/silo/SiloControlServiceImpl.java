package pt.tecnico.sauron.silo;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.domain.exceptions.*;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

import java.time.Instant;

import static io.grpc.Status.INVALID_ARGUMENT;

public class SiloControlServiceImpl extends ControlServiceGrpc.ControlServiceImplBase {

    private pt.tecnico.sauron.silo.domain.Silo silo;

    public SiloControlServiceImpl(pt.tecnico.sauron.silo.domain.Silo silo) {
        this.silo = silo;
    }



    // ===================================================
    // SERVICE IMPLEMENTATION
    // ===================================================
    @Override
    public void ping(Silo.PingRequest request, StreamObserver<Silo.PingResponse> responseObserver) {
        String input = request.getText();

        if (input == null || input.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(ErrorMessages.BLANK_INPUT).asRuntimeException());
            return;
        }

        String output = "Hello " + input + "!";
        Silo.PingResponse response = createPingResponse(output);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void clear( Silo.ClearRequest request, StreamObserver<Silo.ClearResponse> responseObserver) {
        silo.clearObservations();
        silo.clearCams();

        responseObserver.onNext(Silo.ClearResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<Silo.InitCamRequest> initCams(StreamObserver<Silo.InitResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(Silo.InitCamRequest request) {
                try {
                    Cam cam = camFromGRPC(request.getCam());
                    silo.registerCam(cam);
                } catch(SiloException e) {
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
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
                    Report report = reportFromGRPC(request);

                    silo.recordReport(report);
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



    // ===================================================
    // CREATE GRPC RESPONSES
    // ===================================================
    private Silo.PingResponse createPingResponse(String output) {
        return Silo.PingResponse.newBuilder()
                .setText(output)
                .build();
    }



    // ===================================================
    // CONVERT BETWEEN DOMAIN AND GRPC
    // ===================================================
    private Report reportFromGRPC(Silo.InitObservationRequest report) throws SiloInvalidArgumentException, EmptyCameraNameException, InvalidCameraNameException {
        Cam cam = camFromGRPC(report.getCam());
        Observation obs = observationFromGRPC(report.getObservation());
        Instant timestamp = instantFromGRPC(report.getTimestamp());

        return new Report(cam, obs, timestamp);
    }

    private Observation observationFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.Observation observation) throws SiloInvalidArgumentException {
        String id = observation.getObservationId();

        switch (observation.getType()) {
            case PERSON:
                return new Person(id);
            case CAR:
                return new Car(id);
            default:
                throw new SiloInvalidArgumentException(ErrorMessages.UNIMPLEMENTED_OBSERVATION_TYPE);
        }
    }

    private Cam camFromGRPC(Silo.Cam cam) throws EmptyCameraNameException, InvalidCameraNameException {
        String name = cam.getName();
        Coords coords = coordsFromGRPC(cam.getCoords());
        return new Cam(name, coords);
    }

    private Coords coordsFromGRPC(LatLng coords) {
        return new Coords(coords.getLatitude(), coords.getLongitude());
    }

    private Instant instantFromGRPC(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds());
    }
}
