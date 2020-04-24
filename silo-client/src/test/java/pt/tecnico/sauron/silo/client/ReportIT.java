package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.exceptions.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;

public class ReportIT extends BaseIT {
    private static String cameraName = "testCamera";
    private static int LOADTESTOBS = 200;

    @BeforeAll
    public static void registerCamera() {
        CamDto camDto = new CamDto(cameraName, 10, 10);
        try {
            siloFrontend.camJoin(camDto);
        } catch(CameraAlreadyExistsException e) {
            // ignore
        } catch(FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void valid() {
        ObservationDto personDto = new ObservationDto(ObservationDto.ObservationType.PERSON, "123");
        ObservationDto carDto = new ObservationDto(ObservationDto.ObservationType.CAR, "AA00BB");

        LinkedList<ObservationDto> list = new LinkedList<>();
        list.add(personDto);
        list.add(carDto);

        assertDoesNotThrow(() -> siloFrontend.report(this.cameraName, list));
    }

    @Test
    public void validLoadTest() {
        LinkedList<ObservationDto> list = new LinkedList<>();

        for (int i = 1; i <= LOADTESTOBS; i++) {
            ObservationDto personDto = new ObservationDto(ObservationDto.ObservationType.PERSON, String.valueOf(i));
            ObservationDto carDto = new ObservationDto(ObservationDto.ObservationType.CAR, "AA" + String.format("%04d", i));
            list.add(personDto);
            list.add(carDto);
        }

        assertDoesNotThrow(()->siloFrontend.report(this.cameraName, list));
    }

    @Test
    public void invalidPersonId() {
        ObservationDto observationDto = new ObservationDto(ObservationDto.ObservationType.PERSON, "asdf");
        LinkedList<ObservationDto> list = new LinkedList<>();
        list.add(observationDto);
        assertEquals("Person ID must be an unsigned long!",
                assertThrows(InvalidArgumentException.class, () -> siloFrontend.report(this.cameraName, list)).getMessage());
    }

    @Test
    public void invalidCarId() {
        ObservationDto observationDto = new ObservationDto(ObservationDto.ObservationType.CAR, "asdf");
        LinkedList<ObservationDto> list = new LinkedList<>();
        list.add(observationDto);
        assertEquals("Car ID must be a valid portuguese license plate!",
                assertThrows(InvalidArgumentException.class, () -> siloFrontend.report(this.cameraName, list)).getMessage());
    }

    @Test
    public void invalidType() {
        ObservationDto observationDto = new ObservationDto(ObservationDto.ObservationType.UNSPEC, "asdf");
        LinkedList<ObservationDto> list = new LinkedList<>();
        list.add(observationDto);
        assertEquals("Type to observe not supported!",
                assertThrows(InvalidArgumentException.class, () -> siloFrontend.report(this.cameraName, list)).getMessage());
    }

    @Test
    public void emptyList() {
        LinkedList<ObservationDto> list = new LinkedList<>();
        assertDoesNotThrow(() -> siloFrontend.report(this.cameraName, list));
    }

    @Test
    public void unknownCamera() {
        String camName = "thisCamDoesntExist123";
        ObservationDto observationDto = new ObservationDto(ObservationDto.ObservationType.PERSON, "asdf");
        LinkedList<ObservationDto> list = new LinkedList<>();
        list.add(observationDto);
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
