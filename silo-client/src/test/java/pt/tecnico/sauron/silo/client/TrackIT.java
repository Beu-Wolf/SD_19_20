package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.dto.ReportDto;
import pt.tecnico.sauron.silo.client.exceptions.ClearException;
import pt.tecnico.sauron.silo.client.exceptions.InvalidArgumentException;
import pt.tecnico.sauron.silo.client.exceptions.NotFoundException;
import pt.tecnico.sauron.silo.client.exceptions.QueryException;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;

public class TrackIT extends BaseIT {

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

    private static final String[] validPersonIds = {
            "111111",
            "222222"
    };

    private static final String[] validCarIds = {
            "AA00AA",
            "ZZ99ZZ"
    };

    private static final String[] camNames = {
            "First",
            "Second"
    };

    private static final CamDto[] cams = {
            new CamDto(camNames[0], 0, 0),
            new CamDto(camNames[1], 1, 1)
    };

    @BeforeAll
    public static void setupTrack () {
        Instant instant = Instant.now();

        List<ReportDto> reports = new LinkedList<>();
        reports.add(new ReportDto(
                new ObservationDto(ObservationDto.ObservationType.CAR, validCarIds[0]),
                cams[1],
                instant.minus(1, DAYS)));
        reports.add(new ReportDto(
                new ObservationDto(ObservationDto.ObservationType.PERSON, validPersonIds[0]),
                cams[1],
                instant.minus(1, DAYS)));

        // last report
        reports.add(new ReportDto(
                new ObservationDto(ObservationDto.ObservationType.CAR, validCarIds[0]),
                cams[0],
                instant));
        reports.add(new ReportDto(
                new ObservationDto(ObservationDto.ObservationType.PERSON, validPersonIds[0]),
                cams[0],
                instant));

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
            this.siloFrontend.track(ObservationDto.ObservationType.UNSPEC, "1337_5p34k");
        });
    }

    @Test
    public void testInvalidPersonID() {
        for(String invalidId : invalidPersonIDs) {
            Assertions.assertThrows(InvalidArgumentException.class, () -> {
                this.siloFrontend.track(ObservationDto.ObservationType.PERSON, invalidId);
            });
        }
    }

    @Test
    public void testInvalidCarID() {
        for(String invalidId : invalidCarIDs) {
            Assertions.assertThrows(InvalidArgumentException.class, () -> {
                this.siloFrontend.track(ObservationDto.ObservationType.CAR, invalidId);
            });
        }
    }


    @Test
    public void trackNonExistingCar() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            siloFrontend.track(ObservationDto.ObservationType.CAR, validCarIds[1]);
        });
    }

    @Test
    public void trackNonExistingPerson() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            siloFrontend.track(ObservationDto.ObservationType.PERSON, validPersonIds[1]);
        });
    }

    @Test
    public void trackExistingPerson() {
        try {
            ReportDto response = siloFrontend.track(ObservationDto.ObservationType.PERSON, validPersonIds[0]);
            Assertions.assertEquals(response.getCam(), cams[0]);
            Assertions.assertEquals(response.getId(), validPersonIds[0]);

        } catch(NotFoundException | InvalidArgumentException | QueryException e) {
            e.printStackTrace();
            Assertions.fail(e);
        }
    }

    @Test
    public void trackExistingCar() {
        try {
            ReportDto response = siloFrontend.track(ObservationDto.ObservationType.CAR, validCarIds[0]);
            Assertions.assertEquals(response.getCam(), cams[0]);
            Assertions.assertEquals(response.getId(), validCarIds[0]);

        } catch(Exception e) {
            Assertions.fail(e);
        }
    }

    @AfterEach
    public void tearDown() {
        try {
            siloFrontend.ctrlClear();
        } catch(ClearException e) {
            e.printStackTrace();
        }
    }
}
