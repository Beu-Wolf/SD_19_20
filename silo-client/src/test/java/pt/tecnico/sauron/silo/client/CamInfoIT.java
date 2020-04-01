package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.exceptions.CameraNotFoundException;
import pt.tecnico.sauron.silo.client.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;

public class CamInfoIT extends BaseIT {

    public static String name = "testCamera";
    public static double lat = 12.985744;
    public static double lon = 8.987345;

    @BeforeEach
    public void init() {
        siloFrontend.ctrlInit();
    }

    @Test
    public void camInfoOKTest() {
        try {
            CamDto cam = new CamDto(name, lat, lon);
            siloFrontend.camJoin(cam);
            CamDto received = siloFrontend.camInfo(name);
            Assertions.assertEquals(cam, received);
            //remove from Silo
        } catch (FrontendException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void camInfoNotExistsTest() {
        Assertions.assertEquals(ErrorMessages.CAMERA_NOT_FOUND, Assertions.assertThrows(
                CameraNotFoundException.class, ()->siloFrontend.camInfo(name))
                        .getMessage());

    }

    @AfterEach
    public void clear() {
        siloFrontend.ctrlClear();
    }
}
