package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;
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

public class TrackIT extends BaseIT {

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

    private static final FrontendCam[] cams = {
            new FrontendCam(camNames[0], 0, 0),
            new FrontendCam(camNames[1], 1, 1)
    };

    @BeforeAll
    public static void setupTrack () {
        Instant instant = Instant.now();

        List<FrontendReport> reports = new LinkedList<>();
        reports.add(new FrontendReport(
                new FrontendObservation(FrontendObservation.ObservationType.CAR, validCarIds[0]),
                cams[1],
                instant.minus(1, DAYS)));
        reports.add(new FrontendReport(
                new FrontendObservation(FrontendObservation.ObservationType.PERSON, validPersonIds[0]),
                cams[1],
                instant.minus(1, DAYS)));

        // last report
        reports.add(new FrontendReport(
                new FrontendObservation(FrontendObservation.ObservationType.CAR, validCarIds[0]),
                cams[0],
                instant));
        reports.add(new FrontendReport(
                new FrontendObservation(FrontendObservation.ObservationType.PERSON, validPersonIds[0]),
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
        Assertions.assertEquals(
            "Can't handle observation type!",
            Assertions.assertThrows(InvalidArgumentException.class, () -> {
                this.siloFrontend.track(FrontendObservation.ObservationType.UNSPEC, "1337_5p34k");
            }).getMessage()
        );
    }

    @Test
    public void testInvalidPersonID() {
        for(String invalidId : invalidPersonIds) {
            Assertions.assertEquals(
                invalidId + ": Person ID must be an unsigned long!",
                Assertions.assertThrows(InvalidArgumentException.class, () -> {
                    this.siloFrontend.track(FrontendObservation.ObservationType.PERSON, invalidId);
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
                    this.siloFrontend.track(FrontendObservation.ObservationType.CAR, invalidId);
                }).getMessage()
            );
        }
    }


    @Test
    public void trackNonExistingCar() {
        Assertions.assertEquals(
                ErrorMessages.OBSERVATION_NOT_FOUND,
                Assertions.assertThrows(NotFoundException.class, () -> {
                    siloFrontend.track(FrontendObservation.ObservationType.CAR, validCarIds[1]);
                }).getMessage()
        );
    }

    @Test
    public void trackNonExistingPerson() {
        Assertions.assertEquals(
                ErrorMessages.OBSERVATION_NOT_FOUND,
                Assertions.assertThrows(NotFoundException.class, () -> {
                    siloFrontend.track(FrontendObservation.ObservationType.PERSON, validPersonIds[1]);
                }).getMessage()
        );
    }

    @Test
    public void trackExistingPerson() {
        Assertions.assertDoesNotThrow(() -> {
            FrontendReport response = siloFrontend.track(FrontendObservation.ObservationType.PERSON, validPersonIds[0]);
            Assertions.assertEquals(response.getCam(), cams[0]);
            Assertions.assertEquals(response.getId(), validPersonIds[0]);
        });
    }

    @Test
    public void trackExistingCar() {
        Assertions.assertDoesNotThrow(() -> {
            FrontendReport response = siloFrontend.track(FrontendObservation.ObservationType.CAR, validCarIds[0]);
            Assertions.assertEquals(response.getCam(), cams[0]);
            Assertions.assertEquals(response.getId(), validCarIds[0]);
        });
    }

    @AfterAll
    public static void tearDown() {
        try {
            siloFrontend.ctrlClear();
        } catch( ZKNamingException | FrontendException e) {
            e.printStackTrace();
        }
    }
}
