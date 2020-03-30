package pt.tecnico.sauron.silo;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.stub.StreamObserver;
import io.grpc.Status;
import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.domain.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.domain.exceptions.ObservationNotFoundException;
import pt.tecnico.sauron.silo.domain.exceptions.SiloInvalidArgumentException;
import pt.tecnico.sauron.silo.grpc.QueryServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.QueryRequest;
import pt.tecnico.sauron.silo.grpc.Silo.QueryResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ObservationType;

public class SiloQueryServiceImpl extends QueryServiceGrpc.QueryServiceImplBase {
    private Silo silo;

    public SiloQueryServiceImpl(Silo silo) {
        this.silo = silo;
    }

    @Override
    public void track(QueryRequest request, StreamObserver<QueryResponse> responseObserver) {
        String id = request.getId();
        ObservationType type = request.getType();

        Observation observation;
        try {
            switch (type) {
                case CAR:
                    observation = new Car(id);
                    break;
                case PERSON:
                    observation = new Person(id);
                    break;
                case UNSPEC:
                    // TODO
                default:
                    responseObserver.onError(Status.UNIMPLEMENTED.withDescription(
                            ErrorMessages.UNIMPLEMENTED_OBSERVATION_TYPE).asRuntimeException());
                    return;
            }

            Report report = silo.track(observation);

            Timestamp timestamp = Timestamp.newBuilder().setSeconds(report.getTimestamp().getEpochSecond()).build();

            responseObserver.onNext(QueryResponse.newBuilder().setCam(domainCamToGRPC(report.getCam()))
                    .setObservation(domainObservationToGRPC(report.getObservation()))
                    .setTimestamp(timestamp).build());

            responseObserver.onCompleted();

        } catch (SiloInvalidArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (ObservationNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private pt.tecnico.sauron.silo.grpc.Silo.Observation domainObservationToGRPC(Observation observation)
            throws SiloInvalidArgumentException {
        ObservationType observationType;
        if (observation instanceof Car) {
            observationType = ObservationType.CAR;
        } else if (observation instanceof Person) {
            observationType = ObservationType.PERSON;
        } else {
            throw new SiloInvalidArgumentException(ErrorMessages.UNIMPLEMENTED_OBSERVATION_TYPE);
        }

        return pt.tecnico.sauron.silo.grpc.Silo.Observation.newBuilder().setType(observationType)
                .setObservationId(observation.getId()).build();
    }

    private pt.tecnico.sauron.silo.grpc.Silo.Cam domainCamToGRPC(Cam cam) {
        LatLng coords = LatLng.newBuilder().setLatitude(cam.getCoords().getLat())
                .setLongitude(cam.getCoords().getLon()).build();

        return pt.tecnico.sauron.silo.grpc.Silo.Cam.newBuilder().setCoords(coords)
                .setName(cam.getName()).build();
    }
}
