package pt.tecnico.sauron.silo;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.domain.Cam;
import pt.tecnico.sauron.silo.domain.Coords;
import pt.tecnico.sauron.silo.domain.Observation;
import pt.tecnico.sauron.silo.domain.Report;
import pt.tecnico.sauron.silo.domain.exceptions.*;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

import java.time.LocalDateTime;

import static io.grpc.Status.INVALID_ARGUMENT;

public class SiloControlServiceImpl extends ControlServiceGrpc.ControlServiceImplBase {

    private pt.tecnico.sauron.silo.domain.Silo silo;

    public SiloControlServiceImpl(pt.tecnico.sauron.silo.domain.Silo silo) {
        this.silo = silo;
    }

    @Override
    public void ping(Silo.PingRequest request, StreamObserver<Silo.PingResponse> responseObserver) {
        String input = request.getText();

        if (input == null || input.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(ErrorMessages.BLANK_INPUT).asRuntimeException());
            return;
        }

        String output = "Hello " + input + "!";
        Silo.PingResponse response = Silo.PingResponse.newBuilder()
                .setText(output).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public StreamObserver<Silo.InitCamRequest> initCams(StreamObserver<Silo.InitResponse> responseObserver) {
        try {
            return new StreamObserver<>() {
                @Override
                public void onNext(Silo.InitCamRequest request) {
                    String name = request.getCam().getName();
                    Coords coords = new Coords(request.getCam().getCoords().getLatitude(), request.getCam().getCoords().getLongitude());
                    Cam cam = new Cam(name, coords);
                    silo.registerCam(cam);
                }

                @Override
                public void onError(Throwable throwable) {
                    System.err.println("Error while reporting: " + Status.fromThrowable(throwable).getDescription());
                }

                @Override
                public void onCompleted() {
                    responseObserver.onNext(pt.tecnico.sauron.silo.grpc.Silo.ReportResponse.getDefaultInstance());
                    responseObserver.onCompleted();
                }
            };
        } catch (NoCameraFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(ErrorMessages.NO_CAM_FOUND).asRuntimeException());
            return null;
        }
    }

}
