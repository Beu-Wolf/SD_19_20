package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.exceptions.CameraAlreadyExistsException;
import pt.tecnico.sauron.silo.client.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;

public class CamJoinIT extends BaseIT {
    public static String name = "testCamera";
    public static double lat = 12.983456;
    public static double lon = 8.678456;

    @BeforeEach
    public void populate() {
        siloFrontend.ctrlInit();
    }

    @Test
    public void joinCameraOKTest() {
        CamDto cam = new CamDto(name, lat, lon);
        try {
            siloFrontend.camJoin(cam);
            Assertions.assertEquals(siloFrontend.camInfo(name), cam);
        } catch (FrontendException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void joinCameraDuplicateTest() {
        try {
            CamDto cam = new CamDto(name, lat, lon);
            siloFrontend.camJoin(cam);
            Assertions.assertEquals(ErrorMessages.CAMERA_ALREADY_EXISTS, Assertions.assertThrows(
                    CameraAlreadyExistsException.class, ()->siloFrontend.camJoin(cam))
                    .getMessage() );
        } catch (FrontendException e) {
            e.printStackTrace();
        }

    }

    /*@Test
    public void joinCameraWithBlankName() {
        CamDto cam = new CamDto("", lat, lon);
        try {
            siloFrontend.camJoin(cam);
            Assertions.assertEquals(ErrorMessages.CAMERA_ALREADY_EXISTS, Assertions.assertThrows(
                    InvalidCameraName.class, ()->siloFrontend.camJoin(cam))
                    .getMessage() );
        } catch (FrontendException e) {
            e.printStackTrace();
        }
    }
*/

    @AfterEach
    public void clear() {
        siloFrontend.ctrlClear();
    }

}
