package pt.tecnico.sauron.silo;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.*;

import pt.tecnico.sauron.silo.domain.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

public class SiloControlServiceImpl extends ControlServiceGrpc.ControlServiceImplBase {

    private pt.tecnico.sauron.silo.domain.Silo silo;

    SiloControlServiceImpl(pt.tecnico.sauron.silo.domain.Silo silo) {
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

    @Override
    public void clear( Silo.ClearRequest request, StreamObserver<Silo.ClearResponse> responseObserver) {
        silo.clearObservations();
        silo.clearCams();

        responseObserver.onNext(Silo.ClearResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

}
