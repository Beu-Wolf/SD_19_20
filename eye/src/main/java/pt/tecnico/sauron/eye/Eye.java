package pt.tecnico.sauron.eye;

import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.client.domain.FrontendCam;
import pt.tecnico.sauron.silo.client.domain.FrontendObservation;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Eye {
    // command formats
    private final String COMMENT = "^#.*$";
    private final String SLEEP = "^zzz,(\\d+)$";
    private final String CAR_OBSERVATION = "^car,(\\w+)$";
    private final String PERSON_OBSERVATION = "^person,(\\w+)$";

    // patterns
    private final Pattern sleepPattern             = Pattern.compile(SLEEP);
    private final Pattern carObservationPattern    = Pattern.compile(CAR_OBSERVATION);
    private final Pattern personObservationPattern = Pattern.compile(PERSON_OBSERVATION);


    // attributes
    private SiloFrontend siloFrontend;
    private FrontendCam cam;
    private List<FrontendObservation> observationBuffer = new LinkedList<>();

    public Eye(SiloFrontend siloFrontend, String name, double lat, double lon) throws FrontendException, ZKNamingException {
        this.siloFrontend = siloFrontend;
        this.cam = new FrontendCam(name, lat, lon);

        this.siloFrontend.camJoin(this.cam);
        System.out.println("Registered Successfully!");
    }

    public void interactive() {
        try {
            Scanner scanner = new Scanner(System.in);
            String line;
            while ((line = scanner.nextLine().trim()) != null) {

                if(line.isEmpty()) {
                    sendObservations();
                    observationBuffer.clear();
                } else if(Pattern.matches(CAR_OBSERVATION, line)) {
                    // car observation
                    Matcher m = carObservationPattern.matcher(line);
                    m.find();
                    registerObservation(FrontendObservation.ObservationType.CAR, m.group(1));

                } else if(Pattern.matches(PERSON_OBSERVATION, line)) {
                    // person observation
                    Matcher m = personObservationPattern.matcher(line);
                    m.find();
                    registerObservation(FrontendObservation.ObservationType.PERSON, m.group(1));

                } else if(Pattern.matches(COMMENT, line)) {
                    // comment: ignore
                    continue;

                } else if(Pattern.matches(SLEEP, line)) {
                    // sleep
                    Matcher m = sleepPattern.matcher(line);
                    m.find();
                    int sleepAmt = Integer.parseInt(m.group(1));
                    Thread.sleep(sleepAmt);

                } else {
                    // unknown command
                    System.err.println("Input not recognized");
                }
            }

            // after reading all the input, close the scanner
            scanner.close();
        } catch (NoSuchElementException e) {
            // no more lines in input
            System.out.println("Reached end of input");
            sendObservations();
            System.exit(0);
        } catch (InterruptedException e) {
            // interrupt while sleeping
            System.err.printf("Got interrupted while sleeping. %s%nExiting.", e.getMessage());
        }
    }

    private void registerObservation(FrontendObservation.ObservationType type, String id) {
        // add observation to send
        FrontendObservation observation = new FrontendObservation(type, id);
        observationBuffer.add(observation);
    }

    private void sendObservations() {
        if(observationBuffer.size() > 0) {
            try {
                // send observations
                int numAcked = this.siloFrontend.report(this.cam, observationBuffer);
                System.out.printf("Successfully reported %d observations!%n", numAcked);
            } catch (FrontendException e) {
                System.err.println("Could not add all observations:\n" + e.getMessage());
            } catch (ZKNamingException e) {
                System.err.println("Could not find server in given path. Make sure the server is up and running.");
            }
        }
    }
}
