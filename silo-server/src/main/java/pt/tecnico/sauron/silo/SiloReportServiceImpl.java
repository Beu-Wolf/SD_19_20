package pt.tecnico.sauron.silo;

import com.google.type.LatLng;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.domain.exceptions.*;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;

import java.time.LocalDateTime;

public class SiloReportServiceImpl extends ReportServiceGrpc.ReportServiceImplBase {

    private pt.tecnico.sauron.silo.domain.Silo silo;

    SiloReportServiceImpl(pt.tecnico.sauron.silo.domain.Silo silo) {
        this.silo = silo;
    }

    @Override
    public void camJoin(pt.tecnico.sauron.silo.grpc.Silo.JoinRequest request, io.grpc.stub.StreamObserver<pt.tecnico.sauron.silo.grpc.Silo.JoinResponse> responseObserver) {
        String name = request.getCam().getName();
        Coords coords = new Coords(request.getCam().getCoords().getLatitude(), request.getCam().getCoords().getLongitude());
        Cam cam = new Cam(name, coords);

        try {
            this.silo.registerCam(cam);
            pt.tecnico.sauron.silo.grpc.Silo.JoinResponse response = pt.tecnico.sauron.silo.grpc.Silo.JoinResponse.getDefaultInstance();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch(DuplicateCameraNameException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void camInfo(pt.tecnico.sauron.silo.grpc.Silo.InfoRequest request, io.grpc.stub.StreamObserver<pt.tecnico.sauron.silo.grpc.Silo.InfoResponse> responseObserver) {
        String name = request.getName();

        try {
            Cam cam = this.silo.getCam(name);
            Double lat = cam.getLat();
            Double lon = cam.getLon();
            pt.tecnico.sauron.silo.grpc.Silo.InfoResponse response = pt.tecnico.sauron.silo.grpc.Silo.InfoResponse.newBuilder().
                    setCoords(
                            LatLng.newBuilder()
                                    .setLatitude(lat)
                                    .setLongitude(lon)
                                    .build()
                    ).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch(NoCameraFoundException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
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
            @Override
            public void onNext(pt.tecnico.sauron.silo.grpc.Silo.Observation observation) {
                try {
                    Observation obs = createReport(observation.getType(), observation.getObservationId());
                    Report report = new Report(cam, obs, LocalDateTime.now());
                    silo.registerObservation(report);
                } catch (SiloException e) {
                    responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error while reporting: " + Status.fromThrowable(throwable).getDescription());
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(pt.tecnico.sauron.silo.grpc.Silo.ReportResponse.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };
    }

    public Observation createReport(pt.tecnico.sauron.silo.grpc.Silo.ObservationType type, String id) throws InvalidCarIdException, InvalidPersonIdException, TypeNotSupportedException {
        switch (type) {
            case CAR:
                return new Car(id);
            case PERSON:
                return new Person(id);
            default:
                throw new TypeNotSupportedException();
        }
    }
}
