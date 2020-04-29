package pt.tecnico.sauron.silo;

import com.google.type.LatLng;
import io.grpc.Status;
import pt.sauron.silo.contract.domain.*;
import pt.sauron.silo.contract.domain.exceptions.*;
import pt.tecnico.sauron.silo.exceptions.*;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;

public class SiloReportServiceImpl extends ReportServiceGrpc.ReportServiceImplBase {

    private Silo silo;

    SiloReportServiceImpl(Silo silo) {
        this.silo = silo;
    }

    // ===================================================
    // SERVICE IMPLEMENTATION
    // ===================================================
    @Override
    public void camJoin(pt.tecnico.sauron.silo.grpc.Silo.JoinRequest request, io.grpc.stub.StreamObserver<pt.tecnico.sauron.silo.grpc.Silo.JoinResponse> responseObserver) {
        try {
            Cam cam = camFromGRPC(request.getCam());
            this.silo.registerCam(cam);
            pt.tecnico.sauron.silo.grpc.Silo.JoinResponse response = createJoinResponse();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch(DuplicateCameraNameServerException e) {
            responseObserver.onError(Status.ALREADY_EXISTS.asRuntimeException());
        } catch(EmptyCameraNameException | InvalidCameraNameException | InvalidCameraCoordsException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void camInfo(pt.tecnico.sauron.silo.grpc.Silo.InfoRequest request, io.grpc.stub.StreamObserver<pt.tecnico.sauron.silo.grpc.Silo.InfoResponse> responseObserver) {
        String name = request.getName();

        try {
            Cam cam = this.silo.getCam(name);
            pt.tecnico.sauron.silo.grpc.Silo.InfoResponse response = createInfoResponse(cam);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch(NoCameraFoundServerException e) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
        }
    }

    @Override
    public void report(pt.tecnico.sauron.silo.grpc.Silo.ReportRequest request, io.grpc.stub.StreamObserver<pt.tecnico.sauron.silo.grpc.Silo.ReportResponse> responseObserver) {
        Cam cam;
        try {
            final String name = request.getCamName();
            cam = this.silo.getCam(name);
        } catch (NoCameraFoundServerException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
            return;
        }

        // convert repeated observation to observation List
        CompositeSiloServerException exceptions = new CompositeSiloServerException();
        int numAcked = 0;
        for(pt.tecnico.sauron.silo.grpc.Silo.Observation observation : request.getObservationsList()) {
            try {
                Observation o = observationFromGRPC(observation);
                silo.registerObservation(cam, o);
                numAcked += 1;
            } catch (InvalidCarIdException
                    |InvalidPersonIdException
                    | TypeNotSupportedServerException e) {
                exceptions.addException(e);
            }
        }

        if(!exceptions.isEmpty()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
            .withDescription(exceptions.getMessage())
            .asRuntimeException());
            return;
        }

        responseObserver.onNext(createReportResponse(numAcked));
        responseObserver.onCompleted();
    }


    // ===================================================
    // CREATE GRPC RESPONSES
    // ===================================================
    private pt.tecnico.sauron.silo.grpc.Silo.JoinResponse createJoinResponse() {
        return pt.tecnico.sauron.silo.grpc.Silo.JoinResponse.getDefaultInstance();
    }

    private pt.tecnico.sauron.silo.grpc.Silo.InfoResponse createInfoResponse(Cam cam) {
        return pt.tecnico.sauron.silo.grpc.Silo.InfoResponse.newBuilder()
                .setCoords(coordsToGRPC(new Coords(cam.getLat(), cam.getLon())))
                .build();
    }

    private pt.tecnico.sauron.silo.grpc.Silo.ReportResponse createReportResponse(int numAcked) {
        return pt.tecnico.sauron.silo.grpc.Silo.ReportResponse.newBuilder()
                .setNumAcked(numAcked)
                .build();
    }


    // ===================================================
    // CONVERT BETWEEN DOMAIN AND GRPC
    // ===================================================
    private Observation observationFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.Observation observation) throws InvalidCarIdException, InvalidPersonIdException, TypeNotSupportedServerException {
        pt.tecnico.sauron.silo.grpc.Silo.ObservationType type = observation.getType();
        String id = observation.getObservationId();
        switch (type) {
            case CAR:
                return new Car(id);
            case PERSON:
                return new Person(id);
            default:
                throw new TypeNotSupportedServerException();
        }
    }

    private pt.tecnico.sauron.silo.grpc.Silo.Cam camToGRPC(Cam cam) {
        return pt.tecnico.sauron.silo.grpc.Silo.Cam.newBuilder()
                .setName(cam.getName())
                .setCoords(coordsToGRPC(cam.getCoords()))
                .build();
    }
    private Cam camFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.Cam cam) throws EmptyCameraNameException, InvalidCameraNameException {
        String name = cam.getName();
        Coords coords = coordsFromGRPC(cam.getCoords());
        return new Cam(name, coords);
    }

    private LatLng coordsToGRPC(Coords coords) {
        return LatLng.newBuilder()
                .setLatitude(coords.getLat())
                .setLongitude(coords.getLon())
                .build();
    }
    private Coords coordsFromGRPC(LatLng coords) {
        return new Coords(coords.getLatitude(), coords.getLongitude());
    }
}
