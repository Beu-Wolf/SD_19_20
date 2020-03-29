package pt.tecnico.sauron.silo.client;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.*;
import pt.tecnico.sauron.silo.grpc.Silo;


import static io.grpc.Status.Code.INVALID_ARGUMENT;

public class PingIT extends BaseIT{

    @Test
    public void pingOKTest() {
        String sentence = "friend";
        String response = siloFrontend.ctrlPing(sentence);
        Assertions.assertEquals("Hello friend!", response);
    }

    @Test
    public void emptyPingTest() {
        Assertions.assertEquals(INVALID_ARGUMENT, Assertions.assertThrows(
                StatusRuntimeException.class, ()->siloFrontend.ctrlPing(""))
                .getStatus().getCode());
    }

}
