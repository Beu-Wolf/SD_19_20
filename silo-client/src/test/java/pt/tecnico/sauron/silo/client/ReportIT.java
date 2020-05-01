package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.domain.FrontendCam;
import pt.tecnico.sauron.silo.client.domain.FrontendObservation;
import pt.tecnico.sauron.silo.client.exceptions.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

public class ReportIT extends BaseIT {
    private static String cameraName = "testCamera";
    private static FrontendCam frontendCam;
    private static int LOADTESTOBS = 100;

    @BeforeAll
    public static void registerCamera() {
        frontendCam = new FrontendCam(cameraName, 10, 10);
        try {
            siloFrontend.camJoin(frontendCam);
        } catch(CameraAlreadyExistsException e) {
            // ignore
        } catch(FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void valid() {
        FrontendObservation personDto = new FrontendObservation(FrontendObservation.ObservationType.PERSON, "123");
        FrontendObservation carDto = new FrontendObservation(FrontendObservation.ObservationType.CAR, "AA00BB");

        LinkedList<FrontendObservation> list = new LinkedList<>();
        list.add(personDto);
        list.add(carDto);

        assertDoesNotThrow(() -> siloFrontend.report(frontendCam, list));
    }

    @Test
    public void validLoadTest() {
        LinkedList<FrontendObservation> list = new LinkedList<>();

        for (int i = 1; i <= LOADTESTOBS; i++) {
            FrontendObservation personDto = new FrontendObservation(FrontendObservation.ObservationType.PERSON, String.valueOf(i));
            FrontendObservation carDto = new FrontendObservation(FrontendObservation.ObservationType.CAR, "AA" + String.format("%04d", i));
            list.add(personDto);
            list.add(carDto);
        }

        assertDoesNotThrow(()->siloFrontend.report(frontendCam, list));
    }

    @Test
    public void invalidPersonId() {
        String invalidId = "asdf";
        FrontendObservation frontendObservation = new FrontendObservation(FrontendObservation.ObservationType.PERSON, invalidId);
        LinkedList<FrontendObservation> list = new LinkedList<>();
        list.add(frontendObservation);
        assertEquals(invalidId + ": Person ID must be an unsigned long!",
                assertThrows(FrontendException.class, () -> siloFrontend.report(frontendCam, list)).getMessage());
    }

    @Test
    public void invalidCarId() {
        String invalidId = "asdf";
        FrontendObservation frontendObservation = new FrontendObservation(FrontendObservation.ObservationType.CAR, invalidId);
        LinkedList<FrontendObservation> list = new LinkedList<>();
        list.add(frontendObservation);
        assertEquals(invalidId + ": Car ID must be a valid portuguese license plate!",
                assertThrows(FrontendException.class, () -> siloFrontend.report(frontendCam, list)).getMessage());
    }

    @Test
    public void invalidType() {
        FrontendObservation frontendObservation = new FrontendObservation(FrontendObservation.ObservationType.UNSPEC, "asdf");
        LinkedList<FrontendObservation> list = new LinkedList<>();
        list.add(frontendObservation);
        assertEquals("Type to observe not supported!",
                assertThrows(FrontendException.class, () -> siloFrontend.report(frontendCam, list)).getMessage());
    }

    @Test
    public void emptyList() {
        LinkedList<FrontendObservation> list = new LinkedList<>();
        assertDoesNotThrow(() -> siloFrontend.report(frontendCam , list));
    }

    @Test
    public void unknownCamera() {
        String camName = "thisCamDoesntExist123";
        FrontendCam cam = new FrontendCam(camName, 1.1,1.1);
        FrontendObservation frontendObservation = new FrontendObservation(FrontendObservation.ObservationType.PERSON, "asdf");
        LinkedList<FrontendObservation> list = new LinkedList<>();
        list.add(frontendObservation);
        assertEquals("Camera not found",
                assertThrows(CameraNotFoundException.class, () -> siloFrontend.report(cam, list)).getMessage());
    }

    @AfterAll
    public static void clear() {
        try {
            siloFrontend.ctrlClear();
            siloFrontend.resetFrontendTS();
        } catch (FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

}
