package pt.tecnico.sauron.silo;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.sauron.silo.contract.domain.*;
import pt.sauron.silo.contract.domain.exceptions.*;
import pt.tecnico.sauron.silo.exceptions.*;
import pt.tecnico.sauron.silo.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

import java.time.Instant;

import static io.grpc.Status.INVALID_ARGUMENT;

public class SiloControlServiceImpl extends ControlServiceGrpc.ControlServiceImplBase {

    private pt.tecnico.sauron.silo.Silo silo;

    public SiloControlServiceImpl(pt.tecnico.sauron.silo.Silo silo) {
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
    public void clear(Silo.ClearRequest request, StreamObserver<Silo.ClearResponse> responseObserver) {
        silo.clearObservations();
        silo.clearCams();

        responseObserver.onNext(Silo.ClearResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void initCams(Silo.InitCamsRequest request, StreamObserver<Silo.InitCamsResponse> responseObserver) {
        CompositeSiloServerException exceptions = new CompositeSiloServerException();
        for(Silo.Cam grpcCam : request.getCamsList()) {
            try {
                Cam cam = camFromGRPC(grpcCam);
                silo.registerCam(cam);
            } catch (DuplicateCameraNameServerException | EmptyCameraNameException | InvalidCameraNameException | InvalidCameraCoordsException e) {
               exceptions.addException(e);
            }
        }

        if(!exceptions.isEmpty()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription(exceptions.getMessage())
                .asRuntimeException());
        }

        responseObserver.onNext(createInitCamsResponse());
        responseObserver.onCompleted();
    }

    @Override
    public void initObservations(Silo.InitObservationsRequest request, StreamObserver<Silo.InitObservationsResponse> responseObserver) {
        CompositeSiloServerException exceptions = new CompositeSiloServerException();
        for(Silo.InitObservationsItem observation : request.getObservationsList()) {
            try {
                Report report = reportFromGRPC(observation);

                silo.recordReport(report);
            } catch(SiloException e) {
                exceptions.addException(e);
            }
        }


        if(!exceptions.isEmpty()) {
            responseObserver.onError(INVALID_ARGUMENT
            .withDescription(exceptions.getMessage())
            .asRuntimeException());
            return;
        }

        responseObserver.onNext(createInitObservationsResponse());
        responseObserver.onCompleted();
    }



    // ===================================================
    // CREATE GRPC RESPONSES
    // ===================================================
    private Silo.PingResponse createPingResponse(String output) {
        return Silo.PingResponse.newBuilder()
                .setText(output)
                .build();
    }

    private Silo.InitCamsResponse createInitCamsResponse() {
        return Silo.InitCamsResponse.getDefaultInstance();
    }

    private Silo.InitObservationsResponse createInitObservationsResponse() {
        return Silo.InitObservationsResponse.getDefaultInstance();
    }

    // ===================================================
    // CONVERT BETWEEN DOMAIN AND GRPC
    // ===================================================
    private Report reportFromGRPC(Silo.InitObservationsItem report)
            throws InvalidPersonIdException, InvalidCarIdException, TypeNotSupportedServerException, EmptyCameraNameException, InvalidCameraNameException {
        Cam cam = camFromGRPC(report.getCam());
        Observation obs = observationFromGRPC(report.getObservation());
        Instant timestamp = instantFromGRPC(report.getTimestamp());

        return new Report(cam, obs, timestamp);
    }

    private Observation observationFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.Observation observation) throws InvalidPersonIdException, InvalidCarIdException, TypeNotSupportedServerException{
        String id = observation.getObservationId();

        switch (observation.getType()) {
            case PERSON:
                return new Person(id);
            case CAR:
                return new Car(id);
            default:
                throw new TypeNotSupportedServerException();
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
