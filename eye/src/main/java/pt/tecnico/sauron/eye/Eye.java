package pt.tecnico.sauron.eye;

import pt.sauron.silo.contract.domain.Cam;
import pt.sauron.silo.contract.domain.Coords;
import pt.sauron.silo.contract.domain.exceptions.EmptyCameraNameException;
import pt.sauron.silo.contract.domain.exceptions.InvalidCameraNameException;
import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.client.domain.Observation;
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
    private Cam cam;
    private List<Observation> observationBuffer = new LinkedList<>();

    public Eye(SiloFrontend siloFrontend, String name, double lat, double lon) throws FrontendException, ZKNamingException {
        try {
            this.siloFrontend = siloFrontend;
            this.cam = new Cam(name, new Coords(lat, lon));

            this.siloFrontend.camJoin(this.cam);
            System.out.println("Registered Successfully!");
        } catch (InvalidCameraNameException
                |EmptyCameraNameException e) {
            System.err.println("Invalid camera arguments");
        }
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
                    Matcher m = carObservationPattern.matcher(line);
                    m.find();
                    registerObservation(Observation.ObservationType.CAR, m.group(1));

                } else if(Pattern.matches(PERSON_OBSERVATION, line)) {
                    Matcher m = personObservationPattern.matcher(line);
                    m.find();
                    registerObservation(Observation.ObservationType.PERSON, m.group(1));

                } else if(Pattern.matches(COMMENT, line)) {
                    continue;

                } else if(Pattern.matches(SLEEP, line)) {
                    Matcher m = sleepPattern.matcher(line);
                    m.find();
                    int sleepAmt = Integer.parseInt(m.group(1));
                    Thread.sleep(sleepAmt);

                } else {
                    System.err.println("Input not recognized");
                }
            }

            scanner.close();
        } catch (NoSuchElementException e) {
            // no more lines in input
            System.out.println("Reached end of input");
            sendObservations();
            System.exit(0);
        } catch (InterruptedException e) {
            System.err.printf("Got interrupted while sleeping. %s%nExiting.", e.getMessage());
        }
    }

    private void registerObservation(Observation.ObservationType type, String id) {
        Observation observation = new Observation(type, id);
        observationBuffer.add(observation);
    }

    private void sendObservations() {
        if(observationBuffer.size() > 0) {
            try {
                int numAcked = this.siloFrontend.report(this.cam.getName(), observationBuffer);
                System.out.printf("Successfully reported %d observations!%n", numAcked);
            } catch (FrontendException e) {
                System.err.println("Could not add all observations:\n" + e.getMessage());
            }
        }
    }
}
