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


    private static Cam[] cams = new Cam[0];

    // handle ctor error
    static {
        try {
            cams = new Cam[]{
                new Cam("First", new Coords(0, 0)),
                new Cam("Second", new Coords(1, 1)),
                new Cam("Third", new Coords(2, 2)),
                new Cam("Fourth", new Coords(3, 3)),
                new Cam("Fifth", new Coords(4, 4))
            };
        } catch (EmptyCameraNameException e) {
            e.printStackTrace();
        } catch (InvalidCameraNameException e) {
            e.printStackTrace();
        }
    }

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


        List<Report> reports = new LinkedList<>();
        Assertions.assertEquals(cams.length, instants.length);
        for(int i = 0; i < cams.length; i++) {
            reports.add(new Report(
                    new Observation(Observation.ObservationType.CAR, validCarIds[0]),
                    cams[i],
                    instants[i]));
            reports.add(new Report(
                    new Observation(Observation.ObservationType.PERSON, validPersonIds[0]),
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
            "Type to observe not supported!",
            Assertions.assertThrows(InvalidArgumentException.class, () -> {
                this.siloFrontend.trace(Observation.ObservationType.UNSPEC, "1337_5p34k");
            }).getMessage()
        );
    }

    @Test
    public void testInvalidPersonID() {
        for(String invalidId : invalidPersonIds) {
            Assertions.assertEquals(
                invalidId + ": Person ID must be an unsigned long!",
                Assertions.assertThrows(InvalidArgumentException.class, () -> {
                    this.siloFrontend.trace(Observation.ObservationType.PERSON, invalidId);
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
                    this.siloFrontend.trace(Observation.ObservationType.CAR, invalidId);
                }).getMessage()
            );
        }
    }


    @Test
    public void traceNonExistingCar() {
        Assertions.assertEquals(
            ErrorMessages.OBSERVATION_NOT_FOUND,
            Assertions.assertThrows(NotFoundException.class, () -> {
                siloFrontend.trace(Observation.ObservationType.CAR, notSeenCarId);
            }).getMessage()
        );
    }

    @Test
    public void traceNonExistingPerson() {
        Assertions.assertEquals(
            ErrorMessages.OBSERVATION_NOT_FOUND,
            Assertions.assertThrows(NotFoundException.class, () -> {
                siloFrontend.trace(Observation.ObservationType.PERSON, notSeenPeronId);
            }).getMessage()
        );
    }

    @Test
    public void traceExistingPerson() {
        Assertions.assertDoesNotThrow(() -> {
            List<Report> response = siloFrontend.trace(Observation.ObservationType.PERSON, validPersonIds[0]);
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
            List<Report> response = siloFrontend.trace(Observation.ObservationType.CAR, validCarIds[0]);
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
