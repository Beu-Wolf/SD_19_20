package pt.tecnico.sauron.silo;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.sauron.silo.contract.domain.Cam;
import pt.sauron.silo.contract.domain.Observation;
import pt.sauron.silo.contract.domain.*;
import pt.sauron.silo.contract.domain.exceptions.InvalidCarIdException;
import pt.sauron.silo.contract.domain.exceptions.InvalidPersonIdException;
import pt.tecnico.sauron.silo.exceptions.ObservationNotFoundServerException;
import pt.tecnico.sauron.silo.exceptions.TypeNotSupportedServerException;
import pt.tecnico.sauron.silo.grpc.QueryServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.*;

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
    public void track(TrackRequest request, StreamObserver<TrackResponse> responseObserver) {
        String id = request.getId();
        ObservationType type = request.getType();

        try {
            Observation observation = observationFromGRPC(type, id);
            Report report = silo.track(observation);

            TrackResponse response = reportToTrackResponse(report);
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (InvalidPersonIdException
                |InvalidCarIdException
                |TypeNotSupportedServerException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (ObservationNotFoundServerException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void trackMatch(TrackMatchRequest request, StreamObserver<TrackMatchResponse> responseObserver) {
        String pattern = request.getPattern();
        ObservationType type = request.getType();

        try {
            TreeSet<String> matched = new TreeSet<>();
            TrackMatchComparator comparator = new TrackMatchComparator(type, pattern);

            for (Report report : silo.getReportsByNew()) {
                Observation observation = report.getObservation();
                String id = observation.getId();

                if (!matched.contains(id) && observation.matches(comparator)) {
                    matched.add(id);

                    TrackMatchResponse response = reportToTrackMatchResponse(report);
                    responseObserver.onNext(response);
                }
            }

            if (matched.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            } else {
                responseObserver.onCompleted();
            }
        } catch (TypeNotSupportedServerException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
            return;
        }
    }

    @Override
    public void trace(TraceRequest request, StreamObserver<TraceResponse> responseObserver) {
        ObservationType type = request.getType();
        String queryId = request.getId();
        boolean found = false;

        try {
            Observation queryObservation = observationFromGRPC(type, queryId);

            for (Report report : silo.getReportsByNew()) {
                Observation observation = report.getObservation();

                if (observation.equals(queryObservation)) {
                    found = true;
                    responseObserver.onNext(reportToTraceResponse(report));
                }
            }
        } catch (InvalidPersonIdException
                |TypeNotSupportedServerException
                |InvalidCarIdException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
            return;
        }

        if (!found) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
        } else {
            responseObserver.onCompleted();
        }
    }

    // ===================================================
    // CONVERT BETWEEN DOMAIN AND GRPC
    // ===================================================
    private TrackResponse reportToTrackResponse(Report report) throws TypeNotSupportedServerException {
        return TrackResponse.newBuilder()
                .setCam(camFromGRPC(report.getCam()))
                .setObservation(observationToGRPC(report.getObservation()))
                .setTimestamp(timestampToGRPC(report.getTimestamp()))
                .build();
    }

    private TrackMatchResponse reportToTrackMatchResponse(Report report) throws TypeNotSupportedServerException {
        return TrackMatchResponse.newBuilder()
                .setCam(camFromGRPC(report.getCam()))
                .setObservation(observationToGRPC(report.getObservation()))
                .setTimestamp(timestampToGRPC(report.getTimestamp()))
                .build();
    }

    private TraceResponse reportToTraceResponse(Report report) throws TypeNotSupportedServerException {
        return TraceResponse.newBuilder()
                .setCam(camFromGRPC(report.getCam()))
                .setObservation(observationToGRPC(report.getObservation()))
                .setTimestamp(timestampToGRPC(report.getTimestamp()))
                .build();
    }

    private pt.tecnico.sauron.silo.grpc.Silo.Observation observationToGRPC(Observation observation) throws TypeNotSupportedServerException {
        return pt.tecnico.sauron.silo.grpc.Silo.Observation.newBuilder()
                .setType(domainObservationToTypeGRPC(observation))
                .setObservationId(observation.getId()).build();
    }

    private ObservationType domainObservationToTypeGRPC(Observation observation) throws TypeNotSupportedServerException {
        if (observation instanceof Car) {
            return ObservationType.CAR;
        } else if (observation instanceof Person) {
            return ObservationType.PERSON;
        } else {
            throw new TypeNotSupportedServerException();
        }
    }

    private pt.tecnico.sauron.silo.grpc.Silo.Cam camFromGRPC(Cam cam) {
        LatLng coords = LatLng.newBuilder().setLatitude(cam.getCoords().getLat())
                .setLongitude(cam.getCoords().getLon()).build();

        return pt.tecnico.sauron.silo.grpc.Silo.Cam.newBuilder().setCoords(coords)
                .setName(cam.getName()).build();
    }

    private Observation observationFromGRPC(ObservationType type, String id)
            throws InvalidPersonIdException, InvalidCarIdException, TypeNotSupportedServerException {
        switch (type) {
            case PERSON:
                return new Person(id);
            case CAR:
                return new Car(id);
            default:
                throw new TypeNotSupportedServerException();
        }
    }
    private Observation observationFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.Observation observation)
            throws InvalidPersonIdException, InvalidCarIdException, TypeNotSupportedServerException {
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

        TrackMatchComparator(ObservationType type, String pattern) throws TypeNotSupportedServerException {
            if(type == ObservationType.UNSPEC) {
                throw new TypeNotSupportedServerException();
            }
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
