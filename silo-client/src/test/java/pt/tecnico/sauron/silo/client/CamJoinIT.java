package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.silo.client.domain.Cam;
import pt.tecnico.sauron.silo.client.domain.Coords;
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
        Cam cam = new Cam(name, lat, lon);
        try {
            siloFrontend.camJoin(cam);
            Coords coords = siloFrontend.camInfo(cam.getName());
            Cam serverCam = new Cam(name, coords.getLat(), coords.getLon());
            Assertions.assertEquals(cam.toString(), serverCam.toString());
        } catch (FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void joinCameraNameDuplicateTest() {
        try {
            Cam cam = new Cam(name, lat, lon);
            Cam duplicate = new Cam(name, newLat , newLon);
            siloFrontend.camJoin(cam);
            Assertions.assertEquals(
                ErrorMessages.CAMERA_ALREADY_EXISTS,
                Assertions.assertThrows(
                    CameraAlreadyExistsException.class, () -> {
                        siloFrontend.camJoin(duplicate);
                    }
                ).getMessage()
            );

        } catch (FrontendException |ZKNamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void joinSameCameraTwiceTest() {
        try {
            Cam cam = new Cam(name, lat, lon);
            siloFrontend.camJoin(cam);
            Assertions.assertDoesNotThrow(()->siloFrontend.camJoin(cam));
            Coords coords =  siloFrontend.camInfo(cam.getName());
            Cam serverCam = new Cam(name, coords.getLat(), coords.getLon());
            Assertions.assertEquals(cam,serverCam);

        } catch (FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void camJoinWithBlankName() {
        Cam cam = new Cam("", lat, lon);
        Assertions.assertEquals("Camera name is empty!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(cam))
                .getMessage() );
    }

    @Test
    public void camJoinWithShortName() {
        Cam shortCam = new Cam(shortName, lat, lon);
        Assertions.assertEquals("Camera names must be between 3 and 15 characters long!", Assertions.assertThrows(
        CameraRegisterException.class,()->siloFrontend.camJoin(shortCam))
         .getMessage());
    }

    @Test
    public void camJoinWithLongName() {
        Cam longCam = new Cam(longName, lat, lon);
        Assertions.assertEquals("Camera names must be between 3 and 15 characters long!", Assertions.assertThrows(
        CameraRegisterException.class,()->siloFrontend.camJoin(longCam))
        .getMessage());
    }

    @Test
    public void camJoinInvalidLatitude() {
        Cam badNegLatCam = new Cam(name, badNegativeLat, lon);
        Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(badNegLatCam))
                .getMessage());
        Cam badPosLatCam = new Cam(name, badPositiveLat, lon);
        Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(badPosLatCam))
                .getMessage());
    }

    @Test
    public void camJoinInvalidLongitude() {
        Cam badNegLonCam = new Cam(name, lat, badNegativeLon);
        Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(badNegLonCam))
                .getMessage());
        Cam badPosLonCam = new Cam(name, lat, badPositiveLon);
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
