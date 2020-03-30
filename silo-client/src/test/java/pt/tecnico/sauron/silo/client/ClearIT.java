package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.CoordsDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;

import java.util.LinkedList;

public class ClearIT extends BaseIT {

    @Test //Empty Silo
    public void emptySilo() {
        Assertions.assertEquals("OK", siloFrontend.ctrlClear());
    }

    //Run after init and report are completed
   /* @Test //Silo with observations and Cameras
    public void fullSilo() {
        LinkedList<CamDto> list = createCams(5);
        populateSilo(list);
        Assertions.assertEquals("OK", siloFrontend.ctrlClear());
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

    public void populateSilo(LinkedList<CamDto> list) {
        LinkedList<ObservationDto> obsList = new LinkedList<>();
        for(CamDto cam: list) {
            String idPerson = cam.getName() + " Person";
            String idCar = cam.getName() + " Car";
            ObservationDto obsPerson = new ObservationDto(ObservationDto.ObservationType.PERSON, idPerson);
            ObservationDto obsCar = new ObservationDto(ObservationDto.ObservationType.CAR, idCar);
            obsList.add(obsPerson);
            obsList.add(obsCar);
            siloFrontend.report(cam.getName(), obsList);
            obsList.clear();
        }
    }
*/
}
