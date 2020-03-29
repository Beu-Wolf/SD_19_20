package pt.tecnico.sauron.silo;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.domain.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.domain.exceptions.InvalidCarIdException;
import pt.tecnico.sauron.silo.domain.exceptions.InvalidPersonIdException;
import pt.tecnico.sauron.silo.domain.exceptions.TypeNotSupportedException;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

import java.time.LocalDateTime;

public class SiloReportServiceImpl extends ReportServiceGrpc.ReportServiceImplBase {


    @Override
    public StreamObserver<Silo.Observation> report(StreamObserver<Silo.ReportResponse> responseObserver) {
        final String name = SiloReportServiceInterceptor.CAM_NAME.get();
        Cam cam = pt.tecnico.sauron.silo.domain.Silo.getCam(name);
        if (cam == null)
            responseObserver.onError(Status.NOT_FOUND.withDescription(ErrorMessages.NO_CAM_FOUND).asRuntimeException());

        return new StreamObserver<>() {
            @Override
            public void onNext(Silo.Observation observation) {
                try {
                    Observation obs = createReport(observation.getType(), observation.getObservationId());
                    Report report = new Report(cam, obs, LocalDateTime.now());
                    pt.tecnico.sauron.silo.domain.Silo.registerObservation(report);
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
                responseObserver.onNext(Silo.ReportResponse.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };
    }

    public static Observation createReport(Silo.ObservationType type, String id) throws InvalidCarIdException, InvalidPersonIdException, TypeNotSupportedException {
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
