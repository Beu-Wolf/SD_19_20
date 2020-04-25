package pt.tecnico.sauron.silo.client;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.client.domain.FrontendCam;
import pt.tecnico.sauron.silo.client.domain.FrontendCoords;
import pt.tecnico.sauron.silo.client.domain.FrontendObservation;
import pt.tecnico.sauron.silo.client.domain.FrontendReport;
import pt.tecnico.sauron.silo.client.exceptions.*;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.QueryServiceGrpc;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SiloFrontend {
    private ManagedChannel channel;
    private ZKNaming zkNaming;
    private ControlServiceGrpc.ControlServiceBlockingStub ctrlBlockingStub;
    private ControlServiceGrpc.ControlServiceStub ctrlStub;
    private ReportServiceGrpc.ReportServiceStub reportStub;
    private QueryServiceGrpc.QueryServiceStub queryStub;
    private QueryServiceGrpc.QueryServiceBlockingStub queryBlockingStub;
    private ReportServiceGrpc.ReportServiceBlockingStub reportBlockingStub;
    public static final Metadata.Key<String> METADATA_CAM_NAME = Metadata.Key.of("name", Metadata.ASCII_STRING_MARSHALLER);

    public static final String SERVER_PATH = "/grpc/sauron/silo";


    public SiloFrontend(String zooHost, String zooPort) throws ZKNamingException, FrontendException {
        zkNaming = new ZKNaming(zooHost,zooPort);
        Collection<ZKRecord> records = zkNaming.listRecords(SERVER_PATH);
        Optional<ZKRecord> optRecord = getRandomRecord(records);
        if (optRecord.isPresent()) {
            ZKRecord record = optRecord.get();
            siloInfo(record);
        } else {
            throw new FrontendException(ErrorMessages.NO_ONLINE_SERVERS);
        }
    }

    public SiloFrontend(String zooHost, String zooPort, Integer instance) throws ZKNamingException {
        zkNaming = new ZKNaming(zooHost, zooPort);
        String path = SERVER_PATH + "/" + instance.toString();
        ZKRecord record = zkNaming.lookup(path);
        siloInfo(record);
    }

    private void siloInfo(ZKRecord record) {
        String target = record.getURI();
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        this.ctrlStub = ControlServiceGrpc.newStub(this.channel);
        this.ctrlBlockingStub = ControlServiceGrpc.newBlockingStub(this.channel);

        this.queryStub = QueryServiceGrpc.newStub(this.channel);
        this.queryBlockingStub = QueryServiceGrpc.newBlockingStub(this.channel);

        this.reportStub = ReportServiceGrpc.newStub(this.channel);
        this.reportBlockingStub = ReportServiceGrpc.newBlockingStub(this.channel);
    }

    public void shutdown() {
        this.channel.shutdown();
    }

    public static <ZKRecord> Optional<ZKRecord> getRandomRecord(Collection<ZKRecord> e) {
        return e.stream()
                .skip((int) (e.size()* Math.random()))
                .findFirst();
    }

    public void makeNewConnection() throws ZKNamingException {
        Collection<ZKRecord> records = zkNaming.listRecords(SERVER_PATH);
        ZKRecord record = ((ArrayList<ZKRecord>) records).get(new Random().nextInt(records.size()));
        siloInfo(record);
    }



    // ===================================================
    // GRPC FRONTEND
    // ===================================================
    public String ctrlPing(String sentence) throws FrontendException, ZKNamingException {
        Silo.PingRequest request = createPingRequest(sentence);
        try {
            Silo.PingResponse response = this.ctrlBlockingStub.ping(request);
            return response.getText();
        } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            if (status.getCode() == Status.Code.UNAVAILABLE) {
                makeNewConnection();
                throw new FrontendException(ErrorMessages.NO_CONNECTION);
            }
            throw new PingException(e.getStatus().getDescription());
        }
    }


    public void ctrlInitCams(List<FrontendCam> cams) throws RuntimeException, FrontendException {
        Silo.InitCamsRequest request = createInitCamsRequest(cams);

        try {
            this.ctrlBlockingStub.initCams(request);
        } catch(RuntimeException e) {
            throw new FrontendException(e.getMessage());
        }
    }

    public void ctrlInitObservations(List<FrontendReport> reports) throws RuntimeException, ZKNamingException, FrontendException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<Silo.InitObservationResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(Silo.InitObservationResponse response) {}

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };

        StreamObserver<Silo.InitObservationRequest> requestObserver = this.ctrlStub.initObservations(responseObserver);
        try {
            for(FrontendReport report : reports) {
                Silo.InitObservationRequest request = createInitObservationRequest(report);
                requestObserver.onNext(request);

                // As per the documentation for client side streaming in gRPC,
                // we should sleep for an amount of time between each call
                // to allow for the server to send an error if it happens
                Thread.sleep(10);

                if (latch.getCount() == 0) {
                    return;
                }
            }
            requestObserver.onCompleted();
            latch.await(10, TimeUnit.SECONDS);
        } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            if (status.getCode() == Status.Code.UNAVAILABLE) {
                makeNewConnection();
                throw new FrontendException(ErrorMessages.NO_CONNECTION);
            }
            throw e;
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        } catch (InterruptedException e) {
            requestObserver.onError(e);
            Thread.currentThread().interrupt();
        }
    }

    public void ctrlClear() throws FrontendException, ZKNamingException{
        try {
            Silo.ClearResponse response =  this.ctrlBlockingStub.clear(createClearRequest());
        } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            if (status.getCode() == Status.Code.UNAVAILABLE) {
                makeNewConnection();
                throw new FrontendException(ErrorMessages.NO_CONNECTION);
            }
            throw new ClearException(e.getStatus().getDescription());
        }
    }


    public void camJoin(FrontendCam cam) throws ZKNamingException, FrontendException {
        Silo.JoinRequest request = createJoinRequest(cam);

        try {
            this.reportBlockingStub.camJoin(request);
        } catch(RuntimeException e) {
            Status status = Status.fromThrowable(e);
            if (status.getCode() == Status.Code.UNAVAILABLE) {
                makeNewConnection();
                throw new FrontendException(ErrorMessages.NO_CONNECTION);
            }
            if(status.getCode() == Status.Code.ALREADY_EXISTS) {
                throw new CameraAlreadyExistsException();
            }
            if(status.getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new CameraRegisterException(status.getDescription());
            }
            throw new CameraRegisterException();
        }
    }

    public FrontendCoords camInfo(String name) throws FrontendException, ZKNamingException {
        Silo.InfoRequest request = createInfoRequest(name);

        try {
            Silo.InfoResponse response = this.reportBlockingStub.camInfo(request);
            return new FrontendCoords(response.getCoords().getLatitude(), response.getCoords().getLongitude());
        } catch (RuntimeException e) {
            Status status = Status.fromThrowable(e);
            if (status.getCode() == Status.Code.UNAVAILABLE) {
                makeNewConnection();
                throw new FrontendException(ErrorMessages.NO_CONNECTION);
            }
            if(status == Status.NOT_FOUND) {
                throw new CameraNotFoundException();
            }
            throw new CameraInfoException();
        }
    }

    public int report(String name, List<FrontendObservation> observations)
            throws CameraNotFoundException, ReportException, CompositeException {
        if(observations.size() == 0) return 0;
        Silo.ReportRequest request = createReportRequest(name, observations);

        Silo.ReportResponse response;
        try {
            response = this.reportBlockingStub.report(request);
        } catch (RuntimeException e) {
            Status status = Status.fromThrowable(e);
            switch (status.getCode()) {
                case NOT_FOUND:
                    throw new CameraNotFoundException();
                default:
                    throw new ReportException(status.getDescription());
            }
        }
        if(response.getErrorsCount() > 0) {
            CompositeException e = new CompositeException();
            List<String> errors = response.getErrorsList();
            for(String err : errors) {
                e.addException(new FrontendException(err));
            }
            throw e;
        }

        return response.getNumAcked();
    }


    public FrontendReport track(FrontendObservation.ObservationType type, String id) throws FrontendException, ZKNamingException {
        Silo.TrackRequest request = createTrackRequest(type, id);

        try {
            return reportFromTrackResponse(queryBlockingStub.track(request));
        } catch(StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            if(status.getCode() == Status.Code.UNAVAILABLE) {
                makeNewConnection();
                throw new FrontendException(ErrorMessages.NO_CONNECTION);
            }
            if (status.getCode() == Status.Code.NOT_FOUND) {
                throw new NotFoundException();
            }
            if(status.getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new InvalidArgumentException(status.getDescription());
            }

            throw new QueryException();
        }
    }

    public List<FrontendReport> trackMatch(FrontendObservation.ObservationType type, String query) throws FrontendException, ZKNamingException {
        LinkedList<FrontendReport> results = new LinkedList<>();
        Silo.TrackMatchRequest request = createTrackMatchRequest(type, query);

        try {
            Iterator<Silo.TrackMatchResponse> it = queryBlockingStub.trackMatch(request);
            while (it.hasNext()) {
                results.push(reportFromTrackMatchResponse(it.next()));
            }
            return results;
        } catch(StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            if(status.getCode() == Status.Code.UNAVAILABLE) {
                makeNewConnection();
                throw new FrontendException(ErrorMessages.NO_CONNECTION);
            }
            if (status.getCode() == Status.Code.NOT_FOUND) {
                throw new NotFoundException();
            }
            if (status.getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new InvalidArgumentException(status.getDescription());
            }

            throw new QueryException();
        }
    }

    public List<FrontendReport> trace(FrontendObservation.ObservationType type, String id) throws FrontendException, ZKNamingException {
        LinkedList<FrontendReport> results = new LinkedList<>();
        Silo.TraceRequest request = createTraceRequest(type, id);

        try {
            Iterator<Silo.TraceResponse> it = queryBlockingStub.trace(request);
            while (it.hasNext()) {
                results.addLast(reportFromTraceResponse(it.next()));
            }
            return results;
        } catch(StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            if(status.getCode() == Status.Code.UNAVAILABLE) {
                makeNewConnection();
                throw new FrontendException(ErrorMessages.NO_CONNECTION);
            }
            if (status.getCode() == Status.Code.NOT_FOUND) {
                throw new NotFoundException();
            }
            if (status.getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new InvalidArgumentException(status.getDescription());
            }

            throw new QueryException();
        }
    }




    // ===================================================
    // CREATE GRPC REQUESTS
    // ===================================================
    private Silo.PingRequest createPingRequest(String sentence) {
        return Silo.PingRequest.newBuilder().setText(sentence).build();
    }

    private Silo.InitCamsRequest createInitCamsRequest(List<FrontendCam> cams) {
        return Silo.InitCamsRequest.newBuilder()
                .addAllCams(cams.stream()
                        .map(this::camToGRPC)
                        .collect(Collectors.toList()))
                .build();
    }

    private Silo.InitObservationRequest createInitObservationRequest(FrontendReport report) {
        return Silo.InitObservationRequest.newBuilder()
                .setCam(camToGRPC(report.getCam()))
                .setObservation(observationToGRPC(report.getObservation()))
                .setTimestamp(timestampToGRPC(report.getTimestamp()))
                .build();
    }

    private Silo.ClearRequest createClearRequest() {
        return Silo.ClearRequest.getDefaultInstance();
    }


    private Silo.JoinRequest createJoinRequest(FrontendCam cam) {
        return Silo.JoinRequest.newBuilder()
                .setCam(camToGRPC(cam))
                .build();
    }

    private Silo.InfoRequest createInfoRequest(String name) {
        return Silo.InfoRequest.newBuilder().setName(name).build();
    }

    private Silo.ReportRequest createReportRequest(String camName, List<FrontendObservation> observations) {
        return Silo.ReportRequest.newBuilder()
                .setCamName(camName)
                .addAllObservations(observations.stream().map(this::observationToGRPC).collect(Collectors.toList()))
                .build();
    }


    private Silo.TrackRequest createTrackRequest(FrontendObservation.ObservationType type, String id) {
        return Silo.TrackRequest.newBuilder()
                .setType(observationTypeToGRPC(type))
                .setId(id).build();
    }

    private Silo.TrackMatchRequest createTrackMatchRequest(FrontendObservation.ObservationType type, String pattern) {
        return Silo.TrackMatchRequest.newBuilder()
                .setType(observationTypeToGRPC(type))
                .setPattern(pattern).build();
    }

    private Silo.TraceRequest createTraceRequest(FrontendObservation.ObservationType type, String id) {
        return Silo.TraceRequest.newBuilder()
                .setType(observationTypeToGRPC(type))
                .setId(id).build();
    }


    // ===================================================
    // CONVERT BETWEEN DTO AND GRPC
    // ===================================================
    private FrontendReport reportFromTrackResponse(Silo.TrackResponse response) {
        FrontendObservation frontendObservation = observationFromGRPC(response.getObservation());
        FrontendCam frontendCam = camFromGRPC(response.getCam());
        Instant timestamp = timestampFromGRPC(response.getTimestamp());

        return new FrontendReport(frontendObservation, frontendCam, timestamp);
    }

    private FrontendReport reportFromTrackMatchResponse(Silo.TrackMatchResponse response) {
        FrontendObservation frontendObservation = observationFromGRPC(response.getObservation());
        FrontendCam frontendCam = camFromGRPC(response.getCam());
        Instant timestamp = timestampFromGRPC(response.getTimestamp());

        return new FrontendReport(frontendObservation, frontendCam, timestamp);
    }

    private FrontendReport reportFromTraceResponse(Silo.TraceResponse response) {
        FrontendObservation frontendObservation = observationFromGRPC(response.getObservation());
        FrontendCam frontendCam = camFromGRPC(response.getCam());
        Instant timestamp = timestampFromGRPC(response.getTimestamp());

        return new FrontendReport(frontendObservation, frontendCam, timestamp);
    }

    private Silo.Observation observationToGRPC(FrontendObservation observation) {
        return Silo.Observation.newBuilder()
                .setObservationId(observation.getId())
                .setType(observationTypeToGRPC(observation.getType()))
                .build();
    }
    private FrontendObservation observationFromGRPC(Silo.Observation observation) {
        return new FrontendObservation(observationTypeFromGRPC(observation.getType()),
                observation.getObservationId());
    }

    private Silo.ObservationType observationTypeToGRPC(FrontendObservation.ObservationType type) {
        switch(type) {
            case CAR:
                return Silo.ObservationType.CAR;
            case PERSON:
                return Silo.ObservationType.PERSON;
            default:
                return Silo.ObservationType.UNSPEC;
        }
    }
    private FrontendObservation.ObservationType observationTypeFromGRPC(Silo.ObservationType type) {
        switch(type) {
            case CAR:
                return FrontendObservation.ObservationType.CAR;
            case PERSON:
                return FrontendObservation.ObservationType.PERSON;
            default:
                return FrontendObservation.ObservationType.UNSPEC;
        }
    }

    private Silo.Cam camToGRPC(FrontendCam cam) {
        return Silo.Cam.newBuilder()
                .setName(cam.getName())
                .setCoords(coordsToGRPC(cam.getCoords()))
                .build();
    }
    private FrontendCam camFromGRPC(Silo.Cam cam) {
        String name = cam.getName();
        LatLng coords = cam.getCoords();

        return new FrontendCam(name, coords.getLatitude(), coords.getLongitude());
    }

    private LatLng coordsToGRPC(FrontendCoords coords) {
        return LatLng.newBuilder()
                .setLatitude(coords.getLat())
                .setLongitude(coords.getLon())
                .build();
    }

    private Timestamp timestampToGRPC(Instant timestamp) {
        return Timestamp.newBuilder()
                .setSeconds(timestamp.getEpochSecond())
                .build();
    }
    private Instant timestampFromGRPC(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds());
    }
}