package pt.tecnico.sauron.silo;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.*;

import pt.tecnico.sauron.silo.domain.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.grpc.ControlServiceGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;

public class SiloControlServiceImpl extends ControlServiceGrpc.ControlServiceImplBase {

    @Override
    public void ping(Silo.PingRequest request, StreamObserver<Silo.PingResponse> responseObserver) {
        String input = request.getText();

        if (input == null || input.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(ErrorMessages.BLANK_INPUT.getLabel()).asRuntimeException());
            return;
        }

        String output = "Hello " + input + "!";
        Silo.PingResponse response = Silo.PingResponse.newBuilder()
                .setText(output).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
