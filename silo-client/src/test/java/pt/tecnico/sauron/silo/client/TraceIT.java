package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.dto.ReportDto;
import pt.tecnico.sauron.silo.client.exceptions.InvalidArgumentException;
import pt.tecnico.sauron.silo.client.exceptions.NotFoundException;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;

public class TraceIT extends BaseIT {

    private static final String[] invalidPersonIDs = {
            "-4982374",
            "20SD20",
            "324*343"
    };

    private static final String[] invalidCarIDs = {
            "AABBCC",
            "112233",
            "AA11BB22",
            "A11BB2",
            "A1*B2"
    };

    private static final String getNotSeenPeronId = "222222";
    private static final String[] validPersonIds = {
            "111111",
    };

    private static final String notSeenCarId = "ZZ99ZZ";
    private static final String[] validCarIds = {
            "AA00AA",
    };

    private static final String[] camNames = {
            "First",
            "Second",
            "Third",
            "Fourth",
            "Fifth",
    };

    private static final CamDto[] cams = {
            new CamDto(camNames[0], 0, 0),
            new CamDto(camNames[1], 1, 1),
            new CamDto(camNames[2], 2, 2),
            new CamDto(camNames[3], 3, 3),
            new CamDto(camNames[4], 4, 4)
    };

    @BeforeAll
    public static void setupTrack () {
        Instant instant = Instant.now();
        Instant[] instants = {
                instant.minus(4, DAYS),
                instant.minus(3, DAYS),
                instant.minus(2, DAYS),
                instant.minus(1, DAYS),
                instant
        };


        List<ReportDto> reports = new LinkedList<>();
        Assertions.assertEquals(cams.length, instants.length);
        for(int i = 0; i < cams.length; i++) {
            reports.add(new ReportDto(
                    new ObservationDto(ObservationDto.ObservationType.CAR, validCarIds[0]),
                    cams[i],
                    instants[i]));
            reports.add(new ReportDto(
                    new ObservationDto(ObservationDto.ObservationType.CAR, validPersonIds[0]),
                    cams[i],
                    instants[i]));
        }

        try {
            siloFrontend.ctrlInitCams(new LinkedList<>(Arrays.asList(cams)));
            siloFrontend.ctrlInitObservations(reports);
        } catch(Exception e) {
            System.err.println(e);
        }
    }

    @Test
    public void trackNonExistingTypeTest() {
        Assertions.assertThrows(InvalidArgumentException.class, () -> {
            this.siloFrontend.trace(ObservationDto.ObservationType.UNSPEC, "1337_5p34k");
        });
    }

    @Test
    public void testInvalidPersonID() {
        for(String invalidId : invalidPersonIDs) {
            Assertions.assertThrows(InvalidArgumentException.class, () -> {
                this.siloFrontend.trace(ObservationDto.ObservationType.PERSON, invalidId);
            });
        }
    }

    @Test
    public void testInvalidCarID() {
        for(String invalidId : invalidCarIDs) {
            Assertions.assertThrows(InvalidArgumentException.class, () -> {
                this.siloFrontend.trace(ObservationDto.ObservationType.CAR, invalidId);
            });
        }
    }


    @Test
    public void traceNonExistingCar() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            siloFrontend.trace(ObservationDto.ObservationType.CAR, notSeenCarId);
        });
    }

    @Test
    public void traceNonExistingPerson() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            siloFrontend.trace(ObservationDto.ObservationType.PERSON, getNotSeenPeronId);
        });
    }

    @Test
    public void traceExistingPerson() {
        try {
            List<ReportDto> response = siloFrontend.trace(ObservationDto.ObservationType.PERSON, validPersonIds[0]);
            Assertions.assertEquals(response.size(), cams.length);
            for(int i = 0; i < cams.length; i++) {
                // sorted by new
                Assertions.assertEquals(response.get(i).getCam(), cams[cams.length-i-1]);
                Assertions.assertEquals(response.get(i).getId(), invalidPersonIDs[0]);
            }

        } catch(Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void traceExistingCar() {
        try {
            List<ReportDto> response = siloFrontend.trace(ObservationDto.ObservationType.CAR, validCarIds[0]);
            Assertions.assertEquals(response.size(), cams.length);
            for(int i = 0; i < cams.length; i++) {
                // sorted by new
                Assertions.assertEquals(response.get(i).getCam(), cams[cams.length-i-1]);
                Assertions.assertEquals(response.get(i).getId(), invalidCarIDs[0]);
            }

        } catch(Exception e) {
            Assertions.fail(e);
        }
    }
}
