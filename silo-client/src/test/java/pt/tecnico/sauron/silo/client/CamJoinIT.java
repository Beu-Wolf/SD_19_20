package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.silo.client.domain.FrontendCam;
import pt.tecnico.sauron.silo.client.domain.FrontendCoords;
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
        FrontendCam cam = new FrontendCam(name, lat, lon);
        try {
            siloFrontend.camJoin(cam);
            FrontendCoords coords = siloFrontend.camInfo(cam.getName());
            FrontendCam serverCam = new FrontendCam(name, coords.getLat(), coords.getLon());
            Assertions.assertEquals(cam.toString(), serverCam.toString());
        } catch (FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void joinCameraNameDuplicateTest() {
        try {
            FrontendCam cam = new FrontendCam(name, lat, lon);
            FrontendCam duplicate = new FrontendCam(name, newLat , newLon);
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
            FrontendCam cam = new FrontendCam(name, lat, lon);
            siloFrontend.camJoin(cam);
            Assertions.assertDoesNotThrow(()->siloFrontend.camJoin(cam));
            FrontendCoords coords =  siloFrontend.camInfo(cam.getName());
            FrontendCam serverCam = new FrontendCam(name, coords.getLat(), coords.getLon());
            Assertions.assertEquals(cam,serverCam);

        } catch (FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void camJoinWithBlankName() {
        FrontendCam cam = new FrontendCam("", lat, lon);
        Assertions.assertEquals("Camera name is empty!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(cam))
                .getMessage() );
    }

    @Test
    public void camJoinWithShortName() {
        FrontendCam shortCam = new FrontendCam(shortName, lat, lon);
        Assertions.assertEquals("Camera names must be between 3 and 15 characters long!", Assertions.assertThrows(
        CameraRegisterException.class,()->siloFrontend.camJoin(shortCam))
         .getMessage());
    }

    @Test
    public void camJoinWithLongName() {
        FrontendCam longCam = new FrontendCam(longName, lat, lon);
        Assertions.assertEquals("Camera names must be between 3 and 15 characters long!", Assertions.assertThrows(
        CameraRegisterException.class,()->siloFrontend.camJoin(longCam))
        .getMessage());
    }

    @Test
    public void camJoinInvalidLatitude() {
        FrontendCam badNegLatCam = new FrontendCam(name, badNegativeLat, lon);
        Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(badNegLatCam))
                .getMessage());
        FrontendCam badPosLatCam = new FrontendCam(name, badPositiveLat, lon);
        Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(badPosLatCam))
                .getMessage());
    }

    @Test
    public void camJoinInvalidLongitude() {
        FrontendCam badNegLonCam = new FrontendCam(name, lat, badNegativeLon);
        Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(badNegLonCam))
                .getMessage());
        FrontendCam badPosLonCam = new FrontendCam(name, lat, badPositiveLon);
        Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                CameraRegisterException.class, ()->siloFrontend.camJoin(badPosLonCam))
                .getMessage());
    }


    @AfterEach
    public void clear() {
        try {
            siloFrontend.ctrlClear();
            siloFrontend.resetFrontendTS();
        } catch (FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

}
