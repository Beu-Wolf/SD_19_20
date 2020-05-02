package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.silo.client.domain.FrontendCam;
import pt.tecnico.sauron.silo.client.domain.FrontendCoords;
import pt.tecnico.sauron.silo.client.exceptions.CameraNotFoundException;
import pt.tecnico.sauron.silo.client.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class CamInfoIT extends BaseIT {

    public static String name = "testCamera";
    public static double lat = 12.985744;
    public static double lon = 8.987345;

    @Test
    public void camInfoOKTest() {
        try {
            FrontendCam cam = new FrontendCam(name, lat, lon);

            siloFrontend.camJoin(cam);
            FrontendCoords received = siloFrontend.camInfo(name);

            Assertions.assertEquals(cam.getLat(), received.getLat());
            Assertions.assertEquals(cam.getLon(), received.getLon());
        } catch (FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void camInfoNotExistsTest() {
        Assertions.assertEquals(
            ErrorMessages.CAMERA_NOT_FOUND,
            Assertions.assertThrows(
                CameraNotFoundException.class,
                () -> {
                    siloFrontend.camInfo("name");
                }).getMessage());
    }

    @AfterEach
    public void clear() {
        try {
            siloFrontend.ctrlClear();
        } catch (FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }
}
