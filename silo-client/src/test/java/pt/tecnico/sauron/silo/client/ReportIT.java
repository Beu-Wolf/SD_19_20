package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.domain.Cam;
import pt.tecnico.sauron.silo.client.domain.Observation;
import pt.tecnico.sauron.silo.client.exceptions.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

public class ReportIT extends BaseIT {
    private static String cameraName = "testCamera";
    private static int LOADTESTOBS = 200;

    @BeforeAll
    public static void registerCamera() {
        Cam cam = new Cam(cameraName, 10, 10);
        try {
            siloFrontend.camJoin(cam);
        } catch(CameraAlreadyExistsException e) {
            // ignore
        } catch(FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void valid() {
        Observation personDto = new Observation(Observation.ObservationType.PERSON, "123");
        Observation carDto = new Observation(Observation.ObservationType.CAR, "AA00BB");

        LinkedList<Observation> list = new LinkedList<>();
        list.add(personDto);
        list.add(carDto);

        assertDoesNotThrow(() -> siloFrontend.report(this.cameraName, list));
    }

    @Test
    public void validLoadTest() {
        LinkedList<Observation> list = new LinkedList<>();

        for (int i = 1; i <= LOADTESTOBS; i++) {
            Observation personDto = new Observation(Observation.ObservationType.PERSON, String.valueOf(i));
            Observation carDto = new Observation(Observation.ObservationType.CAR, "AA" + String.format("%04d", i));
            list.add(personDto);
            list.add(carDto);
        }

        assertDoesNotThrow(()->siloFrontend.report(this.cameraName, list));
    }

    @Test
    public void invalidPersonId() {
        String invalidId = "asdf";
        Observation observation = new Observation(Observation.ObservationType.PERSON, invalidId);
        LinkedList<Observation> list = new LinkedList<>();
        list.add(observation);
        assertEquals(invalidId + ": Person ID must be an unsigned long!",
                assertThrows(FrontendException.class, () -> siloFrontend.report(this.cameraName, list)).getMessage());
    }

    @Test
    public void invalidCarId() {
        String invalidId = "asdf";
        Observation observation = new Observation(Observation.ObservationType.CAR, invalidId);
        LinkedList<Observation> list = new LinkedList<>();
        list.add(observation);
        assertEquals(invalidId + ": Car ID must be a valid portuguese license plate!",
                assertThrows(FrontendException.class, () -> siloFrontend.report(this.cameraName, list)).getMessage());
    }

    @Test
    public void invalidType() {
        Observation observation = new Observation(Observation.ObservationType.UNSPEC, "asdf");
        LinkedList<Observation> list = new LinkedList<>();
        list.add(observation);
        assertEquals("Type to observe not supported!",
                assertThrows(FrontendException.class, () -> siloFrontend.report(this.cameraName, list)).getMessage());
    }

    @Test
    public void emptyList() {
        LinkedList<Observation> list = new LinkedList<>();
        assertDoesNotThrow(() -> siloFrontend.report(this.cameraName, list));
    }

    @Test
    public void unknownCamera() {
        String camName = "thisCamDoesntExist123";
        Observation observation = new Observation(Observation.ObservationType.PERSON, "asdf");
        LinkedList<Observation> list = new LinkedList<>();
        list.add(observation);
        assertEquals("Camera not found",
                assertThrows(CameraNotFoundException.class, () -> siloFrontend.report(camName, list)).getMessage());
    }

    @AfterAll
    public static void clear() {
        try {
            siloFrontend.ctrlClear();
        } catch (FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

}
