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
        LogEntry le = receiveUpdate(request.getOpId(), request.getPrev());

        if (le != null) {
            try {
                Cam cam = camFromGRPC(request.getCam());
                this.silo.registerCam(cam);
                le.setCommand(new CamJoinCommand(this.silo, cam));
                this.gossipStructures.updateStructures(le);

            } catch(DuplicateCameraNameException e) {
                responseObserver.onError(Status.ALREADY_EXISTS.asRuntimeException());
                return;
            } catch(EmptyCameraNameException | InvalidCameraNameException | InvalidCameraCoordsException e) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription(e.getMessage())
                        .asRuntimeException());
                return;
            }
        }
        pt.tecnico.sauron.silo.grpc.Silo.JoinResponse response = createJoinResponse();
        responseObserver.onNext(response);
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
        LogEntry le = receiveUpdate(request.getOpId(), request.getPrev());
        int numAcked = 0;
        if (le != null) {

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
            Instant regInstant = null;
            for (pt.tecnico.sauron.silo.grpc.Silo.Observation observation : request.getObservationsList()) {
                try {
                    Observation o = observationFromGRPC(observation);
                    obsList.add(o);
                    silo.registerObservation(cam, o);
                    regInstant = Instant.now();
                    numAcked += 1;
                } catch (InvalidCarIdException
                        | InvalidPersonIdException
                        | TypeNotSupportedException e) {
                    exceptions.addException(e);
                }

                le.setCommand(new ReportCommand(this.silo, cam, obsList, regInstant));
                this.gossipStructures.updateStructures(le);
            }


            if (!exceptions.isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription(exceptions.getMessage())
                        .asRuntimeException());
                return;
            }
        }

        responseObserver.onNext(createReportResponse(numAcked));
        responseObserver.onCompleted();
    }

    // ===================================================
    // HELPER FUNCTIONS
    // ===================================================

    private LogEntry receiveUpdate(String opID, pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp prev) {
        // Check if it has been executed before
        if (!this.gossipStructures.getExecutedOperations().contains(opID)) {
            LogEntry newLe = new LogEntry();
            int instance = this.gossipStructures.getInstance();
            newLe.setReplicaId(instance);
            newLe.setOpId(opID);
            newLe.setPrev(vectorTimestampFromGRPC(prev));
            // increment replicaTS
            VectorTimestamp replicaTS = this.gossipStructures.getReplicaTS();
            int newVal = replicaTS.get(this.gossipStructures.getInstance() - 1) + 1;
            replicaTS.set(this.gossipStructures.getInstance() - 1, newVal);
            this.gossipStructures.setReplicaTS(replicaTS);
            // create unique TS
            VectorTimestamp uniqueTS = vectorTimestampFromGRPC(prev);
            uniqueTS.set(this.gossipStructures.getInstance() - 1, newVal);
            newLe.setTs(uniqueTS);
            return newLe;
        }
        return null;
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
                .setNew(vecTimestampToGRPC(this.gossipStructures.getValueTS()))
                .build();
    }

    private pt.tecnico.sauron.silo.grpc.Silo.ReportResponse createReportResponse(int numAcked) {
        return pt.tecnico.sauron.silo.grpc.Silo.ReportResponse.newBuilder()
                .setNumAcked(numAcked)
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
