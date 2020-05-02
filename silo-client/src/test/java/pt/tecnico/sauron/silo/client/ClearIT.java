package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.domain.FrontendCam;
import pt.tecnico.sauron.silo.client.domain.FrontendObservation;
import pt.tecnico.sauron.silo.client.domain.FrontendReport;
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
                Assertions.assertThrows(NotFoundException.class, () -> siloFrontend.trackMatch(FrontendObservation.ObservationType.CAR, "*"))
                        .getMessage());
        Assertions.assertEquals(ErrorMessages.OBSERVATION_NOT_FOUND,
                Assertions.assertThrows(NotFoundException.class, () -> siloFrontend.trackMatch(FrontendObservation.ObservationType.PERSON, "*"))
                        .getMessage());
    }

    @Test //Silo with observations and Cameras
    public void fullSilo() {
        try {
            LinkedList<FrontendCam> camList = createCams(5);
            LinkedList<FrontendReport> observations = createReports(5, camList);
            siloFrontend.ctrlInitCams(camList);
            siloFrontend.ctrlInitObservations(observations);
            Assertions.assertDoesNotThrow(()->siloFrontend.ctrlClear());
            Assertions.assertEquals(ErrorMessages.OBSERVATION_NOT_FOUND,
                    Assertions.assertThrows(NotFoundException.class, () -> siloFrontend.trackMatch(FrontendObservation.ObservationType.CAR, "*"))
                            .getMessage());
            Assertions.assertEquals(ErrorMessages.OBSERVATION_NOT_FOUND,
                    Assertions.assertThrows(NotFoundException.class, () -> siloFrontend.trackMatch(FrontendObservation.ObservationType.PERSON, "*"))
                            .getMessage());
        } catch (FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }


    public LinkedList<FrontendCam> createCams(int count) {
        LinkedList<FrontendCam> list = new LinkedList<>();
        for(int i = 1; i <= count; i++) {
            double lat = i*1.2;
            double lon = i*-1.2;
            FrontendCam cam = new FrontendCam("Camera "+i, lat, lon);
            list.add(cam);
        }
        return list;
    }

    public LinkedList<FrontendReport> createReports(int count, List<FrontendCam> camList) {
        LinkedList<FrontendReport> list =  new LinkedList<>();
        for(int i = 0; i < count; i++ ) {
            FrontendCam at = camList.get(i);
            FrontendObservation personObs =  new FrontendObservation(FrontendObservation.ObservationType.PERSON, String.valueOf(i));
            FrontendReport personReport = new FrontendReport(personObs, at, Instant.now());
            FrontendObservation carObs = new FrontendObservation(FrontendObservation.ObservationType.CAR, "AAAA00");
            FrontendReport carReport = new FrontendReport(carObs, at, Instant.now());
            list.add(personReport);
            list.add(carReport);
        }
        return list;
    }

}
