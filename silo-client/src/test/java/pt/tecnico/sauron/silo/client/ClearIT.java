package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.domain.Cam;
import pt.tecnico.sauron.silo.client.domain.Observation;
import pt.tecnico.sauron.silo.client.domain.Report;
import pt.tecnico.sauron.silo.client.exceptions.ErrorMessages;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;
import pt.tecnico.sauron.silo.client.exceptions.NotFoundException;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

public class ClearIT extends BaseIT {

    @Test //Empty Silo
    public void emptySilo() {

        Assertions.assertDoesNotThrow(()->siloFrontend.ctrlClear());
        Assertions.assertEquals(ErrorMessages.OBSERVATION_NOT_FOUND,
                Assertions.assertThrows(NotFoundException.class, () -> siloFrontend.trackMatch(Observation.ObservationType.CAR, "*"))
                        .getMessage());
        Assertions.assertEquals(ErrorMessages.OBSERVATION_NOT_FOUND,
                Assertions.assertThrows(NotFoundException.class, () -> siloFrontend.trackMatch(Observation.ObservationType.PERSON, "*"))
                        .getMessage());
    }

    @Test //Silo with observations and Cameras
    public void fullSilo() {
        try {
            LinkedList<Cam> camList = createCams(5);
            LinkedList<Report> observations = createReports(5, camList);
            siloFrontend.ctrlInitCams(camList);
            siloFrontend.ctrlInitObservations(observations);
            Assertions.assertDoesNotThrow(()->siloFrontend.ctrlClear());
            Assertions.assertEquals(ErrorMessages.OBSERVATION_NOT_FOUND,
                    Assertions.assertThrows(NotFoundException.class, () -> siloFrontend.trackMatch(Observation.ObservationType.CAR, "*"))
                            .getMessage());
            Assertions.assertEquals(ErrorMessages.OBSERVATION_NOT_FOUND,
                    Assertions.assertThrows(NotFoundException.class, () -> siloFrontend.trackMatch(Observation.ObservationType.PERSON, "*"))
                            .getMessage());
        } catch (FrontendException e) {
            e.printStackTrace();
        }
    }


    public LinkedList<Cam> createCams(int count) {
        LinkedList<Cam> list = new LinkedList<>();
        for(int i = 1; i <= count; i++) {
            double lat = i*1.2;
            double lon = i*-1.2;
            Cam cam = new Cam("Camera "+i, lat, lon);
            list.add(cam);
        }
        return list;
    }

    public LinkedList<Report> createReports(int count, List<Cam> camList) {
        LinkedList<Report> list =  new LinkedList<>();
        for(int i = 0; i < count; i++ ) {
            Cam at = camList.get(i);
            Observation personObs =  new Observation(Observation.ObservationType.PERSON, String.valueOf(i));
            Report personReport = new Report(personObs, at, Instant.now());
            Observation carObs = new Observation(Observation.ObservationType.CAR, "AAAA00");
            Report carReport = new Report(carObs, at, Instant.now());
            list.add(personReport);
            list.add(carReport);
        }
        return list;
    }
}
