package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.exceptions.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class CamJoinIT extends BaseIT {
    public static String name = "testCamera";
    public static String shortName = "ct";
    public static String longName = "VeryBigCameraNameAbove15";
    public static double lat = 12.983456;
    public static double lon = 8.678456;
    public static double newLat = 11.123456;
    public static double newLon = 9.564738;
    public static double badPositiveLat = 200.122001;
    public static double badNegativeLat = -181.192034;
    public static double badPositiveLon = 91.112345;
    public static double badNegativeLon = -90.112344;

    @Test
    public void joinCameraOKTest() {
        CamDto cam = new CamDto(name, lat, lon);
        try {
            siloFrontend.camJoin(cam);
            Assertions.assertEquals(cam.toString(), siloFrontend.camInfo(cam.getName()).toString());
        } catch (FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void joinCameraNameDuplicateTest() {
        try {
            CamDto cam = new CamDto(name, lat, lon);
            CamDto duplicate = new CamDto(name, newLat , newLon);
            siloFrontend.camJoin(cam);
            Assertions.assertEquals(ErrorMessages.CAMERA_ALREADY_EXISTS, Assertions.assertThrows(
                    CameraAlreadyExistsException.class, ()->siloFrontend.camJoin(duplicate))
                    .getMessage() );
        } catch (FrontendException |ZKNamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void joinSameCameraTwiceTest() {
        try {
            CamDto cam = new CamDto(name, lat, lon);
            siloFrontend.camJoin(cam);
            Assertions.assertDoesNotThrow(()->siloFrontend.camJoin(cam));
            Assertions.assertEquals(cam, siloFrontend.camInfo(cam.getName()));
        } catch (FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void camJoinWithBlankName() {
        CamDto cam = new CamDto("", lat, lon);
        Assertions.assertEquals("Camera name is empty!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(cam))
                .getMessage() );
    }

    @Test
    public void camJoinWithShortName() {
        CamDto shortCam = new CamDto(shortName, lat, lon);
        Assertions.assertEquals("Camera names must be between 3 and 15 characters long!", Assertions.assertThrows(
        CameraRegisterException.class,()->siloFrontend.camJoin(shortCam))
         .getMessage());
    }

    @Test
    public void camJoinWithLongName() {
        CamDto longCam = new CamDto(longName, lat, lon);
        Assertions.assertEquals("Camera names must be between 3 and 15 characters long!", Assertions.assertThrows(
        CameraRegisterException.class,()->siloFrontend.camJoin(longCam))
        .getMessage());
    }

    @Test
    public void camJoinInvalidLatitude() {
        CamDto badNegLatCam = new CamDto(name, badNegativeLat, lon);
        Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(badNegLatCam))
                .getMessage());
        CamDto badPosLatCam = new CamDto(name, badPositiveLat, lon);
        Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(badPosLatCam))
                .getMessage());
    }

    @Test
    public void camJoinInvalidLongitude() {
        CamDto badNegLonCam = new CamDto(name, lat, badNegativeLon);
        Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(badNegLonCam))
                .getMessage());
        CamDto badPosLonCam = new CamDto(name, lat, badPositiveLon);
        Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(badPosLonCam))
                .getMessage());
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
