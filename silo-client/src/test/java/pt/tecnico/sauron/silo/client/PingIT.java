package pt.tecnico.sauron.silo.client;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.*;
import pt.tecnico.sauron.silo.grpc.Silo;


import static io.grpc.Status.Code.INVALID_ARGUMENT;

public class PingIT extends BaseIT{

    @Test
    public void pingOKTest() {
        Silo.PingRequest request = Silo.PingRequest.newBuilder().setText("friend").build();
        Silo.PingResponse response = siloFrontend.ctrlPing(request);
        Assertions.assertEquals("Hello friend!", response.getText());
    }

    @Test
    public void emptyPingTest() {
        Silo.PingRequest request = Silo.PingRequest.newBuilder().setText("").build();
        Assertions.assertEquals(INVALID_ARGUMENT, Assertions.assertThrows(
                StatusRuntimeException.class, ()->siloFrontend.ctrlPing(request))
                .getStatus().getCode());
    }

}
