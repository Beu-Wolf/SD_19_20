package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.exceptions.*;

public class CamJoinIT extends BaseIT {
    public static String name = "testCamera";
    public static double lat = 12.983456;
    public static double lon = 8.678456;

    @Test
    public void joinCameraOKTest() {
        CamDto cam = new CamDto(name, lat, lon);
        try {
            siloFrontend.camJoin(cam);
            Assertions.assertEquals(cam.toString(), siloFrontend.camInfo(cam.getName()).toString());
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

    @Test
    public void camJoinWithBlankName() {
        CamDto cam = new CamDto("", lat, lon);
        Assertions.assertEquals(ErrorMessages.FAILED_TO_REGISTER_CAMERA, Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(cam))
                .getMessage() );
    }


    @AfterEach
    public void clear() {
        try {
            siloFrontend.ctrlClear();
        } catch (ClearException e) {
            e.printStackTrace();
        }
    }

}
