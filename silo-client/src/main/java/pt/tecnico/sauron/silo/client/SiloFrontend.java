package pt.tecnico.sauron.silo.client;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.CoordsDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.dto.ReportDto;
import pt.tecnico.sauron.silo.client.exceptions.*;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.QueryServiceGrpc;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

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

    public SiloFrontend(String host, int port) {
        String target = host + ":" + port;
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

    public void ctrlInitCams(List<CamDto> cams) throws RuntimeException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<Silo.InitResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(Silo.InitResponse response) {}

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Could not register cameras");
                latch.countDown();
            }
            @Override
            public void onCompleted() {
                System.out.println("Successfully registered cameras!");
                latch.countDown();
            }
        };

        StreamObserver<Silo.InitCamRequest> requestObserver = this.ctrlStub.initCams(responseObserver);
        try {
            for(CamDto cam : cams) {
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

    public void ctrlInitObservations(List<ReportDto> reports) throws RuntimeException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<Silo.InitResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(Silo.InitResponse response) {}

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Could not register observation");
                latch.countDown();
            }
            @Override
            public void onCompleted() {
                System.out.println("Successfully registered observations!");
                latch.countDown();
            }
        };

        StreamObserver<Silo.InitObservationRequest> requestObserver = this.ctrlStub.initObservations(responseObserver);
        try {
            for(ReportDto report : reports) {
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


    public void camJoin(CamDto cam) throws CameraAlreadyExistsException, CameraRegisterException {
        Silo.JoinRequest request = createJoinRequest(cam);

        try {
            this.reportBlockingStub.camJoin(request);
            System.out.println("Registered Successfully!");
        } catch(RuntimeException e) {
            Status status = Status.fromThrowable(e);
            if(status == Status.ALREADY_EXISTS) {
                throw new CameraAlreadyExistsException();
            }
            throw new CameraRegisterException();
        }
    }

    public CamDto camInfo(String name) throws CameraNotFoundException, CameraInfoException {
        Silo.InfoRequest request = createInfoRequest(name);

        try {
            Silo.InfoResponse response = this.reportBlockingStub.camInfo(request);
            return camFromGRPC(response.getCam());
        } catch (RuntimeException e) {
            Status status = Status.fromThrowable(e);
            if(status == Status.NOT_FOUND) {
                throw new CameraNotFoundException();
            }
            throw new CameraInfoException();
        }
    }

    public void report(String name, List<ObservationDto> observations) throws ReportException {
        Metadata header = new Metadata();

        header.put(METADATA_CAM_NAME, name);
        ReportServiceGrpc.ReportServiceStub reportStubWithHeaders = MetadataUtils.attachHeaders(this.reportStub, header);
        final CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<Silo.ReportResponse> responseObserver =  new StreamObserver<>() {
            @Override
            public void onNext(Silo.ReportResponse reportResponse) {}

            @Override
            public void onError(Throwable throwable) {
                Status status = Status.fromThrowable(throwable);
                if(status.getCode() == Status.Code.NOT_FOUND || status.getCode() == Status.Code.INVALID_ARGUMENT) {
                    System.err.println(status.getDescription());
                    latch.countDown();
                }
            }
            @Override
            public void onCompleted() {
                System.out.println("Successfully reported observations!");
                latch.countDown();
            }
        };

        StreamObserver<Silo.Observation> requestObserver = reportStubWithHeaders.report(responseObserver);
        try {
            for (ObservationDto observation : observations) {
                Silo.Observation request = createReportRequest(observation);
                requestObserver.onNext(request);

                // As per the documentation for client side streaming in gRPC,
                // we should sleep for an amount of time between each call
                // to allow for the server to send an error if it happens
                Thread.sleep(10);

                if(latch.getCount() == 0) {
                    return;
                }
            }

            requestObserver.onCompleted();
            latch.await(10, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReportException(ErrorMessages.WAITING_THREAD_INTERRUPT);
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw new ReportException(e.toString());
        }
    }


    public ReportDto track(ObservationDto.ObservationType type, String id) throws QueryException {
        Silo.QueryRequest request = createQueryRequest(type, id);

        try {
            return reportFromGRPC(queryBlockingStub.track(request));
        } catch(StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            if (status.getCode() == Status.Code.NOT_FOUND) {
                throw new QueryException(ErrorMessages.OBSERVATION_NOT_FOUND);
            }
            if(status.getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new QueryException(status.getDescription());
            }

            throw new QueryException();
        }
    }

    public List<ReportDto> trackMatch(ObservationDto.ObservationType type, String query) throws QueryException {
        LinkedList<ReportDto> results = new LinkedList<>();


        Silo.QueryRequest request = createQueryRequest(type, query);

        try {
            Iterator<Silo.QueryResponse> it = queryBlockingStub.trackMatch(request);
            while (it.hasNext()) {
                results.push(reportFromGRPC(it.next()));
            }
            return results;
        } catch(StatusRuntimeException e) {
            System.out.println("GOT ERROR: " + e);
            Status status = Status.fromThrowable(e);
            if (status.getCode() == Status.Code.NOT_FOUND) {
                throw new QueryException(ErrorMessages.OBSERVATION_NOT_FOUND);
            }
            if (status.getCode() == Status.Code.UNIMPLEMENTED) {
                throw new QueryException(ErrorMessages.TYPE_NOT_SUPPORTED);
            }

            throw new QueryException();
        }
    }

    public List<ReportDto> trace(ObservationDto.ObservationType type, String id) throws QueryException {
        LinkedList<ReportDto> results = new LinkedList<>();


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
                throw new QueryException(ErrorMessages.OBSERVATION_NOT_FOUND);
            }
            if (status.getCode() == Status.Code.UNIMPLEMENTED) {
                throw new QueryException(ErrorMessages.TYPE_NOT_SUPPORTED);
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

    private Silo.InitCamRequest createInitCamRequest(CamDto cam) {
        return Silo.InitCamRequest.newBuilder()
                .setCam(camToGRPC(cam))
                .build();
    }

    private Silo.InitObservationRequest createInitObservationRequest(ReportDto report) {
        return Silo.InitObservationRequest.newBuilder()
                .setCam(camToGRPC(report.getCam()))
                .setObservation(observationToGRPC(report.getObservation()))
                .setTimestamp(timestampToGRPC(report.getTimestamp()))
                .build();
    }

    private Silo.ClearRequest createClearRequest() {
        return Silo.ClearRequest.getDefaultInstance();
    }


    private Silo.JoinRequest createJoinRequest(CamDto cam) {
        return Silo.JoinRequest.newBuilder()
                .setCam(camToGRPC(cam))
                .build();
    }

    private Silo.InfoRequest createInfoRequest(String name) {
        return Silo.InfoRequest.newBuilder().setName(name).build();
    }

    private Silo.Observation createReportRequest(ObservationDto observation) {
        return observationToGRPC(observation);
    }


    private Silo.QueryRequest createQueryRequest(ObservationDto.ObservationType type, String id) {
        return Silo.QueryRequest.newBuilder()
                .setType(observationTypeToGRPC(type))
                .setId(id).build();
    }


    // ===================================================
    // CONVERT BETWEEN DTO AND GRPC
    // ===================================================
    private ReportDto reportFromGRPC(Silo.QueryResponse response) {
        ObservationDto observationDto = observationFromGRPC(response.getObservation());
        CamDto camDto = camFromGRPC(response.getCam());
        Instant timestamp = timestampFromGRPC(response.getTimestamp());

        return new ReportDto(observationDto, camDto, timestamp);
    }

    private Silo.Observation observationToGRPC(ObservationDto observation) {
        return Silo.Observation.newBuilder()
                .setObservationId(observation.getId())
                .setType(observationTypeToGRPC(observation.getType()))
                .build();
    }
    private ObservationDto observationFromGRPC(Silo.Observation observation) {
        return new ObservationDto(observationTypeFromGRPC(observation.getType()),
                observation.getObservationId());
    }

    private Silo.ObservationType observationTypeToGRPC(ObservationDto.ObservationType type) {
        switch(type) {
            case CAR:
                return Silo.ObservationType.CAR;
            case PERSON:
                return Silo.ObservationType.PERSON;
            default:
                return Silo.ObservationType.UNSPEC;
        }
    }
    private ObservationDto.ObservationType observationTypeFromGRPC(Silo.ObservationType type) {
        switch(type) {
            case CAR:
                return ObservationDto.ObservationType.CAR;
            case PERSON:
                return ObservationDto.ObservationType.PERSON;
            default:
                return ObservationDto.ObservationType.UNSPEC;
        }
    }

    private Silo.Cam camToGRPC(CamDto cam) {
        return Silo.Cam.newBuilder()
                .setName(cam.getName())
                .setCoords(coordsToGRPC(cam.getCoords()))
                .build();
    }
    private CamDto camFromGRPC(Silo.Cam cam) {
        String name = cam.getName();
        LatLng coords = cam.getCoords();

        return new CamDto(name, coords.getLatitude(), coords.getLongitude());
    }

    private LatLng coordsToGRPC(CoordsDto coords) {
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