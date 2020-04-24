package pt.tecnico.sauron.silo;

import com.google.type.LatLng;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.exceptions.*;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;

public class SiloReportServiceImpl extends ReportServiceGrpc.ReportServiceImplBase {

    private pt.tecnico.sauron.silo.domain.Silo silo;

    SiloReportServiceImpl(pt.tecnico.sauron.silo.domain.Silo silo) {
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
        } catch(DuplicateCameraNameException e) {
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
        } catch(NoCameraFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
        }
    }

    @Override
    public StreamObserver<pt.tecnico.sauron.silo.grpc.Silo.Observation> report(StreamObserver<pt.tecnico.sauron.silo.grpc.Silo.ReportResponse> responseObserver) {
        Cam cam;
        try {
            final String name = SiloReportServiceInterceptor.CAM_NAME.get();
            cam = this.silo.getCam(name);
        } catch (NoCameraFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
            return null;
        }

        return new StreamObserver<>() {
            boolean error = false;
            SiloException e;

            @Override
            public void onNext(pt.tecnico.sauron.silo.grpc.Silo.Observation observation) {
                try {
                    Observation obs = observationFromGRPC(observation);
                    silo.registerObservation(cam, obs);
                } catch (SiloException e) {
                    this.error = true;
                    this.e = e;
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error while reporting: " + Status.fromThrowable(throwable).getDescription());
            }

            @Override
            public void onCompleted() {
                if (this.error) {
                    responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(this.e.getMessage()).asRuntimeException());
                } else {
                    responseObserver.onNext(pt.tecnico.sauron.silo.grpc.Silo.ReportResponse.getDefaultInstance());
                    responseObserver.onCompleted();
                }
            }
        };
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



    // ===================================================
    // CONVERT BETWEEN DOMAIN AND GRPC
    // ===================================================
    private Observation observationFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.Observation observation) throws InvalidCarIdException, InvalidPersonIdException, TypeNotSupportedException {
        pt.tecnico.sauron.silo.grpc.Silo.ObservationType type = observation.getType();
        String id = observation.getObservationId();
        switch (type) {
            case CAR:
                return new Car(id);
            case PERSON:
                return new Person(id);
            default:
                throw new TypeNotSupportedException();
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
