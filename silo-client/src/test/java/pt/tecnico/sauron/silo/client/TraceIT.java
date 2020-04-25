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

public class TraceIT extends BaseIT {

    private static final String[] invalidPersonIds = {
            "-4982374",
            "20SD20",
            "324*343"
    };

    private static final String[] invalidCarIds = {
            "AABBCC",
            "112233",
            "AA11BB22",
            "A11BB2",
            "A1*B2"
    };

    private static final String notSeenPeronId = "222222";
    private static final String[] validPersonIds = {
            "111111",
    };

    private static final String notSeenCarId = "ZZ99ZZ";
    private static final String[] validCarIds = {
            "AA00AA",
    };


    private static final FrontendCam[] cams = {
            new FrontendCam("First", 0, 0),
            new FrontendCam("Second", 1, 1),
            new FrontendCam("Third", 2, 2),
            new FrontendCam("Fourth", 3, 3),
            new FrontendCam("Fifth", 4, 4)
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


        List<FrontendReport> reports = new LinkedList<>();
        Assertions.assertEquals(cams.length, instants.length);
        for(int i = 0; i < cams.length; i++) {
            reports.add(new FrontendReport(
                    new FrontendObservation(FrontendObservation.ObservationType.CAR, validCarIds[0]),
                    cams[i],
                    instants[i]));
            reports.add(new FrontendReport(
                    new FrontendObservation(FrontendObservation.ObservationType.PERSON, validPersonIds[0]),
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
        Assertions.assertEquals(
            "Can't handle observation type!",
            Assertions.assertThrows(InvalidArgumentException.class, () -> {
                this.siloFrontend.trace(FrontendObservation.ObservationType.UNSPEC, "1337_5p34k");
            }).getMessage()
        );
    }

    @Test
    public void testInvalidPersonID() {
        for(String invalidId : invalidPersonIds) {
            Assertions.assertEquals(
                invalidId + ": Person ID must be an unsigned long!",
                Assertions.assertThrows(InvalidArgumentException.class, () -> {
                    this.siloFrontend.trace(FrontendObservation.ObservationType.PERSON, invalidId);
                }).getMessage()
            );
        }
    }

    @Test
    public void testInvalidCarID() {
        for(String invalidId : invalidCarIds) {
            Assertions.assertEquals(
                invalidId + ": Car ID must be a valid portuguese license plate!",
                Assertions.assertThrows(InvalidArgumentException.class, () -> {
                    this.siloFrontend.trace(FrontendObservation.ObservationType.CAR, invalidId);
                }).getMessage()
            );
        }
    }


    @Test
    public void traceNonExistingCar() {
        Assertions.assertEquals(
            ErrorMessages.OBSERVATION_NOT_FOUND,
            Assertions.assertThrows(NotFoundException.class, () -> {
                siloFrontend.trace(FrontendObservation.ObservationType.CAR, notSeenCarId);
            }).getMessage()
        );
    }

    @Test
    public void traceNonExistingPerson() {
        Assertions.assertEquals(
            ErrorMessages.OBSERVATION_NOT_FOUND,
            Assertions.assertThrows(NotFoundException.class, () -> {
                siloFrontend.trace(FrontendObservation.ObservationType.PERSON, notSeenPeronId);
            }).getMessage()
        );
    }

    @Test
    public void traceExistingPerson() {
        Assertions.assertDoesNotThrow(() -> {
            List<FrontendReport> response = siloFrontend.trace(FrontendObservation.ObservationType.PERSON, validPersonIds[0]);
            Assertions.assertEquals(response.size(), cams.length);
            for(int i = 0; i < cams.length; i++) {
                // sorted by new
                Assertions.assertEquals(response.get(i).getCam(), cams[cams.length-i-1]);
                Assertions.assertEquals(response.get(i).getId(), validPersonIds[0]);
            }
        });
    }

    @Test
    public void traceExistingCar() {
        Assertions.assertDoesNotThrow(() -> {
            List<FrontendReport> response = siloFrontend.trace(FrontendObservation.ObservationType.CAR, validCarIds[0]);
            Assertions.assertEquals(response.size(), cams.length);
            for(int i = 0; i < cams.length; i++) {
                // sorted by new
                Assertions.assertEquals(response.get(i).getCam(), cams[cams.length-i-1]);
                Assertions.assertEquals(response.get(i).getId(), validCarIds[0]);
            }
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
