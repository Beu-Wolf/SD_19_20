package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.sauron.silo.contract.domain.Cam;
import pt.sauron.silo.contract.domain.Coords;
import pt.sauron.silo.contract.domain.exceptions.EmptyCameraNameException;
import pt.sauron.silo.contract.domain.exceptions.InvalidCameraNameException;
import pt.tecnico.sauron.silo.client.domain.Observation;
import pt.tecnico.sauron.silo.client.domain.Report;
import pt.tecnico.sauron.silo.client.exceptions.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

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

    private static
    Cam[] cams;

    static {
        try {
            cams = new Cam[]{
                        new Cam("First", new Coords(0, 0)),
                        new Cam("Second", new Coords(1, 1))
                };
        } catch (EmptyCameraNameException e) {
            e.printStackTrace();
        } catch (InvalidCameraNameException e) {
            e.printStackTrace();
        }
    }

    @BeforeAll
    public static void setupTrackMatch () {
        Instant instant = Instant.now();

        // old observations by cams[0]
        List<Report> reports = new LinkedList<>();
        for(String id : seenPersonIds) {
            reports.add(new Report(
                    new Observation(Observation.ObservationType.PERSON, id),
                    cams[0],
                    instant.minus(1, DAYS)));
        }
        for(String id : seenCarIds) {
            reports.add(new Report(
                    new Observation(Observation.ObservationType.CAR, id),
                    cams[0],
                    instant.minus(1, DAYS)));
        }

        // most recent observations by cams[1]
        for(String id : seenPersonIds) {
            reports.add(new Report(
                    new Observation(Observation.ObservationType.PERSON, id),
                    cams[1],
                    instant));
        }
        for(String id : seenCarIds) {
            reports.add(new Report(
                    new Observation(Observation.ObservationType.CAR, id),
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
        Assertions.assertEquals(
                "Type to observe not supported!",
                Assertions.assertThrows(InvalidArgumentException.class, () -> {
                    this.siloFrontend.track(Observation.ObservationType.UNSPEC, "1337_5p34k");
                }).getMessage()
        );
    }

    @Test
    public void testInvalidPersonID() {
        for(String invalidId : invalidPersonIDs) {
            Assertions.assertEquals(
                ErrorMessages.OBSERVATION_NOT_FOUND,
                Assertions.assertThrows(NotFoundException.class, () -> {
                    this.siloFrontend.trackMatch(Observation.ObservationType.PERSON, invalidId);
                }).getMessage()
            );
        }
    }

    @Test
    public void testInvalidCarID() {
        for(String invalidId : invalidCarIDs) {
            Assertions.assertEquals(
                ErrorMessages.OBSERVATION_NOT_FOUND,
                Assertions.assertThrows(NotFoundException.class, () -> {
                    this.siloFrontend.trackMatch(Observation.ObservationType.CAR, invalidId);
                }).getMessage()
            );
        }
    }


    @Test
    public void trackMatchNonExistingCar() {
        Assertions.assertEquals(
            ErrorMessages.OBSERVATION_NOT_FOUND,
            Assertions.assertThrows(NotFoundException.class, () -> {
                siloFrontend.track(Observation.ObservationType.CAR, notSeenCarId);
            }).getMessage()
        );
    }

    @Test
    public void trackMatchNonExistingPerson() {
        Assertions.assertEquals(
            ErrorMessages.OBSERVATION_NOT_FOUND,
            Assertions.assertThrows(NotFoundException.class, () -> {
                siloFrontend.trackMatch(Observation.ObservationType.PERSON, notSeenPersonId);
            }).getMessage()
        );
    }

    @Test
    public void trackMatchPeople() {
        Assertions.assertDoesNotThrow(() -> {
            List<Report> results = siloFrontend.trackMatch(Observation.ObservationType.PERSON, seenPeoplePatternId);
            Assertions.assertEquals(results.size(), 2);
            for(Report report : results) {
                Assertions.assertEquals(report.getCam(), cams[1]);
                Assertions.assertTrue(Arrays.asList(seenPersonIds).contains(report.getId()));
            }
        });
    }

    @Test
    public void trackMatchCars() {
        Assertions.assertDoesNotThrow(() -> {
            List<Report> results = siloFrontend.trackMatch(Observation.ObservationType.CAR, seenCarsPatternId);
            Assertions.assertEquals(results.size(), 2);
            for(Report report : results) {
                Assertions.assertEquals(report.getCam(), cams[1]);
                Assertions.assertTrue(Arrays.asList(seenCarIds).contains(report.getId()));
            }
        });
    }

    @Test
    public void trackMatchPersonNoPattern() {
        Assertions.assertDoesNotThrow(() -> {
            List<Report> results = siloFrontend.trackMatch(Observation.ObservationType.PERSON, seenPersonIds[0]);
            Assertions.assertEquals(results.size(), 1);
            Report report = results.get(0);
            Assertions.assertEquals(report.getCam(), cams[1]);
            Assertions.assertTrue(report.getId().equals(seenPersonIds[0]));
        });
    }

    @Test
    public void trackMatchCarNoPattern() {
        Assertions.assertDoesNotThrow(() -> {
            List<Report> results = siloFrontend.trackMatch(Observation.ObservationType.CAR, seenCarIds[0]);
            Assertions.assertEquals(results.size(), 1);
            Report report = results.get(0);
            Assertions.assertEquals(report.getCam(), cams[1]);
            Assertions.assertTrue(report.getId().equals(seenCarIds[0]));
        });
    }

    @AfterAll
    public static void tearDown() {
        try {
            siloFrontend.ctrlClear();
        } catch(FrontendException | ZKNamingException e) {
            e.printStackTrace();
        }
    }
}
