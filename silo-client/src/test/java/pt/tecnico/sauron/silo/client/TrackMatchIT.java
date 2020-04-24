package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.domain.FrontendCam;
import pt.tecnico.sauron.silo.client.domain.FrontendObservation;
import pt.tecnico.sauron.silo.client.domain.FrontendReport;
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

    private static final FrontendCam[] cams = {
            new FrontendCam("First", 0, 0),
            new FrontendCam("Second", 1, 1)
    };

    @BeforeAll
    public static void setupTrackMatch () {
        Instant instant = Instant.now();

        // old observations by cams[0]
        List<FrontendReport> reports = new LinkedList<>();
        for(String id : seenPersonIds) {
            reports.add(new FrontendReport(
                    new FrontendObservation(FrontendObservation.ObservationType.PERSON, id),
                    cams[0],
                    instant.minus(1, DAYS)));
        }
        for(String id : seenCarIds) {
            reports.add(new FrontendReport(
                    new FrontendObservation(FrontendObservation.ObservationType.CAR, id),
                    cams[0],
                    instant.minus(1, DAYS)));
        }

        // most recent observations by cams[1]
        for(String id : seenPersonIds) {
            reports.add(new FrontendReport(
                    new FrontendObservation(FrontendObservation.ObservationType.PERSON, id),
                    cams[1],
                    instant));
        }
        for(String id : seenCarIds) {
            reports.add(new FrontendReport(
                    new FrontendObservation(FrontendObservation.ObservationType.CAR, id),
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
                "Can't handle observation type!",
                Assertions.assertThrows(InvalidArgumentException.class, () -> {
                    this.siloFrontend.track(FrontendObservation.ObservationType.UNSPEC, "1337_5p34k");
                }).getMessage()
        );
    }

    @Test
    public void testInvalidPersonID() {
        for(String invalidId : invalidPersonIDs) {
            Assertions.assertEquals(
                ErrorMessages.OBSERVATION_NOT_FOUND,
                Assertions.assertThrows(NotFoundException.class, () -> {
                    this.siloFrontend.trackMatch(FrontendObservation.ObservationType.PERSON, invalidId);
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
                    this.siloFrontend.trackMatch(FrontendObservation.ObservationType.CAR, invalidId);
                }).getMessage()
            );
        }
    }


    @Test
    public void trackMatchNonExistingCar() {
        Assertions.assertEquals(
            ErrorMessages.OBSERVATION_NOT_FOUND,
            Assertions.assertThrows(NotFoundException.class, () -> {
                siloFrontend.track(FrontendObservation.ObservationType.CAR, notSeenCarId);
            }).getMessage()
        );
    }

    @Test
    public void trackMatchNonExistingPerson() {
        Assertions.assertEquals(
            ErrorMessages.OBSERVATION_NOT_FOUND,
            Assertions.assertThrows(NotFoundException.class, () -> {
                siloFrontend.trackMatch(FrontendObservation.ObservationType.PERSON, notSeenPersonId);
            }).getMessage()
        );
    }

    @Test
    public void trackMatchPeople() {
        Assertions.assertDoesNotThrow(() -> {
            List<FrontendReport> results = siloFrontend.trackMatch(FrontendObservation.ObservationType.PERSON, seenPeoplePatternId);
            Assertions.assertEquals(results.size(), 2);
            for(FrontendReport report : results) {
                Assertions.assertEquals(report.getCam(), cams[1]);
                Assertions.assertTrue(Arrays.asList(seenPersonIds).contains(report.getId()));
            }
        });
    }

    @Test
    public void trackMatchCars() {
        Assertions.assertDoesNotThrow(() -> {
            List<FrontendReport> results = siloFrontend.trackMatch(FrontendObservation.ObservationType.CAR, seenCarsPatternId);
            Assertions.assertEquals(results.size(), 2);
            for(FrontendReport report : results) {
                Assertions.assertEquals(report.getCam(), cams[1]);
                Assertions.assertTrue(Arrays.asList(seenCarIds).contains(report.getId()));
            }
        });
    }

    @Test
    public void trackMatchPersonNoPattern() {
        Assertions.assertDoesNotThrow(() -> {
            List<FrontendReport> results = siloFrontend.trackMatch(FrontendObservation.ObservationType.PERSON, seenPersonIds[0]);
            Assertions.assertEquals(results.size(), 1);
            FrontendReport report = results.get(0);
            Assertions.assertEquals(report.getCam(), cams[1]);
            Assertions.assertTrue(report.getId().equals(seenPersonIds[0]));
        });
    }

    @Test
    public void trackMatchCarNoPattern() {
        Assertions.assertDoesNotThrow(() -> {
            List<FrontendReport> results = siloFrontend.trackMatch(FrontendObservation.ObservationType.CAR, seenCarIds[0]);
            Assertions.assertEquals(results.size(), 1);
            FrontendReport report = results.get(0);
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
