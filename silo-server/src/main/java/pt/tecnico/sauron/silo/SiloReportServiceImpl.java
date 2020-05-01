package pt.tecnico.sauron.silo;

import com.google.type.LatLng;
import io.grpc.Status;
import pt.tecnico.sauron.silo.commands.CamJoinCommand;
import pt.tecnico.sauron.silo.commands.ReportCommand;
import pt.tecnico.sauron.silo.contract.VectorTimestamp;
import pt.tecnico.sauron.silo.contract.exceptions.InvalidVectorTimestampException;
import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.exceptions.*;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;

import java.time.Instant;
import java.util.LinkedList;

public class SiloReportServiceImpl extends ReportServiceGrpc.ReportServiceImplBase {

    private pt.tecnico.sauron.silo.domain.Silo silo;
    private GossipStructures gossipStructures;

    SiloReportServiceImpl(pt.tecnico.sauron.silo.domain.Silo silo, GossipStructures structures) {
        this.silo = silo;
        this.gossipStructures = structures;
    }

    // ===================================================
    // SERVICE IMPLEMENTATION
    // ===================================================
    @Override
    public void camJoin(pt.tecnico.sauron.silo.grpc.Silo.JoinRequest request, io.grpc.stub.StreamObserver<pt.tecnico.sauron.silo.grpc.Silo.JoinResponse> responseObserver) {


        // If it has not been executed before
        if (!this.gossipStructures.getExecutedOperations().contains(request.getOpId()) &&
                !this.gossipStructures.logContainsOp(request.getOpId())) {
            LogEntry le = receiveUpdateAndSetLogEntry(request.getOpId(), request.getPrev());
            try {
                Cam cam = camFromGRPC(request.getCam());

                // If is stable
                if (vectorTimestampFromGRPC(request.getPrev()).lessOrEqualThan(this.gossipStructures.getValueTS())) {
                    this.silo.registerCam(cam);
                    this.gossipStructures.updateStructures(le);
                }

                // add to update log
                le.setCommand(new CamJoinCommand(this.silo, cam));
                this.gossipStructures.addLogEntry(le);

            } catch(DuplicateCameraNameException e) {
                responseObserver.onError(Status.ALREADY_EXISTS.asRuntimeException());
                return;
            } catch(EmptyCameraNameException | InvalidCameraNameException | InvalidCameraCoordsException e) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription(e.getMessage())
                        .asRuntimeException());
                return;
            } catch (InvalidVectorTimestampException e) {
                System.out.println(e.getMessage());
            }
            responseObserver.onNext(createJoinResponse(le.getTs()));
        } else {
            responseObserver.onNext(createJoinResponse(vectorTimestampFromGRPC(request.getPrev())));
        }

        responseObserver.onCompleted();
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
    public void report(pt.tecnico.sauron.silo.grpc.Silo.ReportRequest request, io.grpc.stub.StreamObserver<pt.tecnico.sauron.silo.grpc.Silo.ReportResponse> responseObserver) {
        // If has not been executed before
        int numAcked = 0;
        if (!this.gossipStructures.getExecutedOperations().contains(request.getOpId()) &&
                !this.gossipStructures.logContainsOp(request.getOpId())) {
            LogEntry le = receiveUpdateAndSetLogEntry(request.getOpId(), request.getPrev());

            Cam cam;
            try {
                final String name = request.getCamName();
                cam = this.silo.getCam(name);
            } catch (NoCameraFoundException e) {
                responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
                return;
            }

            // convert repeated observation to observation List
            CompositeSiloException exceptions = new CompositeSiloException();

            LinkedList<Observation> obsList = new LinkedList<>();

            //add to helper list
            for (pt.tecnico.sauron.silo.grpc.Silo.Observation observation : request.getObservationsList()) {
                try {
                    Observation o = observationFromGRPC(observation);
                    obsList.add(o);
                } catch (InvalidCarIdException
                        | InvalidPersonIdException
                        | TypeNotSupportedException e) {
                    exceptions.addException(e);
                }
            }

            //if is stable, execute
            Instant instant = Instant.now();
            try {
                if (vectorTimestampFromGRPC(request.getPrev()).lessOrEqualThan(this.gossipStructures.getValueTS())) {
                    for (Observation o : obsList) {
                        this.silo.registerObservation(cam, o);
                        numAcked++;
                    }
                    this.gossipStructures.updateStructures(le);
                }
            }catch (InvalidVectorTimestampException e) {
                System.out.println(e.getMessage());
            }

            // add to update log
            le.setCommand(new ReportCommand(this.silo, cam.getName(), obsList, instant));
            this.gossipStructures.addLogEntry(le);

            if (!exceptions.isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription(exceptions.getMessage())
                        .asRuntimeException());
                return;
            }
            responseObserver.onNext(createReportResponse(numAcked, le.getTs()));
        } else {
            // If the operation was already executed, return the prev Timestamp
            responseObserver.onNext(createReportResponse(numAcked, vectorTimestampFromGRPC(request.getPrev())));
        }

        responseObserver.onCompleted();
    }

    // ===================================================
    // HELPER FUNCTIONS
    // ===================================================


    private synchronized LogEntry receiveUpdateAndSetLogEntry(String opID, pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp prev) {
        // Check if it has been executed before
        int instance = this.gossipStructures.getInstance();
        // increment replicaTS
        VectorTimestamp replicaTS = this.gossipStructures.getReplicaTS();
        int newVal = replicaTS.get(this.gossipStructures.getInstance() - 1) + 1;
        replicaTS.set(this.gossipStructures.getInstance() - 1, newVal);
        this.gossipStructures.setReplicaTS(replicaTS);
        // create unique TS
        VectorTimestamp uniqueTS = vectorTimestampFromGRPC(prev);
        uniqueTS.set(this.gossipStructures.getInstance() - 1, newVal);
        return new LogEntry(instance, opID, vectorTimestampFromGRPC(prev), uniqueTS);
    }


    // ===================================================
    // CREATE GRPC RESPONSES
    // ===================================================
    private pt.tecnico.sauron.silo.grpc.Silo.JoinResponse createJoinResponse(VectorTimestamp newTS) {
        return pt.tecnico.sauron.silo.grpc.Silo.JoinResponse.newBuilder()
                .setNew(vecTimestampToGRPC(newTS))
                .build();
    }

    private pt.tecnico.sauron.silo.grpc.Silo.InfoResponse createInfoResponse(Cam cam) {
        return pt.tecnico.sauron.silo.grpc.Silo.InfoResponse.newBuilder()
                .setCoords(coordsToGRPC(new Coords(cam.getLat(), cam.getLon())))
                .setNew(vecTimestampToGRPC(this.gossipStructures.getValueTS()))
                .build();
    }

    private pt.tecnico.sauron.silo.grpc.Silo.ReportResponse createReportResponse(int numAcked, VectorTimestamp newTS) {
        return pt.tecnico.sauron.silo.grpc.Silo.ReportResponse.newBuilder()
                .setNumAcked(numAcked)
                .setNew(vecTimestampToGRPC(newTS))
                .build();
    }


    // ===================================================
    // CONVERT BETWEEN DOMAIN AND GRPC
    // ===================================================
    private VectorTimestamp vectorTimestampFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp timestamp) {
        return new VectorTimestamp(timestamp.getTimestampsList());
    }

    private pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp vecTimestampToGRPC(VectorTimestamp ts) {
        return pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp.newBuilder().addAllTimestamps(ts.getValues()).build();
    }

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
