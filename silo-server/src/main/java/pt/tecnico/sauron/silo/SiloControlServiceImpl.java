package pt.tecnico.sauron.silo;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.commands.ClearCommand;
import pt.tecnico.sauron.silo.commands.Command;
import pt.tecnico.sauron.silo.commands.InitCamsCommand;
import pt.tecnico.sauron.silo.commands.InitObsCommand;
import pt.tecnico.sauron.silo.contract.VectorTimestamp;
import pt.tecnico.sauron.silo.contract.exceptions.InvalidVectorTimestampException;
import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.exceptions.*;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

import java.time.Instant;
import java.util.LinkedList;

import static io.grpc.Status.INVALID_ARGUMENT;

public class SiloControlServiceImpl extends ControlServiceGrpc.ControlServiceImplBase {

    private pt.tecnico.sauron.silo.domain.Silo silo;
    private GossipStructures gossipStructures;

    public SiloControlServiceImpl(pt.tecnico.sauron.silo.domain.Silo silo, GossipStructures structures) {
        this.silo = silo;
        this.gossipStructures = structures;
    }



    // ===================================================
    // SERVICE IMPLEMENTATION
    // ===================================================
    @Override
    public void ping(Silo.PingRequest request, StreamObserver<Silo.PingResponse> responseObserver) {
        String input = request.getText();

        if (input == null || input.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(ErrorMessages.BLANK_INPUT).asRuntimeException());
            return;
        }

        String output = "Hello " + input + "!";
        Silo.PingResponse response = createPingResponse(output);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void clear(Silo.ClearRequest request, StreamObserver<Silo.ClearResponse> responseObserver) {
        LogEntry newLe = receiveUpdate(request.getOpId(), request.getPrev());

        if (newLe != null) {
            // add command
            newLe.setCommand(new ClearCommand(this.silo));
            this.silo.clearObservations();
            this.silo.clearCams();
            this.gossipStructures.updateStructures(newLe);
        }

        responseObserver.onNext(Silo.ClearResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void initCams(Silo.InitCamsRequest request, StreamObserver<Silo.InitCamsResponse> responseObserver) {
        CompositeSiloException exceptions = new CompositeSiloException();
        LogEntry newLe = receiveUpdate(request.getOpId(), request.getPrev());
        if (newLe != null) {
            LinkedList<Cam> camList = new LinkedList<>();
            for(Silo.Cam grpcCam : request.getCamsList()) {
                try {
                    Cam cam = camFromGRPC(grpcCam);
                    camList.add(cam);
                    this.silo.registerCam(cam);
                } catch (SiloException e) {
                    exceptions.addException(e);
                }
            }
            newLe.setCommand(new InitCamsCommand(this.silo, camList));
            this.gossipStructures.updateStructures(newLe);
        }

        if(!exceptions.isEmpty()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription(exceptions.getMessage())
                .asRuntimeException());
            return;
        }



        responseObserver.onNext(createInitCamsResponse());
        responseObserver.onCompleted();
    }

    @Override
    public void initObservations(Silo.InitObservationsRequest request, StreamObserver<Silo.InitObservationsResponse> responseObserver) {
        CompositeSiloException exceptions = new CompositeSiloException();
        LogEntry newLe = receiveUpdate(request.getOpId(), request.getPrev());
        if (newLe != null) {
            LinkedList<Report> reportList = new LinkedList<>();
            for (Silo.InitObservationsItem observation : request.getObservationsList()) {
                try {
                    Report report = reportFromGRPC(observation);
                    reportList.add(report);
                    this.silo.recordReport(report);
                } catch (SiloException e) {
                    exceptions.addException(e);
                }
            }
            newLe.setCommand(new InitObsCommand(this.silo, reportList));
            this.gossipStructures.updateStructures(newLe);
        }


        if(!exceptions.isEmpty()) {
            responseObserver.onError(INVALID_ARGUMENT
            .withDescription(exceptions.getMessage())
            .asRuntimeException());
            return;
        }

        responseObserver.onNext(createInitObservationsResponse());
        responseObserver.onCompleted();
    }


    // ===================================================
    // HELPER FUNCTIONS
    // ===================================================

    private LogEntry receiveUpdate(String opID, Silo.VecTimestamp prev) {
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
    private Silo.PingResponse createPingResponse(String output) {
        return Silo.PingResponse.newBuilder()
                .setText(output)
                .build();
    }

    private Silo.InitCamsResponse createInitCamsResponse() {
        return Silo.InitCamsResponse.getDefaultInstance();
    }

    private Silo.InitObservationsResponse createInitObservationsResponse() {
        return Silo.InitObservationsResponse.getDefaultInstance();
    }

    // ===================================================
    // CONVERT BETWEEN DOMAIN AND GRPC
    // ===================================================
    private VectorTimestamp vectorTimestampFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp timestamp) {
        return new VectorTimestamp(timestamp.getTimestampsList());
    }

    private Report reportFromGRPC(Silo.InitObservationsItem report) throws SiloInvalidArgumentException, EmptyCameraNameException, InvalidCameraNameException {
        Cam cam = camFromGRPC(report.getCam());
        Observation obs = observationFromGRPC(report.getObservation());
        Instant timestamp = instantFromGRPC(report.getTimestamp());

        return new Report(cam, obs, timestamp);
    }

    private Observation observationFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.Observation observation) throws SiloInvalidArgumentException {
        String id = observation.getObservationId();

        switch (observation.getType()) {
            case PERSON:
                return new Person(id);
            case CAR:
                return new Car(id);
            default:
                throw new SiloInvalidArgumentException(ErrorMessages.UNIMPLEMENTED_OBSERVATION_TYPE);
        }
    }

    private Cam camFromGRPC(Silo.Cam cam) throws EmptyCameraNameException, InvalidCameraNameException {
        String name = cam.getName();
        Coords coords = coordsFromGRPC(cam.getCoords());
        return new Cam(name, coords);
    }

    private Coords coordsFromGRPC(LatLng coords) {
        return new Coords(coords.getLatitude(), coords.getLongitude());
    }

    private Instant instantFromGRPC(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds());
    }
}
