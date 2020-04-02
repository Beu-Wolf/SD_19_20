package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.dto.ReportDto;
import pt.tecnico.sauron.silo.client.exceptions.InvalidArgumentException;
import pt.tecnico.sauron.silo.client.exceptions.NotFoundException;
import pt.tecnico.sauron.silo.client.exceptions.QueryException;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;

public class TrackMatchIT extends BaseIT {

    private static final String[] invalidPersonIDs = {
            "-4982374",
            "20SD20",
    };

    private static final String[] invalidCarIDs = {
            "AABBCC",
            "112233",
            "AA11BB22",
            "A11BB2",
    };

    private static final String notSeenPersonId = "999999";
    private static final String seenPeoplePatternId = "1*";
    private static final String[] seenPersonIds = {
            "111112",
            "111113"
    };

    private static final String notSeenCarId = "ZZ99ZZ";
    private static final String seenCarsPatternId = "AA*AA";
    private static final String[] seenCarIds = {
            "AA00AA",
            "AA11AA"
    };

    private static final CamDto[] cams = {
            new CamDto("First", 0, 0),
            new CamDto("Second", 1, 1)
    };

    @BeforeAll
    public static void setupTrackMatch () {
        Instant instant = Instant.now();

        // old observations by cams[0]
        List<ReportDto> reports = new LinkedList<>();
        for(String id : seenPersonIds) {
            reports.add(new ReportDto(
                    new ObservationDto(ObservationDto.ObservationType.PERSON, id),
                    cams[0],
                    instant.minus(1, DAYS)));
        }
        for(String id : seenCarIds) {
            reports.add(new ReportDto(
                    new ObservationDto(ObservationDto.ObservationType.CAR, id),
                    cams[0],
                    instant.minus(1, DAYS)));
        }

        // most recent observations by cams[1]
        for(String id : seenPersonIds) {
            reports.add(new ReportDto(
                    new ObservationDto(ObservationDto.ObservationType.PERSON, id),
                    cams[1],
                    instant));
        }
        for(String id : seenCarIds) {
            reports.add(new ReportDto(
                    new ObservationDto(ObservationDto.ObservationType.CAR, id),
                    cams[1],
                    instant));
        }

        try {
            siloFrontend.ctrlInitCams(new LinkedList<>(Arrays.asList(cams)));
            siloFrontend.ctrlInitObservations(reports);
        } catch(Exception e) {
            System.err.println(e);
        }
    }

    @Test
    public void trackMatchNonExistingTypeTest() {
        Assertions.assertThrows(InvalidArgumentException.class, () -> {
            this.siloFrontend.trackMatch(ObservationDto.ObservationType.UNSPEC, "1337_5p34k");
        });
    }

    @Test
    public void testInvalidPersonID() {
        for(String invalidId : invalidPersonIDs) {
            Assertions.assertThrows(InvalidArgumentException.class, () -> {
                this.siloFrontend.trackMatch(ObservationDto.ObservationType.PERSON, invalidId);
            });
        }
    }

    @Test
    public void testInvalidCarID() {
        for(String invalidId : invalidCarIDs) {
            Assertions.assertThrows(InvalidArgumentException.class, () -> {
                this.siloFrontend.trackMatch(ObservationDto.ObservationType.CAR, invalidId);
            });
        }
    }


    @Test
    public void trackMatchNonExistingCar() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            siloFrontend.track(ObservationDto.ObservationType.CAR, notSeenCarId);
        });
    }

    @Test
    public void trackMatchNonExistingPerson() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            siloFrontend.trackMatch(ObservationDto.ObservationType.PERSON, notSeenPersonId);
        });
    }

    @Test
    public void trackMatchPeople() {
        try {
            List<ReportDto> results = siloFrontend.trackMatch(ObservationDto.ObservationType.PERSON, seenPeoplePatternId);
            Assertions.assertEquals(results.size(), 2);
            for(ReportDto report : results) {
                Assertions.assertEquals(report.getCam(), cams[1]);
                Assertions.assertTrue(Arrays.asList(seenPersonIds).contains(report.getId()));
            }
        } catch(QueryException e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void trackMatchCars() {
        try {
            List<ReportDto> results = siloFrontend.trackMatch(ObservationDto.ObservationType.CAR, seenCarsPatternId);
            Assertions.assertEquals(results.size(), 2);
            for(ReportDto report : results) {
                Assertions.assertEquals(report.getCam(), cams[1]);
                Assertions.assertTrue(Arrays.asList(seenCarIds).contains(report.getId()));
            }
        } catch(QueryException e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void trackMatchPersonNoPattern() {
        try {
            List<ReportDto> results = siloFrontend.trackMatch(ObservationDto.ObservationType.PERSON, seenPersonIds[0]);
            Assertions.assertEquals(results.size(), 1);
            ReportDto report = results.get(0);
            Assertions.assertEquals(report.getCam(), cams[1]);
            Assertions.assertTrue(report.getId() == seenPersonIds[0]);
        } catch(QueryException e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void trackMatchCarNoPattern() {
        try {
            List<ReportDto> results = siloFrontend.trackMatch(ObservationDto.ObservationType.PERSON, seenCarIds[0]);
            Assertions.assertEquals(results.size(), 1);
            ReportDto report = results.get(0);
            Assertions.assertEquals(report.getCam(), cams[1]);
            Assertions.assertTrue(report.getId() == seenPersonIds[0]);
        } catch(QueryException e) {
            Assertions.fail(e);
        }
    }
}
