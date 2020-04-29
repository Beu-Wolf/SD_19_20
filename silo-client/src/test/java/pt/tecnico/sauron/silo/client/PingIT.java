package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.silo.client.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;
import pt.tecnico.sauron.silo.client.exceptions.PingException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class PingIT extends BaseIT{

    @Test
    public void pingOKTest() {
        String sentence = "friend";
        try {
            String response = siloFrontend.ctrlPing(sentence);
            Assertions.assertEquals("Hello friend!", response);
        } catch (FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void emptyPingTest() {
        Assertions.assertEquals(ErrorMessages.BLANK_PING_INPUT, Assertions.assertThrows(
                PingException.class, ()->siloFrontend.ctrlPing(""))
                .getMessage());
    }

}
