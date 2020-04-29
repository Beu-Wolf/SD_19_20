package pt.tecnico.sauron.silo.client;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.*;
import pt.sauron.silo.contract.domain.Cam;
import pt.sauron.silo.contract.domain.Coords;
import pt.sauron.silo.contract.domain.exceptions.EmptyCameraNameException;
import pt.sauron.silo.contract.domain.exceptions.InvalidCameraNameException;
import pt.tecnico.sauron.silo.client.domain.Observation;
import pt.tecnico.sauron.silo.client.domain.Report;
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


    public void ctrlInitCams(List<Cam> cams) throws RuntimeException, FrontendException {
        Silo.InitCamsRequest request = createInitCamsRequest(cams);

        try {
            this.ctrlBlockingStub.initCams(request);
        } catch(RuntimeException e) {
            throw new FrontendException(e.getMessage());
        }
    }

    public void ctrlInitObservations(List<Report> reports) throws FrontendException {
        Silo.InitObservationsRequest request = createInitObservationsRequest(reports);

        try {
            this.ctrlBlockingStub.initObservations(request);
        } catch (RuntimeException e) {
            throw new FrontendException(e.getMessage());
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


    public void camJoin(Cam cam) throws ZKNamingException, FrontendException {
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

    public Coords camInfo(String name) throws FrontendException, ZKNamingException {
        Silo.InfoRequest request = createInfoRequest(name);

        try {
            Silo.InfoResponse response = this.reportBlockingStub.camInfo(request);
            return new Coords(response.getCoords().getLatitude(), response.getCoords().getLongitude());
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

    public int report(String name, List<Observation> observations)
            throws CameraNotFoundException, ReportException, InvalidArgumentException {
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
                case INVALID_ARGUMENT:
                    throw new InvalidArgumentException(status.getDescription());
                default:
                    throw new ReportException(status.getDescription());
            }
        }

        return response.getNumAcked();
    }


    public Report track(Observation.ObservationType type, String id) throws FrontendException, ZKNamingException {
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

        } catch (InvalidCameraNameException
                |EmptyCameraNameException e) {
            // WILL NOT HAPPEN: cams from GRPC always have good names
            e.printStackTrace();
            throw new QueryException();
        }
    }

    public List<Report> trackMatch(Observation.ObservationType type, String query) throws FrontendException, ZKNamingException {
        LinkedList<Report> results = new LinkedList<>();
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

        } catch (InvalidCameraNameException
                |EmptyCameraNameException e) {
            // WILL NOT HAPPEN: cams from GRPC always have good names
            e.printStackTrace();
            throw new QueryException();
        }
}

    public List<Report> trace(Observation.ObservationType type, String id) throws FrontendException, ZKNamingException {
        LinkedList<Report> results = new LinkedList<>();
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

        } catch (InvalidCameraNameException
                |EmptyCameraNameException e) {
            // WILL NOT HAPPEN: cams from GRPC always have good names
            e.printStackTrace();
            throw new QueryException();
        }
    }




    // ===================================================
    // CREATE GRPC REQUESTS
    // ===================================================
    private Silo.PingRequest createPingRequest(String sentence) {
        return Silo.PingRequest.newBuilder().setText(sentence).build();
    }

    private Silo.InitCamsRequest createInitCamsRequest(List<Cam> cams) {
        return Silo.InitCamsRequest.newBuilder()
                .addAllCams(cams.stream()
                        .map(this::camToGRPC)
                        .collect(Collectors.toList()))
                .build();
    }

    private Silo.InitObservationsRequest createInitObservationsRequest(List<Report> reports) {
        return Silo.InitObservationsRequest.newBuilder()
                .addAllObservations(reports.stream()
                        .map(this::reportToGRPC)
                        .collect(Collectors.toList()))
                .build();
    }

    private Silo.ClearRequest createClearRequest() {
        return Silo.ClearRequest.getDefaultInstance();
    }


    private Silo.JoinRequest createJoinRequest(Cam cam) {
        return Silo.JoinRequest.newBuilder()
                .setCam(camToGRPC(cam))
                .build();
    }

    private Silo.InfoRequest createInfoRequest(String name) {
        return Silo.InfoRequest.newBuilder().setName(name).build();
    }

    private Silo.ReportRequest createReportRequest(String camName, List<Observation> observations) {
        return Silo.ReportRequest.newBuilder()
                .setCamName(camName)
                .addAllObservations(observations.stream().map(this::observationToGRPC).collect(Collectors.toList()))
                .build();
    }


    private Silo.TrackRequest createTrackRequest(Observation.ObservationType type, String id) {
        return Silo.TrackRequest.newBuilder()
                .setType(observationTypeToGRPC(type))
                .setId(id).build();
    }

    private Silo.TrackMatchRequest createTrackMatchRequest(Observation.ObservationType type, String pattern) {
        return Silo.TrackMatchRequest.newBuilder()
                .setType(observationTypeToGRPC(type))
                .setPattern(pattern).build();
    }

    private Silo.TraceRequest createTraceRequest(Observation.ObservationType type, String id) {
        return Silo.TraceRequest.newBuilder()
                .setType(observationTypeToGRPC(type))
                .setId(id).build();
    }


    // ===================================================
    // CONVERT BETWEEN DTO AND GRPC
    // ===================================================
    private Report reportFromTrackResponse(Silo.TrackResponse response) throws InvalidCameraNameException, EmptyCameraNameException {
        Observation observation = observationFromGRPC(response.getObservation());
        Cam cam = camFromGRPC(response.getCam());
        Instant timestamp = timestampFromGRPC(response.getTimestamp());

        return new Report(observation, cam, timestamp);
    }

    private Silo.InitObservationsItem reportToGRPC(Report report) {
        return Silo.InitObservationsItem.newBuilder()
                .setCam(camToGRPC(report.getCam()))
                .setObservation(observationToGRPC(report.getObservation()))
                .setTimestamp(timestampToGRPC(report.getTimestamp()))
                .build();
    }

    private Report reportFromTrackMatchResponse(Silo.TrackMatchResponse response) throws InvalidCameraNameException, EmptyCameraNameException {
        Observation observation = observationFromGRPC(response.getObservation());
        Cam cam = camFromGRPC(response.getCam());
        Instant timestamp = timestampFromGRPC(response.getTimestamp());

        return new Report(observation, cam, timestamp);
    }

    private Report reportFromTraceResponse(Silo.TraceResponse response) throws InvalidCameraNameException, EmptyCameraNameException {
        Observation observation = observationFromGRPC(response.getObservation());
        Cam cam = camFromGRPC(response.getCam());
        Instant timestamp = timestampFromGRPC(response.getTimestamp());

        return new Report(observation, cam, timestamp);
    }

    private Silo.Observation observationToGRPC(Observation observation) {
        return Silo.Observation.newBuilder()
                .setObservationId(observation.getId())
                .setType(observationTypeToGRPC(observation.getType()))
                .build();
    }
    private Observation observationFromGRPC(Silo.Observation observation) {
        return new Observation(observationTypeFromGRPC(observation.getType()),
                observation.getObservationId());
    }

    private Silo.ObservationType observationTypeToGRPC(Observation.ObservationType type) {
        switch(type) {
            case CAR:
                return Silo.ObservationType.CAR;
            case PERSON:
                return Silo.ObservationType.PERSON;
            default:
                return Silo.ObservationType.UNSPEC;
        }
    }
    private Observation.ObservationType observationTypeFromGRPC(Silo.ObservationType type) {
        switch(type) {
            case CAR:
                return Observation.ObservationType.CAR;
            case PERSON:
                return Observation.ObservationType.PERSON;
            default:
                return Observation.ObservationType.UNSPEC;
        }
    }

    private Silo.Cam camToGRPC(Cam cam) {
        return Silo.Cam.newBuilder()
                .setName(cam.getName())
                .setCoords(coordsToGRPC(cam.getCoords()))
                .build();
    }
    private Cam camFromGRPC(Silo.Cam cam) throws InvalidCameraNameException, EmptyCameraNameException {
        String name = cam.getName();
        Coords coords = coordsFromGRPC(cam.getCoords());

        return new Cam(name, coords);
    }

    private Coords coordsFromGRPC(LatLng coords) {
        return new Coords(coords.getLatitude(), coords.getLongitude());
    }

    private LatLng coordsToGRPC(Coords coords) {
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