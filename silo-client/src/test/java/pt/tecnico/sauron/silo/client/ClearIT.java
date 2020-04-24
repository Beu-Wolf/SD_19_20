package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.dto.ReportDto;
import pt.tecnico.sauron.silo.client.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;
import pt.tecnico.sauron.silo.client.exceptions.NotFoundException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

public class ClearIT extends BaseIT {

    @Test //Empty Silo
    public void emptySilo() {

        Assertions.assertDoesNotThrow(()->siloFrontend.ctrlClear());
        Assertions.assertEquals(ErrorMessages.OBSERVATION_NOT_FOUND,
                Assertions.assertThrows(NotFoundException.class, () -> siloFrontend.trackMatch(ObservationDto.ObservationType.CAR, "*"))
                        .getMessage());
        Assertions.assertEquals(ErrorMessages.OBSERVATION_NOT_FOUND,
                Assertions.assertThrows(NotFoundException.class, () -> siloFrontend.trackMatch(ObservationDto.ObservationType.PERSON, "*"))
                        .getMessage());
    }

    @Test //Silo with observations and Cameras
    public void fullSilo() {
        try {
            LinkedList<CamDto> camList = createCams(5);
            LinkedList<ReportDto> observations = createReports(5, camList);
            siloFrontend.ctrlInitCams(camList);
            siloFrontend.ctrlInitObservations(observations);
            Assertions.assertDoesNotThrow(()->siloFrontend.ctrlClear());
            Assertions.assertEquals(ErrorMessages.OBSERVATION_NOT_FOUND,
                    Assertions.assertThrows(NotFoundException.class, () -> siloFrontend.trackMatch(ObservationDto.ObservationType.CAR, "*"))
                            .getMessage());
            Assertions.assertEquals(ErrorMessages.OBSERVATION_NOT_FOUND,
                    Assertions.assertThrows(NotFoundException.class, () -> siloFrontend.trackMatch(ObservationDto.ObservationType.PERSON, "*"))
                            .getMessage());
        } catch (InterruptedException | FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }


    public LinkedList<CamDto> createCams(int count) {
        LinkedList<CamDto> list = new LinkedList<>();
        for(int i = 1; i <= count; i++) {
            double lat = i*1.2;
            double lon = i*-1.2;
            CamDto cam = new CamDto("Camera "+i, lat, lon);
            list.add(cam);
        }
        return list;
    }

    public LinkedList<ReportDto> createReports(int count, List<CamDto> camList) {
        LinkedList<ReportDto> list =  new LinkedList<>();
        for(int i = 0; i < count; i++ ) {
            CamDto at = camList.get(i);
            ObservationDto personObs =  new ObservationDto(ObservationDto.ObservationType.PERSON, String.valueOf(i));
            ReportDto personReport = new ReportDto(personObs, at, Instant.now());
            ObservationDto carObs = new ObservationDto(ObservationDto.ObservationType.CAR, "AAAA00");
            ReportDto carReport = new ReportDto(carObs, at, Instant.now());
            list.add(personReport);
            list.add(carReport);
        }
        return list;
    }
}
