package pt.tecnico.sauron.silo.client;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.client.dto.FrontendCam;
import pt.tecnico.sauron.silo.client.dto.FrontendCoords;
import pt.tecnico.sauron.silo.client.dto.FrontendObservation;
import pt.tecnico.sauron.silo.client.dto.FrontendReport;
import pt.tecnico.sauron.silo.client.exceptions.*;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.QueryServiceGrpc;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SiloFrontend {
    private ManagedChannel channel;
    private ControlServiceGrpc.ControlServiceBlockingStub ctrlBlockingStub;
    private ControlServiceGrpc.ControlServiceStub ctrlStub;
    private ReportServiceGrpc.ReportServiceStub reportStub;
    private QueryServiceGrpc.QueryServiceStub queryStub;
    private QueryServiceGrpc.QueryServiceBlockingStub queryBlockingStub;
    private ReportServiceGrpc.ReportServiceBlockingStub reportBlockingStub;
    public static final Metadata.Key<String> METADATA_CAM_NAME = Metadata.Key.of("name", Metadata.ASCII_STRING_MARSHALLER);

    public SiloFrontend(String zooHost, String zooPort, String serverPath) throws ZKNamingException {
        ZKNaming zkNaming = new ZKNaming(zooHost,zooPort);
        ZKRecord record = zkNaming.lookup(serverPath);
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



    // ===================================================
    // GRPC FRONTEND
    // ===================================================
    public String ctrlPing(String sentence) throws PingException {
        Silo.PingRequest request = createPingRequest(sentence);
        try {
            Silo.PingResponse response = this.ctrlBlockingStub.ping(request);
            return response.getText();
        } catch (StatusRuntimeException e) {
            throw new PingException(e.getStatus().getDescription());
        }
    }

    public void ctrlInitCams(List<FrontendCam> cams) throws RuntimeException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<Silo.InitResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(Silo.InitResponse response) {}

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };

        StreamObserver<Silo.InitCamRequest> requestObserver = this.ctrlStub.initCams(responseObserver);
        try {
            for(FrontendCam cam : cams) {
                Silo.InitCamRequest request = createInitCamRequest(cam);
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
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        } catch (InterruptedException e) {
            requestObserver.onError(e);
            Thread.currentThread().interrupt();
        }
    }

    public void ctrlInitObservations(List<FrontendReport> reports) throws RuntimeException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<Silo.InitResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(Silo.InitResponse response) {}

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
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        } catch (InterruptedException e) {
            requestObserver.onError(e);
            Thread.currentThread().interrupt();
        }
    }

    public void ctrlClear() throws ClearException{
        try {
            Silo.ClearResponse response =  this.ctrlBlockingStub.clear(createClearRequest());
        } catch (StatusRuntimeException e) {
            throw new ClearException(e.getStatus().getDescription());
        }
    }


    public void camJoin(FrontendCam cam) throws CameraAlreadyExistsException, CameraRegisterException {
        Silo.JoinRequest request = createJoinRequest(cam);

        try {
            this.reportBlockingStub.camJoin(request);
        } catch(RuntimeException e) {
            Status status = Status.fromThrowable(e);
            if(status.getCode() == Status.Code.ALREADY_EXISTS) {
                throw new CameraAlreadyExistsException();
            }
            if(status.getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new CameraRegisterException(status.getDescription());
            }
            throw new CameraRegisterException();
        }
    }

    public FrontendCoords camInfo(String name) throws CameraNotFoundException, CameraInfoException {
        Silo.InfoRequest request = createInfoRequest(name);

        try {
            Silo.InfoResponse response = this.reportBlockingStub.camInfo(request);
            return new FrontendCoords(response.getCoords().getLatitude(), response.getCoords().getLongitude());
        } catch (RuntimeException e) {
            Status status = Status.fromThrowable(e);
            if(status == Status.NOT_FOUND) {
                throw new CameraNotFoundException();
            }
            throw new CameraInfoException();
        }
    }

    public void report(String name, List<FrontendObservation> observations)
            throws ReportException, CameraNotFoundException, InvalidArgumentException {
        Metadata header = new Metadata();

        header.put(METADATA_CAM_NAME, name);
        ReportServiceGrpc.ReportServiceStub reportStubWithHeaders = MetadataUtils.attachHeaders(this.reportStub, header);
        final CountDownLatch latch = new CountDownLatch(1);

        // Arrays must be used to allow changes from the inner class
        final boolean[] error = new boolean[1];
        final Status[] errorStatus = new Status[1];

        StreamObserver<Silo.ReportResponse> responseObserver =  new StreamObserver<>() {
            @Override
            public void onNext(Silo.ReportResponse reportResponse) {}

            @Override
            public void onError(Throwable throwable) {
                Status status = Status.fromThrowable(throwable);
                    error[0] = true;
                    errorStatus[0] = status;
                    latch.countDown();
            }
            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };

        StreamObserver<Silo.Observation> requestObserver = reportStubWithHeaders.report(responseObserver);
        try {
            for (FrontendObservation observation : observations) {
                Silo.Observation request = createReportRequest(observation);
                requestObserver.onNext(request);

                // As per the documentation for client side streaming in gRPC,
                // we should sleep for an amount of time between each call
                // to allow for the server to send an error if it happens
                Thread.sleep(10);

                if (latch.getCount() == 0) {
                    break;
                }

            }

            requestObserver.onCompleted();
            latch.await(10, TimeUnit.SECONDS);

            if (error[0]) {
                switch(errorStatus[0].getCode()) {
                    case NOT_FOUND:
                        throw new CameraNotFoundException();
                    case INVALID_ARGUMENT:
                        throw new InvalidArgumentException(errorStatus[0].getDescription());
                    default:
                        throw new ReportException(errorStatus[0].getDescription());
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReportException(ErrorMessages.WAITING_THREAD_INTERRUPT);
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw new ReportException(e.toString());
        }

    }


    public FrontendReport track(FrontendObservation.ObservationType type, String id) throws QueryException, NotFoundException, InvalidArgumentException {
        Silo.QueryRequest request = createQueryRequest(type, id);

        try {
            return reportFromGRPC(queryBlockingStub.track(request));
        } catch(StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            if (status.getCode() == Status.Code.NOT_FOUND) {
                throw new NotFoundException();
            }
            if(status.getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new InvalidArgumentException(status.getDescription());
            }

            throw new QueryException();
        }
    }

    public List<FrontendReport> trackMatch(FrontendObservation.ObservationType type, String query) throws QueryException, NotFoundException, InvalidArgumentException {
        LinkedList<FrontendReport> results = new LinkedList<>();


        Silo.QueryRequest request = createQueryRequest(type, query);

        try {
            Iterator<Silo.QueryResponse> it = queryBlockingStub.trackMatch(request);
            while (it.hasNext()) {
                results.push(reportFromGRPC(it.next()));
            }
            return results;
        } catch(StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            if (status.getCode() == Status.Code.NOT_FOUND) {
                throw new NotFoundException();
            }
            if (status.getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new InvalidArgumentException(status.getDescription());
            }

            throw new QueryException();
        }
    }

    public List<FrontendReport> trace(FrontendObservation.ObservationType type, String id) throws QueryException, InvalidArgumentException, NotFoundException {
        LinkedList<FrontendReport> results = new LinkedList<>();


        Silo.QueryRequest request = createQueryRequest(type, id);

        try {
            Iterator<Silo.QueryResponse> it = queryBlockingStub.trace(request);
            while (it.hasNext()) {
                results.addLast(reportFromGRPC(it.next()));
            }
            return results;
        } catch(StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
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

    private Silo.InitCamRequest createInitCamRequest(FrontendCam cam) {
        return Silo.InitCamRequest.newBuilder()
                .setCam(camToGRPC(cam))
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

    private Silo.Observation createReportRequest(FrontendObservation observation) {
        return observationToGRPC(observation);
    }


    private Silo.QueryRequest createQueryRequest(FrontendObservation.ObservationType type, String id) {
        return Silo.QueryRequest.newBuilder()
                .setType(observationTypeToGRPC(type))
                .setId(id).build();
    }


    // ===================================================
    // CONVERT BETWEEN DTO AND GRPC
    // ===================================================
    private FrontendReport reportFromGRPC(Silo.QueryResponse response) {
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