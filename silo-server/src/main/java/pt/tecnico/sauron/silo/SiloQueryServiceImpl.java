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

import java.time.Instant;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class SiloQueryServiceImpl extends QueryServiceGrpc.QueryServiceImplBase {
    private Silo silo;

    public SiloQueryServiceImpl(Silo silo) {
        this.silo = silo;
    }



    // ===================================================
    // SERVICE IMPLEMENTATION
    // ===================================================
    @Override
    public void track(QueryRequest request, StreamObserver<QueryResponse> responseObserver) {
        String id = request.getId();
        ObservationType type = request.getType();

        try {
            Observation observation = observationFromGRPC(type, id);
            Report report = silo.track(observation);

            QueryResponse response = createQueryResponse(report);
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (SiloInvalidArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (ObservationNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public synchronized void trackMatch(QueryRequest request, StreamObserver<QueryResponse> responseObserver) {
        String pattern = request.getId();
        ObservationType type = request.getType();

        TreeSet<String> matched = new TreeSet<>();
        TrackMatchComparator comparator = new TrackMatchComparator(type, pattern);

        try {
            for (Report report : silo.getReportsByNew()) {
                Observation observation = report.getObservation();
                String id = observation.getId();

                if (!matched.contains(id) && observation.matches(comparator)) {
                    matched.add(id);

                    QueryResponse response = createQueryResponse(report);
                    responseObserver.onNext(response);
                }
            }
        } catch (SiloInvalidArgumentException e) {
            e.printStackTrace();
            responseObserver.onError(Status.UNIMPLEMENTED.withDescription(
                    ErrorMessages.UNIMPLEMENTED_OBSERVATION_TYPE).asRuntimeException());
            return;
        }

        if (matched.isEmpty()) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
        } else {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void trace(QueryRequest request, StreamObserver<QueryResponse> responseObserver) {
        ObservationType type = request.getType();
        String queryId = request.getId();
        boolean found = false;

        try {
            Observation queryObservation = observationFromGRPC(type, queryId);

            for (Report report : silo.getReportsByNew()) {
                Observation observation = report.getObservation();

                if (observation.equals(queryObservation)) {
                    found = true;
                    responseObserver.onNext(createQueryResponse(report));
                }
            }
        } catch (SiloInvalidArgumentException e) {
            responseObserver.onError(Status.UNIMPLEMENTED.withDescription(
                    ErrorMessages.UNIMPLEMENTED_OBSERVATION_TYPE).asRuntimeException());
            return;
        }

        if (!found) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
        } else {
            responseObserver.onCompleted();
        }
    }



    // ===================================================
    // CREATE GRPC RESPONSES
    // ===================================================
    private QueryResponse createQueryResponse(Report report) throws SiloInvalidArgumentException {
        return reportToGRPC(report);
    }



    // ===================================================
    // CONVERT BETWEEN DOMAIN AND GRPC
    // ===================================================
    private QueryResponse reportToGRPC(Report report) throws SiloInvalidArgumentException {
        return QueryResponse.newBuilder()
                .setCam(camFromGRPC(report.getCam()))
                .setObservation(observationToGRPC(report.getObservation()))
                .setTimestamp(timestampToGRPC(report.getTimestamp()))
                .build();
    }

    private pt.tecnico.sauron.silo.grpc.Silo.Observation observationToGRPC(Observation observation) throws SiloInvalidArgumentException {
        return pt.tecnico.sauron.silo.grpc.Silo.Observation.newBuilder()
                .setType(domainObservationToTypeGRPC(observation))
                .setObservationId(observation.getId()).build();
    }

    private ObservationType domainObservationToTypeGRPC(Observation observation) throws SiloInvalidArgumentException {
        if (observation instanceof Car) {
            return ObservationType.CAR;
        } else if (observation instanceof Person) {
            return ObservationType.PERSON;
        } else {
            throw new SiloInvalidArgumentException(ErrorMessages.UNIMPLEMENTED_OBSERVATION_TYPE);
        }
    }

    private pt.tecnico.sauron.silo.grpc.Silo.Cam camFromGRPC(Cam cam) {
        LatLng coords = LatLng.newBuilder().setLatitude(cam.getCoords().getLat())
                .setLongitude(cam.getCoords().getLon()).build();

        return pt.tecnico.sauron.silo.grpc.Silo.Cam.newBuilder().setCoords(coords)
                .setName(cam.getName()).build();
    }

    private Observation observationFromGRPC(ObservationType type, String id) throws SiloInvalidArgumentException {
        switch (type) {
            case PERSON:
                return new Person(id);
            case CAR:
                return new Car(id);
            default:
                throw new SiloInvalidArgumentException(ErrorMessages.UNIMPLEMENTED_OBSERVATION_TYPE);
        }
    }
    private Observation observationFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.Observation observation) throws SiloInvalidArgumentException {
        return observationFromGRPC(observation.getType(), observation.getObservationId());
    }

    private Timestamp timestampToGRPC(Instant timestamp) {
        return Timestamp.newBuilder()
                .setSeconds(timestamp.getEpochSecond())
                .build();
    }
    private Instant timestampFromGRPC(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds());
    }



    // ===================================================
    // HELPER CLASS
    // ===================================================
    private class TrackMatchComparator implements ObservationVisitor {
        Pattern p;
        ObservationType type;

        TrackMatchComparator(ObservationType type, String pattern) {
            pattern = Pattern.quote(pattern);
            pattern = pattern.replace("*", "\\E.*\\Q");
            pattern = "^" + pattern + "$";
            this.p = Pattern.compile(pattern);
            this.type = type;
        }

        public boolean visit(Car car) {
            return this.type == ObservationType.CAR && this.p.matcher(car.getId()).find();
        }


        public boolean visit(Person person) {
            return this.type == ObservationType.PERSON && this.p.matcher(person.getId()).find();
        }
    }
}
