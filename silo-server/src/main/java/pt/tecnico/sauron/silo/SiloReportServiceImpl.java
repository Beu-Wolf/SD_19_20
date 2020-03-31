package pt.tecnico.sauron.silo;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.domain.Cam;
import pt.tecnico.sauron.silo.domain.Observation;
import pt.tecnico.sauron.silo.domain.Report;
import pt.tecnico.sauron.silo.domain.Person;
import pt.tecnico.sauron.silo.domain.Car;
import pt.tecnico.sauron.silo.domain.exceptions.*;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;

import java.time.LocalDateTime;

public class SiloReportServiceImpl extends ReportServiceGrpc.ReportServiceImplBase {

    private pt.tecnico.sauron.silo.domain.Silo silo;

    SiloReportServiceImpl(pt.tecnico.sauron.silo.domain.Silo silo) {
        this.silo = silo;
    }

    @Override
    public StreamObserver<pt.tecnico.sauron.silo.grpc.Silo.Observation> report(StreamObserver<pt.tecnico.sauron.silo.grpc.Silo.ReportResponse> responseObserver) {
        try {
            final String name = SiloReportServiceInterceptor.CAM_NAME.get();
            Cam cam = silo.getCam(name);

            return new StreamObserver<>() {
                @Override
                public void onNext(pt.tecnico.sauron.silo.grpc.Silo.Observation observation) {
                    try {
                        Observation obs = createReport(observation.getType(), observation.getObservationId());
                        Report report = new Report(cam, obs, LocalDateTime.now());
                        silo.registerObservation(report);
                    } catch (InvalidCarIdException e) {
                        responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(ErrorMessages.INVALID_CAR_ID).asRuntimeException());
                    } catch (InvalidPersonIdException e) {
                        responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(ErrorMessages.INVALID_PERSON_ID).asRuntimeException());
                    } catch (TypeNotSupportedException e) {
                        responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(ErrorMessages.TYPE_NOT_SUPPORTED).asRuntimeException());
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
        } catch (NoCameraFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(ErrorMessages.NO_CAM_FOUND).asRuntimeException());
            return null;
        }
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
