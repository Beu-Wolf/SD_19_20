package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.dto.FrontendCam;
import pt.tecnico.sauron.silo.client.dto.FrontendObservation;
import pt.tecnico.sauron.silo.client.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;

public class ReportIT extends BaseIT {
    private static String cameraName = "testCamera";

    @BeforeAll
    public static void registerCamera() {
        FrontendCam frontendCam = new FrontendCam(cameraName, 10, 10);
        try {
            siloFrontend.camJoin(frontendCam);
        } catch(CameraAlreadyExistsException e) {
            // ignore
        } catch(CameraRegisterException e) {
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

        assertDoesNotThrow(() -> siloFrontend.report(this.cameraName, list));
    }

    @Test
    public void invalidPersonId() {
        FrontendObservation frontendObservation = new FrontendObservation(FrontendObservation.ObservationType.PERSON, "asdf");
        LinkedList<FrontendObservation> list = new LinkedList<>();
        list.add(frontendObservation);
        assertEquals("Person ID must be an unsigned long!",
                assertThrows(InvalidArgumentException.class, () -> siloFrontend.report(this.cameraName, list)).getMessage());
    }

    @Test
    public void invalidCarId() {
        FrontendObservation frontendObservation = new FrontendObservation(FrontendObservation.ObservationType.CAR, "asdf");
        LinkedList<FrontendObservation> list = new LinkedList<>();
        list.add(frontendObservation);
        assertEquals("Car ID must be a valid portuguese license plate!",
                assertThrows(InvalidArgumentException.class, () -> siloFrontend.report(this.cameraName, list)).getMessage());
    }

    @Test
    public void invalidType() {
        FrontendObservation frontendObservation = new FrontendObservation(FrontendObservation.ObservationType.UNSPEC, "asdf");
        LinkedList<FrontendObservation> list = new LinkedList<>();
        list.add(frontendObservation);
        assertEquals("Type to observe not supported!",
                assertThrows(InvalidArgumentException.class, () -> siloFrontend.report(this.cameraName, list)).getMessage());
    }

    @Test
    public void emptyList() {
        LinkedList<FrontendObservation> list = new LinkedList<>();
        assertDoesNotThrow(() -> siloFrontend.report(this.cameraName, list));
    }

    @Test
    public void unknownCamera() {
        String camName = "thisCamDoesntExist123";
        FrontendObservation frontendObservation = new FrontendObservation(FrontendObservation.ObservationType.PERSON, "asdf");
        LinkedList<FrontendObservation> list = new LinkedList<>();
        list.add(frontendObservation);
        assertEquals("Camera not found",
                assertThrows(CameraNotFoundException.class, () -> siloFrontend.report(camName, list)).getMessage());
    }

    @AfterAll
    public static void clear() {
        try {
            siloFrontend.ctrlClear();
        } catch (ClearException e) {
            e.printStackTrace();
        }
    }

}
