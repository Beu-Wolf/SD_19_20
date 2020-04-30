package pt.tecnico.sauron.silo;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.stub.StreamObserver;
import io.grpc.Status;
import pt.tecnico.sauron.silo.contract.VectorTimestamp;
import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.exceptions.ObservationNotFoundException;
import pt.tecnico.sauron.silo.exceptions.SiloInvalidArgumentException;
import pt.tecnico.sauron.silo.grpc.QueryServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.TrackRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TraceRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TraceResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ObservationType;

import java.time.Instant;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class SiloQueryServiceImpl extends QueryServiceGrpc.QueryServiceImplBase {
    private Silo silo;
    private GossipStructures gossipStructures;

    public SiloQueryServiceImpl(Silo silo, GossipStructures structures) {
        this.silo = silo;
        this.gossipStructures = structures;
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

            TrackResponse response = TrackResponse.newBuilder()
                    .setReport(reportToGRPC(report)).setNew(vecTimestampToGRPC(this.gossipStructures.getValueTS())).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (SiloInvalidArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (ObservationNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void trackMatch(TrackMatchRequest request, StreamObserver<TrackMatchResponse> responseObserver) {
        String pattern = request.getPattern();
        ObservationType type = request.getType();
        LinkedList<pt.tecnico.sauron.silo.grpc.Silo.Report> reports = new LinkedList<>();

        try {
            TreeSet<String> matched = new TreeSet<>();
            TrackMatchComparator comparator = new TrackMatchComparator(type, pattern);

            for (Report report : silo.getReportsByNew()) {
                Observation observation = report.getObservation();
                String id = observation.getId();

                if (!matched.contains(id) && observation.matches(comparator)) {
                    matched.add(id);
                    reports.add(reportToGRPC(report));
                }
            }

            if (matched.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            } else {
                TrackMatchResponse response = TrackMatchResponse.newBuilder()
                        .addAllReports(reports).setNew(vecTimestampToGRPC(this.gossipStructures.getValueTS())).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (SiloInvalidArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void trace(TraceRequest request, StreamObserver<TraceResponse> responseObserver) {
        ObservationType type = request.getType();
        String queryId = request.getId();
        LinkedList<pt.tecnico.sauron.silo.grpc.Silo.Report> reports = new LinkedList<>();
        boolean found = false;

        try {
            Observation queryObservation = observationFromGRPC(type, queryId);

            for (Report report : silo.getReportsByNew()) {
                Observation observation = report.getObservation();

                if (observation.equals(queryObservation)) {
                    found = true;
                    reports.add(reportToGRPC(report));
                }
            }
        } catch (SiloInvalidArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
            return;
        }

        if (!found) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
        } else {
            TraceResponse response = TraceResponse.newBuilder()
                    .addAllReports(reports).setNew(vecTimestampToGRPC(this.gossipStructures.getValueTS())).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    // ===================================================
    // CONVERT BETWEEN DOMAIN AND GRPC
    // ===================================================

    private pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp vecTimestampToGRPC(VectorTimestamp ts) {
        return pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp.newBuilder().addAllTimestamps(ts.getValues()).build();
    }

    private pt.tecnico.sauron.silo.grpc.Silo.Report reportToGRPC(Report report) throws SiloInvalidArgumentException {
        return pt.tecnico.sauron.silo.grpc.Silo.Report.newBuilder()
                .setCam(camToGRPC(report.getCam()))
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

    private pt.tecnico.sauron.silo.grpc.Silo.Cam camToGRPC(Cam cam) {
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

        TrackMatchComparator(ObservationType type, String pattern) throws SiloInvalidArgumentException {
            if(type == ObservationType.UNSPEC) {
                throw new SiloInvalidArgumentException(ErrorMessages.UNIMPLEMENTED_OBSERVATION_TYPE);
            }
            // escape every regex character
            pattern = Pattern.quote(pattern);

            // un-escape *
            pattern = pattern.replace("*", "\\E.*\\Q");

            // make pattern match the entire string
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
