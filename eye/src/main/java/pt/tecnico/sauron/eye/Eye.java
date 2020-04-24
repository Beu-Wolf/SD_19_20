package pt.tecnico.sauron.eye;

import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.exceptions.CameraAlreadyExistsException;
import pt.tecnico.sauron.silo.client.exceptions.CameraRegisterException;
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
    private CamDto cam;
    private List<ObservationDto> observationBuffer = new LinkedList<>();

    public Eye(SiloFrontend siloFrontend, String name, double lat, double lon) throws FrontendException, ZKNamingException {
        this.siloFrontend = siloFrontend;
        this.cam = new CamDto(name, lat, lon);

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
                    Matcher m = carObservationPattern.matcher(line);
                    m.find();
                    registerObservation(ObservationDto.ObservationType.CAR, m.group(1));

                } else if(Pattern.matches(PERSON_OBSERVATION, line)) {
                    Matcher m = personObservationPattern.matcher(line);
                    m.find();
                    registerObservation(ObservationDto.ObservationType.PERSON, m.group(1));

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

    private void registerObservation(ObservationDto.ObservationType type, String id) {
        ObservationDto observation = new ObservationDto(type, id);
        observationBuffer.add(observation);
    }

    private void sendObservations() {
        if(observationBuffer.size() > 0) {
            try {
                this.siloFrontend.report(this.cam.getName(), observationBuffer);
                System.out.println("Successfully reported observations!");
            } catch (FrontendException e) {
                System.err.println("Could not add observations: " + e.getMessage());
            } catch (ZKNamingException e) {
                System.err.println("Could not find server in given path. Make sure the server is up and running.");
            }
        }
    }
}
