package pt.tecnico.sauron.silo.client;

import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.dto.ReportDto;
import pt.tecnico.sauron.silo.client.exceptions.InvalidArgumentException;
import pt.tecnico.sauron.silo.client.exceptions.QueryException;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.QueryServiceGrpc;
import pt.tecnico.sauron.silo.grpc.ReportServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SiloFrontend {
    private ManagedChannel channel;
    private ControlServiceGrpc.ControlServiceBlockingStub ctrlStub;
    private ReportServiceGrpc.ReportServiceStub reportStub;
    private QueryServiceGrpc.QueryServiceStub queryStub;
    public static final Metadata.Key<String> METADATA_CAM_NAME = Metadata.Key.of("name", Metadata.ASCII_STRING_MARSHALLER);

    public SiloFrontend(String host, int port) {
        String target = host + ":" + port;
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.ctrlStub = ControlServiceGrpc.newBlockingStub(this.channel);
        this.reportStub = ReportServiceGrpc.newStub(this.channel);
        this.queryStub = QueryServiceGrpc.newStub(this.channel);
    }

    public void camJoin(CamDto cam) {}

    public CamDto camInfo(String name) { return null; }

    public void report(String name, List<ObservationDto> observations) throws InterruptedException {
        Metadata header = new Metadata();
        header.put(METADATA_CAM_NAME, name);
        this.reportStub = MetadataUtils.attachHeaders(this.reportStub, header);
        final CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<Silo.ReportResponse> responseObserver =  new StreamObserver<>() {
            @Override
            public void onNext(Silo.ReportResponse reportResponse) { }

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
        StreamObserver<Silo.Observation> requestObserver = this.reportStub.report(responseObserver);
        try {
            for (ObservationDto observationDto : observations) {
                Silo.ObservationType observationType = getObservationType(observationDto);
                Silo.Observation observation = Silo.Observation.newBuilder().setObservationId(observationDto.getId())
                        .setType(observationType).build();
                requestObserver.onNext(observation);
                if(latch.getCount() == 0) {
                    return;
                }
                Thread.sleep(10);                         //As per the documentation for client side streaming in gRPC, we should sleep for an amount of time between each call
                                                            // to allow for the server to send an error if it happens
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            System.out.println("Error while sending reports" + e.toString());
        }
        requestObserver.onCompleted();
        latch.await(10, TimeUnit.SECONDS);



    }

    public ReportDto track(ObservationDto.ObservationType type, String id)
        throws QueryException, InterruptedException {

        final CountDownLatch finishLatch = new CountDownLatch(1);

        // Having arrays of 1 element is the only way to set
        // these variables from the inner class
        final ReportDto[] reportDto = new ReportDto[1];
        final boolean[] error = new boolean[1];

        StreamObserver<Silo.QueryResponse> responseObserver = new StreamObserver<Silo.QueryResponse>() {
            @Override
            public void onNext(Silo.QueryResponse queryResponse) {
                reportDto[0] = GRPCToReportDto(queryResponse);
            }

            @Override
            public void onError(Throwable throwable) {
                // We can't throw exceptions from here to the outer method
                error[0] = true;
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        };

        Silo.QueryRequest request = Silo.QueryRequest.newBuilder()
                .setType(ObservationTypeToGRPC(type))
                .setId(id).build();

        queryStub.track(request, responseObserver);

        finishLatch.await(10, TimeUnit.SECONDS);

        if (error[0]) {
            throw new QueryException();
        } else {
            return reportDto[0];
        }
    }

    // public void trackMatch(ObservationDto.ObservationType type, String query, Lambda)

    // public void trace(ObservationDto.ObservationType type, String id, Lambda)

    public String ctrlPing(String sentence) {
        Silo.PingRequest request = Silo.PingRequest.newBuilder().setText(sentence).build();
        Silo.PingResponse response = this.ctrlStub.ping(request);
        return response.getText();
    }

    public void ctrlClear() {}

    public void ctrlInit() {}


    public Silo.ObservationType getObservationType(ObservationDto observationDto) {
        Silo.ObservationType observationType;
        ObservationDto.ObservationType type = observationDto.getType();


        switch (type) {
            case CAR:
                observationType = Silo.ObservationType.CAR;
                break;
            case PERSON:
                observationType = Silo.ObservationType.PERSON;
                break;
            default:
                observationType = Silo.ObservationType.UNSPEC;
        }
        return observationType;
    }

    public void shutdown() {
        this.channel.shutdown();
    }

    private ObservationDto GRPCToObservationDto(Silo.Observation observation) {
        return new ObservationDto(GRPCToObservationType(observation.getType()),
                observation.getObservationId());
    }

    private CamDto GRPCToCamDto(Silo.Cam cam) {
        return new CamDto(cam.getName(), cam.getCoords().getLatitude(), cam.getCoords().getLongitude());
    }

    private ReportDto GRPCToReportDto(Silo.QueryResponse response) {
        ObservationDto observationDto = GRPCToObservationDto(response.getObservation());
        CamDto camDto = GRPCToCamDto(response.getCam());
        Instant timestamp = Instant.ofEpochSecond(response.getTimestamp().getSeconds());

        return new ReportDto(observationDto, camDto, timestamp);
    }

    private ObservationDto.ObservationType GRPCToObservationType(Silo.ObservationType type) {
        switch(type) {
            case CAR:
                return ObservationDto.ObservationType.CAR;
            case PERSON:
                return ObservationDto.ObservationType.PERSON;
            default:
                return ObservationDto.ObservationType.UNSPEC;
        }
    }

    private Silo.ObservationType ObservationTypeToGRPC(ObservationDto.ObservationType type) {
        switch(type) {
            case CAR:
                return Silo.ObservationType.CAR;
            case PERSON:
                return Silo.ObservationType.PERSON;
            default:
                return Silo.ObservationType.UNSPEC;
        }
    }
}
