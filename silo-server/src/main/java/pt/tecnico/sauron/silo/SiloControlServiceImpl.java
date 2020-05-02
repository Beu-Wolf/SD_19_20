package pt.tecnico.sauron.silo;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
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
    public synchronized void clear(Silo.ClearRequest request, StreamObserver<Silo.ClearResponse> responseObserver) {
        this.silo.clearObservations();
        this.silo.clearCams();
        this.gossipStructures.clearAll();
        responseObserver.onNext(Silo.ClearResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void initCams(Silo.InitCamsRequest request, StreamObserver<Silo.InitCamsResponse> responseObserver) {
        CompositeSiloException exceptions = new CompositeSiloException();

        // if is not executed yet
        if (!this.gossipStructures.getExecutedOperations().contains(request.getOpId())) {
            LogEntry newLe = receiveUpdateAndSetLogEntry(request.getOpId(), request.getPrev());
            LinkedList<Cam> camList = new LinkedList<>();

            // add cam to a helper list
            for(Silo.Cam grpcCam : request.getCamsList()) {
                try {
                    camList.add(camFromGRPC(grpcCam));
                } catch (SiloException e) {
                    exceptions.addException(e);
                }
            }

            // Check if is stable to execute
            try {
                if (vectorTimestampFromGRPC(request.getPrev()).lessOrEqualThan(this.gossipStructures.getValueTS())) {
                    for(Cam cam : camList) {
                        this.silo.registerCam(cam);
                    }
                    this.gossipStructures.updateStructures(newLe);
                }
            } catch (InvalidVectorTimestampException e) {
                System.out.println(e.getMessage());
            } catch (SiloException e) {
                exceptions.addException(e);
            }

            // add to update log
            newLe.setCommand(new InitCamsCommand(this.silo, camList));
            this.gossipStructures.addLogEntry(newLe);
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

        // If is not executed yet
        if (!this.gossipStructures.getExecutedOperations().contains(request.getOpId())) {
            LogEntry newLe = receiveUpdateAndSetLogEntry(request.getOpId(), request.getPrev());
            LinkedList<Report> reportList = new LinkedList<>();

            // put in helper list
            for (Silo.InitObservationsItem observation : request.getObservationsList()) {
                try {
                    reportList.add(reportFromGRPC(observation));
                } catch (SiloException e) {
                    exceptions.addException(e);
                }
            }

            // Check if is stable
            try {
                if (vectorTimestampFromGRPC(request.getPrev()).lessOrEqualThan(this.gossipStructures.getValueTS())) {
                    for (Report report : reportList) {
                        this.silo.recordReport(report);
                    }
                    this.gossipStructures.updateStructures(newLe);
                }
            } catch (InvalidVectorTimestampException e) {
                System.out.println(e.getMessage());
            }

            // add to update Log
            newLe.setCommand(new InitObsCommand(this.silo, reportList));
            this.gossipStructures.addLogEntry(newLe);
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

    private LogEntry receiveUpdateAndSetLogEntry(String opID, Silo.VecTimestamp prev) {
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
