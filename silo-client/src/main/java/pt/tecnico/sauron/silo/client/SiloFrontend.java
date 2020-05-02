package pt.tecnico.sauron.silo.client;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.*;
import pt.tecnico.sauron.silo.client.domain.FrontendCam;
import pt.tecnico.sauron.silo.client.domain.FrontendCoords;
import pt.tecnico.sauron.silo.client.domain.FrontendObservation;
import pt.tecnico.sauron.silo.client.domain.FrontendReport;
import pt.tecnico.sauron.silo.client.exceptions.*;
import pt.tecnico.sauron.silo.contract.VectorTimestamp;
import pt.tecnico.sauron.silo.contract.exceptions.InvalidVectorTimestampException;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.QueryServiceGrpc;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class SiloFrontend {

    private static final int NUM_RETRIES = 10;
    private int numReplicas;
    private VectorTimestamp frontendTS;
    private int opCount = 0;
    private String uuid = UUID.randomUUID().toString();
    private ManagedChannel channel;
    private ZKNaming zkNaming;
    private ControlServiceGrpc.ControlServiceBlockingStub ctrlBlockingStub;
    private ControlServiceGrpc.ControlServiceStub ctrlStub;
    private ReportServiceGrpc.ReportServiceStub reportStub;
    private QueryServiceGrpc.QueryServiceStub queryStub;
    private QueryServiceGrpc.QueryServiceBlockingStub queryBlockingStub;
    private ReportServiceGrpc.ReportServiceBlockingStub reportBlockingStub;

    public static final String SERVER_PATH = "/grpc/sauron/silo";
    public static final String PROPERTIES = "/main.properties";

    private static Properties gossipProperties = new Properties();

    // caches for server responses
    private HashMap<FrontendObservation.ObservationType, HashMap<String, FrontendReport>> trackCache = new HashMap<>();
    private HashMap<FrontendObservation.ObservationType, HashMap<String, List<FrontendReport>>> trackMatchCache = new HashMap<>();
    private HashMap<FrontendObservation.ObservationType, HashMap<String, List<FrontendReport>>> traceCache = new HashMap<>();

    // initialize frontend cache
    private void initCache() {
        trackCache.put(FrontendObservation.ObservationType.CAR, new HashMap<>());
        trackCache.put(FrontendObservation.ObservationType.PERSON, new HashMap<>());

        trackMatchCache.put(FrontendObservation.ObservationType.CAR, new HashMap<>());
        trackMatchCache.put(FrontendObservation.ObservationType.PERSON, new HashMap<>());

        traceCache.put(FrontendObservation.ObservationType.CAR, new HashMap<>());
        traceCache.put(FrontendObservation.ObservationType.PERSON, new HashMap<>());
    }

    public SiloFrontend(String zooHost, String zooPort) throws ZKNamingException, FrontendException {
        initCache();
        zkNaming = new ZKNaming(zooHost,zooPort);
        Collection<ZKRecord> records = zkNaming.listRecords(SERVER_PATH);
        Optional<ZKRecord> optRecord = getRandomRecord(records);
        // if found running server, connect
        if (optRecord.isPresent()) {
            ZKRecord record = optRecord.get();
            connectToReplica(record);
        } else {
            throw new FrontendException(ErrorMessages.NO_ONLINE_SERVERS);
        }
    }

    public SiloFrontend(String zooHost, String zooPort, Integer instance) throws ZKNamingException {
        initCache();
        zkNaming = new ZKNaming(zooHost, zooPort);
        String path = SERVER_PATH + "/" + instance.toString();
        ZKRecord record = zkNaming.lookup(path);
        connectToReplica(record);
    }

    private void connectToReplica(ZKRecord record) {
        String target = record.getURI();
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        this.ctrlStub = ControlServiceGrpc.newStub(this.channel);
        this.ctrlBlockingStub = ControlServiceGrpc.newBlockingStub(this.channel);

        this.queryStub = QueryServiceGrpc.newStub(this.channel);
        this.queryBlockingStub = QueryServiceGrpc.newBlockingStub(this.channel);

        this.reportStub = ReportServiceGrpc.newStub(this.channel);
        this.reportBlockingStub = ReportServiceGrpc.newBlockingStub(this.channel);

        try {
            // verify number of replicas
            gossipProperties.load(SiloFrontend.class.getResourceAsStream(PROPERTIES));
            this.numReplicas = Integer.parseInt(gossipProperties.getProperty("numReplicas"));
        } catch (IOException e) {
            final String msg = String.format("Could not load properties file {}", PROPERTIES);
            System.out.println(msg);
            shutdown();
            System.exit(-1);
        }
        this.frontendTS = new VectorTimestamp(this.numReplicas);

    }

    public void shutdown() {
        this.channel.shutdown();
    }

    public static <ZKRecord> Optional<ZKRecord> getRandomRecord(Collection<ZKRecord> e) {
        // choose random record from collection
        return e.stream()
                .skip((int) (e.size()* Math.random()))
                .findFirst();
    }

    public void reconnect() throws ZKNamingException {
        // inform the user that previous connection went down
        System.out.println(ErrorMessages.NO_CONNECTION);
        Collection<ZKRecord> records = zkNaming.listRecords(SERVER_PATH);
        ZKRecord record = ((ArrayList<ZKRecord>) records).get(new Random().nextInt(records.size()));
        connectToReplica(record);
    }

    // generate unique operation Id
    private String genOpID() {
        this.opCount++;
        return this.uuid + Integer.toString(this.opCount);
    }

    // ===================================================
    // GRPC FRONTEND
    // ===================================================
    public String ctrlPing(String sentence) throws FrontendException, ZKNamingException {
        int numRetries = 0;
        while(numRetries < NUM_RETRIES) {
            try {
                return executePing(sentence);
            } catch (StatusRuntimeException e) {
                Status status = Status.fromThrowable(e);
                if (status.getCode() == Status.Code.UNAVAILABLE) {
                    reconnect();
                    numRetries++;
                } else {
                    throw new PingException(e.getStatus().getDescription());
                }
            }
        }
        throw new FrontendException(ErrorMessages.NO_ONLINE_SERVERS);
    }

    private String executePing(String sentence) {
        Silo.PingRequest request = createPingRequest(sentence);
        Silo.PingResponse response = this.ctrlBlockingStub.ping(request);
        return response.getText();
    }


    public void ctrlInitCams(List<FrontendCam> cams) throws RuntimeException, FrontendException, ZKNamingException {
        // generate operation ID
        String newOpId = genOpID();
        int numRetries = 0;
        while(numRetries < NUM_RETRIES) {
            try {
                executeInitCams(cams, newOpId);
                return;
            } catch(RuntimeException e) {
                Status status = Status.fromThrowable(e);
                if (status.getCode() == Status.Code.UNAVAILABLE) {
                    reconnect();
                    numRetries++;
                } else {
                    throw new FrontendException(e.getMessage());
                }
            }
        }
    }

    private void executeInitCams(List<FrontendCam> cams, String opID) {
        Silo.InitCamsRequest request = createInitCamsRequest(cams, opID);
        this.ctrlBlockingStub.initCams(request);
    }

    public void ctrlInitObservations(List<FrontendReport> reports) throws FrontendException, ZKNamingException {
        String newOpId = genOpID();
        int numRetries = 0;
        while(numRetries < NUM_RETRIES) {
            try {
                executeInitObs(reports, newOpId);
                return;
            } catch (RuntimeException e) {
                Status status = Status.fromThrowable(e);
                if (status.getCode() == Status.Code.UNAVAILABLE) {
                    reconnect();
                    numRetries++;
                } else {
                    throw new FrontendException(e.getMessage());
                }
            }
        }

    }

    private void executeInitObs(List<FrontendReport> reports, String opID) {
        Silo.InitObservationsRequest request = createInitObservationsRequest(reports, opID);
        this.ctrlBlockingStub.initObservations(request);
    }

    public void ctrlClear() throws FrontendException, ZKNamingException{
        int numRetries = 0;
        while (numRetries < NUM_RETRIES) {
            try {
                executeClear();
                return;
            } catch (StatusRuntimeException e) {
                Status status = Status.fromThrowable(e);
                if (status.getCode() == Status.Code.UNAVAILABLE) {
                    reconnect();
                    numRetries++;
                } else {
                    throw new ClearException(e.getStatus().getDescription());
                }
            }
        }

    }

    private void executeClear() {
        this.ctrlBlockingStub.clear(createClearRequest());
    }


    public void camJoin(FrontendCam cam) throws ZKNamingException, FrontendException {
        String newOpId = genOpID();
        int numRetries = 0;
        while (numRetries < NUM_RETRIES) {
            try {
                executeCamJoin(cam, newOpId);
                return;
            } catch (RuntimeException e) {
                Status status = Status.fromThrowable(e);
                if (status.getCode() == Status.Code.UNAVAILABLE) {
                    reconnect();
                    numRetries++;
                } else if(status.getCode() == Status.Code.ALREADY_EXISTS) {
                    throw new CameraAlreadyExistsException();
                } else if(status.getCode() == Status.Code.INVALID_ARGUMENT) {
                    throw new CameraRegisterException(status.getDescription());
                } else {
                    throw new CameraRegisterException(status.getDescription());
                }
            } catch (InvalidVectorTimestampException e) {
                throw new FrontendException(ErrorMessages.INVALID_VECTOR_TIMESTAMP);
            }
        }
        throw new FrontendException(ErrorMessages.NO_ONLINE_SERVERS);
    }

    private void executeCamJoin(FrontendCam cam, String opID) throws InvalidVectorTimestampException {
        Silo.JoinRequest request = createJoinRequest(cam, opID);
        Silo.JoinResponse response = this.reportBlockingStub.camJoin(request);
        this.frontendTS.merge(vectorTimestampFromGRPC(response.getNew()));
    }

    public FrontendCoords camInfo(String name) throws FrontendException, ZKNamingException {
        int numRetries = 0;
        while (numRetries < NUM_RETRIES) {
            try {
                return executeCamInfo(name);
            } catch (RuntimeException e) {
                Status status = Status.fromThrowable(e);
                if (status.getCode() == Status.Code.UNAVAILABLE) {
                    reconnect();
                    numRetries++;
                } else if(status.getCode() == Status.Code.NOT_FOUND) {
                    throw new CameraNotFoundException();
                } else {
                    throw new CameraInfoException();
                }
            } catch (InvalidVectorTimestampException e) {
                throw new FrontendException(e.getMessage());
            }
        }
        throw new FrontendException(ErrorMessages.NO_ONLINE_SERVERS);
    }

    private FrontendCoords executeCamInfo(String name) throws InvalidVectorTimestampException {
        Silo.InfoRequest request = createInfoRequest(name);
        Silo.InfoResponse response = this.reportBlockingStub.camInfo(request);
        this.frontendTS.merge(vectorTimestampFromGRPC(response.getNew()));
        return new FrontendCoords(response.getCoords().getLatitude(), response.getCoords().getLongitude());
    }

    public int report(FrontendCam cam, List<FrontendObservation> observations)
            throws FrontendException, ZKNamingException {
        if(observations.size() == 0) return 0;
        String newOpId = genOpID();
        int numRetries = 0;
        while (numRetries < NUM_RETRIES) {
            try {
                return executeReport(cam.getName(), observations, newOpId);
            } catch (RuntimeException e) {
                Status status = Status.fromThrowable(e);
                switch (status.getCode()) {
                    case UNAVAILABLE:
                        reconnect();
                        // resend camJoinInformation
                        camJoin(cam);
                        numRetries++;
                        break;
                    case NOT_FOUND:
                        throw new CameraNotFoundException();
                    case INVALID_ARGUMENT:
                        throw new InvalidArgumentException(status.getDescription());
                    default:
                        throw new ReportException(status.getDescription());
                }
            } catch (InvalidVectorTimestampException e) {
                throw new FrontendException(ErrorMessages.INVALID_VECTOR_TIMESTAMP);
            }
        }
        throw new FrontendException(ErrorMessages.NO_ONLINE_SERVERS);
    }

    private int executeReport(String name, List<FrontendObservation> observations, String opID)
        throws InvalidVectorTimestampException {
        Silo.ReportRequest request = createReportRequest(name, observations, opID);
        Silo.ReportResponse response = this.reportBlockingStub.report(request);
        this.frontendTS.merge(vectorTimestampFromGRPC(response.getNew()));
        return response.getNumAcked();
    }


    public FrontendReport track(FrontendObservation.ObservationType type, String id) throws FrontendException, ZKNamingException {
        int numRetries = 0;
        while (numRetries < NUM_RETRIES) {
            try {
                return executeTrack(type, id);
            } catch(StatusRuntimeException e) {
                Status status = Status.fromThrowable(e);
                if(status.getCode() == Status.Code.UNAVAILABLE) {
                    reconnect();
                    numRetries++;
                } else if (status.getCode() == Status.Code.NOT_FOUND) {
                    if (trackCache.get(type).containsKey(id))
                        return trackCache.get(type).get(id);
                    else
                        throw new NotFoundException();
                } else if(status.getCode() == Status.Code.INVALID_ARGUMENT) {
                    throw new InvalidArgumentException(status.getDescription());
                } else {
                    throw new QueryException();
                }
            } catch (InvalidVectorTimestampException e) {
                throw new FrontendException(e.getMessage());
            }
        }
        throw new FrontendException(ErrorMessages.NO_ONLINE_SERVERS);
    }

    private FrontendReport executeTrack(FrontendObservation.ObservationType type, String id) throws InvalidVectorTimestampException {
        Silo.TrackRequest request = createTrackRequest(type, id);
        Silo.TrackResponse response = queryBlockingStub.track(request);
        VectorTimestamp newTS = vectorTimestampFromGRPC(response.getNew());
        if (newTS.lessOrEqualThan(frontendTS)) {
            // WARNING: don't && with above condition.
            // avoiding unnecessary merge
            if (trackCache.get(type).containsKey(id))
                return trackCache.get(type).get(id);
        } else {
            frontendTS.merge(newTS);
        }
        FrontendReport result = reportFromGRPC(response.getReport());
        trackCache.get(type).put(id, result);
        return result;
    }

    public List<FrontendReport> trackMatch(FrontendObservation.ObservationType type, String query) throws FrontendException, ZKNamingException {
        int numRetries = 0;
        while (numRetries < NUM_RETRIES) {
            try {
                return executeTrackMatch(type, query);
            } catch(StatusRuntimeException e) {
                Status status = Status.fromThrowable(e);
                if(status.getCode() == Status.Code.UNAVAILABLE) {
                    reconnect();
                    numRetries++;
                } else if (status.getCode() == Status.Code.NOT_FOUND) {
                    if (trackMatchCache.get(type).containsKey(query))
                        return trackMatchCache.get(type).get(query);
                    else
                        throw new NotFoundException();
                } else if (status.getCode() == Status.Code.INVALID_ARGUMENT) {
                    throw new InvalidArgumentException(status.getDescription());
                } else {
                    throw new QueryException();
                }
            } catch (InvalidVectorTimestampException e) {
                throw new FrontendException(e.getMessage());
            }
        }
        throw new FrontendException(ErrorMessages.NO_ONLINE_SERVERS);
    }

    private List<FrontendReport> executeTrackMatch(FrontendObservation.ObservationType type, String query) throws InvalidVectorTimestampException {
        LinkedList<FrontendReport> results = new LinkedList<>();
        Silo.TrackMatchRequest request = createTrackMatchRequest(type, query);
        Silo.TrackMatchResponse response = queryBlockingStub.trackMatch(request);
        VectorTimestamp newTS = vectorTimestampFromGRPC(response.getNew());
        if (newTS.lessOrEqualThan(this.frontendTS)) {
            if (trackMatchCache.get(type).containsKey(query))
                return trackMatchCache.get(type).get(query);
        } else {
            this.frontendTS.merge(newTS);
        }
        List<Silo.Report> reports = response.getReportsList();
        for (Silo.Report report : reports) {
            results.push(reportFromGRPC(report));
        }
        trackMatchCache.get(type).put(query, results);
        return results;
    }

    public List<FrontendReport> trace(FrontendObservation.ObservationType type, String id) throws FrontendException, ZKNamingException {
        int numRetries = 0;
        while (numRetries < NUM_RETRIES) {
            try {
                return executeTrace(type, id);
            } catch(StatusRuntimeException e) {
                Status status = Status.fromThrowable(e);
                if(status.getCode() == Status.Code.UNAVAILABLE) {
                    reconnect();
                    numRetries++;
                } else if (status.getCode() == Status.Code.NOT_FOUND) {
                    if (traceCache.get(type).containsKey(id))
                        return traceCache.get(type).get(id);
                    else
                        throw new NotFoundException();
                } else if (status.getCode() == Status.Code.INVALID_ARGUMENT) {
                    throw new InvalidArgumentException(status.getDescription());
                } else {
                    throw new QueryException();
                }
            } catch (InvalidVectorTimestampException e) {
                throw new FrontendException(e.getMessage());
            }
        }
        throw new FrontendException(ErrorMessages.NO_ONLINE_SERVERS);
    }

    private List<FrontendReport> executeTrace(FrontendObservation.ObservationType type, String id) throws InvalidVectorTimestampException {
        LinkedList<FrontendReport> results = new LinkedList<>();
        Silo.TraceRequest request = createTraceRequest(type, id);
        Silo.TraceResponse response = queryBlockingStub.trace(request);
        VectorTimestamp newTS = vectorTimestampFromGRPC(response.getNew());
        if (newTS.lessOrEqualThan(this.frontendTS)) {
            if (traceCache.get(type).containsKey(id))
                return traceCache.get(type).get(id);
        } else {
            this.frontendTS.merge(newTS);
        }
        List<Silo.Report> reports = response.getReportsList();
        for (Silo.Report report : reports) {
            results.addLast(reportFromGRPC(report));
        }
        traceCache.get(type).put(id, results);
        return results;
    }



    // ===================================================
    // HELPER FUNCTION
    // ===================================================

    //Since the clear method clears all structures, in the IT tests we need to reset the frontend TS as well
    protected void resetFrontendTS() {
        this.frontendTS = new VectorTimestamp(this.numReplicas);
    }




    // ===================================================
    // CREATE GRPC REQUESTS
    // ===================================================
    private Silo.PingRequest createPingRequest(String sentence) {
        return Silo.PingRequest.newBuilder().setText(sentence).build();
    }

    private Silo.InitCamsRequest createInitCamsRequest(List<FrontendCam> cams, String opId) {
        return Silo.InitCamsRequest.newBuilder()
                .addAllCams(cams.stream()
                        .map(this::camToGRPC)
                        .collect(Collectors.toList()))
                .setPrev(vecTimestampToGRPC(this.frontendTS))
                .setOpId(opId)
                .build();
    }

    private Silo.InitObservationsRequest createInitObservationsRequest(List<FrontendReport> reports, String opId) {
        return Silo.InitObservationsRequest.newBuilder()
                .addAllObservations(reports.stream()
                        .map(this::reportToGRPC)
                        .collect(Collectors.toList()))
                .setPrev(vecTimestampToGRPC(this.frontendTS))
                .setOpId(opId)
                .build();
    }

    private Silo.ClearRequest createClearRequest() {
        return Silo.ClearRequest.getDefaultInstance();
    }


    private Silo.JoinRequest createJoinRequest(FrontendCam cam, String opId) {
        return Silo.JoinRequest.newBuilder()
                .setCam(camToGRPC(cam))
                .setPrev(vecTimestampToGRPC(this.frontendTS))
                .setOpId(opId)
                .build();
    }

    private Silo.InfoRequest createInfoRequest(String name) {
        return Silo.InfoRequest.newBuilder().setName(name).build();
    }

    private Silo.ReportRequest createReportRequest(String camName, List<FrontendObservation> observations, String opId) {
        return Silo.ReportRequest.newBuilder()
                .setCamName(camName)
                .addAllObservations(observations.stream().map(this::observationToGRPC).collect(Collectors.toList()))
                .setPrev(vecTimestampToGRPC(this.frontendTS))
                .setOpId(opId)
                .build();
    }


    private Silo.TrackRequest createTrackRequest(FrontendObservation.ObservationType type, String id) {
        return Silo.TrackRequest.newBuilder()
                .setPrev(vecTimestampToGRPC(this.frontendTS))
                .setType(observationTypeToGRPC(type))
                .setId(id).build();
    }

    private Silo.TrackMatchRequest createTrackMatchRequest(FrontendObservation.ObservationType type, String pattern) {
        return Silo.TrackMatchRequest.newBuilder()
                .setPrev(vecTimestampToGRPC(this.frontendTS))
                .setType(observationTypeToGRPC(type))
                .setPattern(pattern).build();
    }

    private Silo.TraceRequest createTraceRequest(FrontendObservation.ObservationType type, String id) {
        return Silo.TraceRequest.newBuilder()
                .setPrev(vecTimestampToGRPC(this.frontendTS))
                .setType(observationTypeToGRPC(type))
                .setId(id).build();
    }


    // ===================================================
    // CONVERT BETWEEN DTO AND GRPC
    // ===================================================


    private Silo.VecTimestamp vecTimestampToGRPC(VectorTimestamp ts) {
        return Silo.VecTimestamp.newBuilder().addAllTimestamps(ts.getValues()).build();
    }

    private VectorTimestamp vectorTimestampFromGRPC(pt.tecnico.sauron.silo.grpc.Silo.VecTimestamp timestamp) {
        return new VectorTimestamp(timestamp.getTimestampsList());
    }

    private Silo.InitObservationsItem reportToGRPC(FrontendReport report) {
        return Silo.InitObservationsItem.newBuilder()
                .setCam(camToGRPC(report.getCam()))
                .setObservation(observationToGRPC(report.getObservation()))
                .setTimestamp(timestampToGRPC(report.getTimestamp()))
                .build();
    }

    private FrontendReport reportFromGRPC(Silo.Report report) {
        FrontendObservation frontendObservation = observationFromGRPC(report.getObservation());
        FrontendCam frontendCam = camFromGRPC(report.getCam());
        Instant timestamp = timestampFromGRPC(report.getTimestamp());
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