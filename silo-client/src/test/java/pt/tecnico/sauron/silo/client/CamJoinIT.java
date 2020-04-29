package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pt.sauron.silo.contract.domain.Cam;
import pt.sauron.silo.contract.domain.Coords;
import pt.sauron.silo.contract.domain.exceptions.EmptyCameraNameException;
import pt.sauron.silo.contract.domain.exceptions.InvalidCameraNameException;
import pt.tecnico.sauron.silo.client.exceptions.CameraAlreadyExistsException;
import pt.tecnico.sauron.silo.client.exceptions.CameraRegisterException;
import pt.tecnico.sauron.silo.client.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;
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
        Assertions.assertDoesNotThrow(() -> {
            Cam cam = new Cam(name, new Coords(lat, lon));

            siloFrontend.camJoin(cam);
            Coords coords = siloFrontend.camInfo(cam.getName());
            Cam serverCam = new Cam(name, coords);
            Assertions.assertEquals(cam.toString(), serverCam.toString());
        });
    }

    @Test
    public void joinCameraNameDuplicateTest() {
        Assertions.assertDoesNotThrow(() -> {
            Cam cam = new Cam(name, new Coords(lat, lon));
            Cam duplicate = new Cam(name, new Coords(newLat , newLon));
            siloFrontend.camJoin(cam);
            Assertions.assertEquals(
                ErrorMessages.CAMERA_ALREADY_EXISTS,
                Assertions.assertThrows(
                    CameraAlreadyExistsException.class, () -> {
                        siloFrontend.camJoin(duplicate);
                    }
                ).getMessage()
            );

        });
    }

    @Test
    public void joinSameCameraTwiceTest() {
        Assertions.assertDoesNotThrow(() -> {
            Cam cam = new Cam(name, new Coords(lat, lon));
            siloFrontend.camJoin(cam);
            Assertions.assertDoesNotThrow(()->siloFrontend.camJoin(cam));
            Coords coords =  siloFrontend.camInfo(cam.getName());
            Cam serverCam = new Cam(name, coords);
            Assertions.assertEquals(cam,serverCam);

        });
    }

    @Test
    public void camJoinWithBlankName() {
        Assertions.assertEquals("Camera name is empty!",
            Assertions.assertThrows(
                EmptyCameraNameException.class, () -> {
                    Cam cam = new Cam("", new Coords(lat, lon));
                    siloFrontend.camJoin(cam);
                })
            .getMessage());
    }

    @Test
    public void camJoinWithShortName() {
        Assertions.assertEquals("Camera names must be between 3 and 15 characters long!",
            Assertions.assertThrows(
            InvalidCameraNameException.class, () -> {
                Cam shortCam = new Cam(shortName, new Coords(lat, lon));
                siloFrontend.camJoin(shortCam);
            })
            .getMessage());
    }

    @Test
    public void camJoinWithLongName() {
        Assertions.assertEquals("Camera names must be between 3 and 15 characters long!", Assertions.assertThrows(
            InvalidCameraNameException.class, () -> {
                Cam longCam = new Cam(longName, new Coords(lat, lon));
                siloFrontend.camJoin(longCam);
            })
            .getMessage());
    }

    @Test
    public void camJoinInvalidLatitude() {
        Assertions.assertDoesNotThrow( () -> {
            Cam badNegLatCam = new Cam(name, new Coords(badNegativeLat, lon));
            Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                    CameraRegisterException.class, () -> siloFrontend.camJoin(badNegLatCam))
                    .getMessage());
            Cam badPosLatCam = new Cam(name, new Coords(badPositiveLat, lon));
            Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                    CameraRegisterException.class, () -> siloFrontend.camJoin(badPosLatCam))
                    .getMessage());
        });
    }

    @Test
    public void camJoinInvalidLongitude() {
        Assertions.assertDoesNotThrow( () -> {
            Cam badNegLonCam = new Cam(name, new Coords(lat, badNegativeLon));
            Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                    CameraRegisterException.class, () -> siloFrontend.camJoin(badNegLonCam))
                    .getMessage());
            Cam badPosLonCam = new Cam(name, new Coords(lat, badPositiveLon));
            Assertions.assertEquals("Invalid Camera coordinates!", Assertions.assertThrows(
                    CameraRegisterException.class, () -> siloFrontend.camJoin(badPosLonCam))
                    .getMessage());
        });
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
