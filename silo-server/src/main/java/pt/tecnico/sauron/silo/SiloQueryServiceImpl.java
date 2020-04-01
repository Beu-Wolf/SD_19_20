package pt.tecnico.sauron.silo;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.StatusException;
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

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

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

    @Override
    public void trackMatch(QueryRequest request, StreamObserver<QueryResponse> responseObserver) {
        ObservationType type = request.getType();
        String pattern = request.getId();

        TreeSet<String> matched = new TreeSet<>();

        pattern = Pattern.quote(pattern);
        pattern = pattern.replace("*", "\\E.*\\Q");
        Pattern p = Pattern.compile(pattern);

        for (Report report : silo.getReports()) {
            Observation observation = report.getObservation();
            String id = observation.getId();

            try {
                if (domainObservationToTypeGRPC(observation) == request.getType() &&
                        !matched.contains(id) &&
                        p.matcher(id).find()) {
                    matched.add(id);

                    responseObserver.onNext(domainReportToGRPC(report));
                }
            } catch (SiloInvalidArgumentException e) {}
        }

        if (matched.isEmpty()) {
            responseObserver.onError(Status.NOT_FOUND.asException());
        } else {
            responseObserver.onCompleted();
        }
    }

    private QueryResponse domainReportToGRPC(Report report) throws SiloInvalidArgumentException {
        return QueryResponse.newBuilder()
                .setTimestamp(Timestamp.newBuilder().setSeconds(report.getTimestamp().getEpochSecond()))
                .setObservation(domainObservationToGRPC(report.getObservation()))
                .setCam(domainCamToGRPC(report.getCam())).build();
    }

    private pt.tecnico.sauron.silo.grpc.Silo.Observation domainObservationToGRPC(Observation observation)
            throws SiloInvalidArgumentException {
        return pt.tecnico.sauron.silo.grpc.Silo.Observation.newBuilder()
                .setType(domainObservationToTypeGRPC(observation))
                .setObservationId(observation.getId()).build();
    }

    private ObservationType domainObservationToTypeGRPC(Observation observation)
        throws SiloInvalidArgumentException {
        if (observation instanceof Car) {
            return ObservationType.CAR;
        } else if (observation instanceof Person) {
            return ObservationType.PERSON;
        } else {
            throw new SiloInvalidArgumentException(ErrorMessages.UNIMPLEMENTED_OBSERVATION_TYPE);
        }
    }

    private pt.tecnico.sauron.silo.grpc.Silo.Cam domainCamToGRPC(Cam cam) {
        LatLng coords = LatLng.newBuilder().setLatitude(cam.getCoords().getLat())
                .setLongitude(cam.getCoords().getLon()).build();

        return pt.tecnico.sauron.silo.grpc.Silo.Cam.newBuilder().setCoords(coords)
                .setName(cam.getName()).build();
    }
}
