package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.exceptions.CameraAlreadyExistsException;
import pt.tecnico.sauron.silo.client.exceptions.CameraNotFoundException;
import pt.tecnico.sauron.silo.client.exceptions.CameraRegisterException;
import pt.tecnico.sauron.silo.client.exceptions.InvalidArgumentException;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;

public class ReportIT extends BaseIT {
    private static String cameraName = "testCamera";

    @BeforeAll
    public static void registerCamera() {
        CamDto camDto = new CamDto(cameraName, 10, 10);
        try {
            siloFrontend.camJoin(camDto);
        } catch(CameraAlreadyExistsException| CameraRegisterException e) {}
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
    public void invalidId() {
        ObservationDto observationDto = new ObservationDto(ObservationDto.ObservationType.PERSON, "asdf");
        LinkedList<ObservationDto> list = new LinkedList<>();
        list.add(observationDto);
        assertThrows(InvalidArgumentException.class, () -> siloFrontend.report(this.cameraName, list));
    }

    @Test
    public void invalidType() {
        ObservationDto observationDto = new ObservationDto(ObservationDto.ObservationType.UNSPEC, "asdf");
        LinkedList<ObservationDto> list = new LinkedList<>();
        list.add(observationDto);
        assertThrows(InvalidArgumentException.class, () -> siloFrontend.report(this.cameraName, list));
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
        assertThrows(CameraNotFoundException.class, () -> siloFrontend.report(camName, list));
    }

}
