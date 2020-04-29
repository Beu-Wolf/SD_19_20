package pt.tecnico.sauron.spotter;

import io.grpc.StatusRuntimeException;
import pt.sauron.silo.contract.domain.exceptions.EmptyCameraNameException;
import pt.sauron.silo.contract.domain.exceptions.InvalidCameraNameException;
import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.sauron.silo.contract.domain.Cam;
import pt.sauron.silo.contract.domain.Coords;
import pt.tecnico.sauron.silo.client.domain.Observation;
import pt.tecnico.sauron.silo.client.domain.Report;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Spotter {
    private SiloFrontend siloFrontend;

    // commands
    private static final String HELP = "^help$";
    private static final String CLEAR = "^clear$";
    private static final String PING = "^ping (.*)$";
    private static final String INIT_CAMS = "^init cams$";
    private static final String INIT_OBS = "^init obs$";
    private static final String SPOT_CAR = "^spot car (\\w+)$";
    private static final String SPOT_PERSON = "^spot person (\\w+)$";
    private static final String SPOT_CAR_PARTIAL = "^spot car ([\\w*]+)$";
    private static final String SPOT_PERSON_PARTIAL = "^spot person ([\\w*]+)$";
    private static final String TRACE_CAR = "^trail car (\\w+)$";
    private static final String TRACE_PERSON = "^trail person (\\w+)$";
    private static final String EXIT = "^exit$";
    private static final String DONE = "^[Dd]one$";

    //init commands
    private static final String CAMS_TO_LOAD = "^(\\w+),([\\d.]+),([\\d.-]+)$";
    private static final String OBS_TO_LOAD_CAR = "^(\\w+),car,(\\w+)$";
    private static final String OBS_TO_LOAD_PERSON = "^(\\w+),person,(\\w+)$";

    // patterns
    private final Pattern pingPattern = Pattern.compile(PING);
    private final Pattern spotCar = Pattern.compile(SPOT_CAR);
    private final Pattern spotPerson = Pattern.compile(SPOT_PERSON);
    private final Pattern spotCarPartial = Pattern.compile(SPOT_CAR_PARTIAL);
    private final Pattern spotPersonPartial = Pattern.compile(SPOT_PERSON_PARTIAL);
    private final Pattern traceCar = Pattern.compile(TRACE_CAR);
    private final Pattern tracePerson = Pattern.compile(TRACE_PERSON);

    //init cameras
    private final Pattern camsToLoad = Pattern.compile(CAMS_TO_LOAD);
    private final Pattern obsToLoadCar = Pattern.compile(OBS_TO_LOAD_CAR);
    private final Pattern obsToLoadPerson = Pattern.compile(OBS_TO_LOAD_PERSON);


    public Spotter(SiloFrontend siloFrontend) {
        this.siloFrontend = siloFrontend;
    }

    public void begin() {
        System.out.println("Spotter started, write 'exit' to quit and 'help' for a list of commands");
        Scanner scanner = new Scanner(System.in);
        List<Report> reportList;
         try {
             while (true) {
                 System.out.print("> ");
                 try {
                     String command = scanner.nextLine().trim();

                     if (Pattern.matches(EXIT, command)) {
                         break;
                     } else if (Pattern.matches(HELP, command)) {
                         showHelp();
                     } else if (Pattern.matches(CLEAR, command)) {
                         siloFrontend.ctrlClear();
                     } else if (Pattern.matches(PING, command)) {
                         String message = getGroupFromPattern(command, pingPattern, 1);
                         System.out.println(siloFrontend.ctrlPing(message));

                     } else if (Pattern.matches(INIT_CAMS, command)) {
                         initCameras(scanner);
                     } else if (Pattern.matches(INIT_OBS, command)) {
                         initObs(scanner);
                     } else if (Pattern.matches(SPOT_CAR, command)) {
                         String id = getGroupFromPattern(command, spotCar, 1);
                         Report report = siloFrontend.track(Observation.ObservationType.CAR, id);
                         System.out.println(report.toString());
                     } else if (Pattern.matches(SPOT_PERSON, command)) {
                         String id = getGroupFromPattern(command, spotPerson, 1);
                         Report report = siloFrontend.track(Observation.ObservationType.PERSON, id);
                         System.out.println(report.toString());
                     } else if (Pattern.matches(SPOT_CAR_PARTIAL, command)) {
                         String id = getGroupFromPattern(command, spotCarPartial, 1);
                         reportList = siloFrontend.trackMatch(Observation.ObservationType.CAR, id);
                         showReports(reportList, true);
                     } else if (Pattern.matches(SPOT_PERSON_PARTIAL, command)) {
                         String id = getGroupFromPattern(command, spotPersonPartial, 1);
                         reportList = siloFrontend.trackMatch(Observation.ObservationType.PERSON, id);
                         showReports(reportList, true);
                     } else if (Pattern.matches(TRACE_CAR, command)) {
                         String id = getGroupFromPattern(command, traceCar, 1);
                         reportList = siloFrontend.trace(Observation.ObservationType.CAR, id);
                         showReports(reportList, false);
                     } else if (Pattern.matches(TRACE_PERSON, command)) {
                         String id = getGroupFromPattern(command, tracePerson, 1);
                         reportList = siloFrontend.trace(Observation.ObservationType.PERSON, id);
                         showReports(reportList, false);
                     } else {
                         System.out.println("Unrecognized command, try again");
                     }

                 } catch (StatusRuntimeException e) {
                     System.err.println(e.getStatus().getDescription());
                 } catch (FrontendException e) {
                     System.err.println(e.getMessage());
                 } catch (ZKNamingException e) {
                     System.err.println("Could not find server in given path. Make sure the server is up and running.");
                 }

             }
             scanner.close();
         } catch (NoSuchElementException e) {
             System.err.println("Reached enf of input. Exiting...");
         }
    }

    private String getGroupFromPattern(String command, Pattern pattern, int index) {
        Matcher m = pattern.matcher(command);
        m.find();
        return m.group(index);
    }

    private void showReports(List<Report> reportList, boolean orderId) {
        if(orderId) {
            Collections.sort(reportList);
        }
        for(Report report : reportList) {
            System.out.println(report.toString());
        }
    }

    private void showHelp() {
        System.out.print("Spotter client:\n" +
                "help: this screen\n" +
                "init cams: enter an interactive mode to upload cameras to server\n" +
                "init obs: enter an interactive mode to upload observations to server\n" +
                "ping [string]: pings server\n" +
                "clear: clears all cameras and observations of the server\n" +
                "spot [car|person] [id]: spot a car ou a person with a given id (partial or not)\n" +
                "trail [car|person] [id]: find all observations of person or car, must use a valid id\n");
    }

    private void initCameras(Scanner scanner) throws FrontendException {
        System.out.println("Insert cameras: name,latitude,longitude . done when finished");
        LinkedList<Cam> listCams = new LinkedList<>();
        while(true) {
            System.out.print("initCams $ ");

                String command = scanner.nextLine().trim();
                if(Pattern.matches(DONE, command)) {
                    if(!listCams.isEmpty())
                        siloFrontend.ctrlInitCams(listCams);
                    break;
                } else if (Pattern.matches(CAMS_TO_LOAD, command)) {
                    try {
                        String camName = getGroupFromPattern(command, camsToLoad, 1);
                        double lat = Double.parseDouble(getGroupFromPattern(command, camsToLoad, 2));
                        double lon = Double.parseDouble(getGroupFromPattern(command, camsToLoad, 3));
                        Cam cam = new Cam(camName, new Coords(lat, lon));
                        listCams.add(cam);
                    } catch (EmptyCameraNameException
                            |InvalidCameraNameException e) {
                        System.err.println(e.getMessage());
                    }

                } else {
                    System.out.println("Unrecognized command, try again");
                }
        }
    }

    private void initObs(Scanner scanner) throws ZKNamingException, FrontendException {
        System.out.println("Insert observations: cameraName,type,id . done when finished");
        LinkedList<Report> listReports = new LinkedList<>();
        while(true) {
            System.out.print("initObservations $ ");

            String command = scanner.nextLine().trim();
            if(Pattern.matches(DONE, command)) {
                if(!listReports.isEmpty())
                    siloFrontend.ctrlInitObservations(listReports);
                break;
            } else if (Pattern.matches(OBS_TO_LOAD_CAR, command)) {

                try {
                    String camName = getGroupFromPattern(command, obsToLoadCar, 1);
                    Coords coords = siloFrontend.camInfo(camName);
                    Cam cam = new Cam(camName, coords);
                    String id = getGroupFromPattern(command, obsToLoadCar, 2);
                    Observation obs = new Observation(Observation.ObservationType.CAR, id);
                    Report report = new Report(obs, cam, Instant.now());
                    listReports.add(report);
                } catch (EmptyCameraNameException
                        |InvalidCameraNameException e) {
                    System.err.println(e.getMessage());
                }

            } else if (Pattern.matches(OBS_TO_LOAD_PERSON, command)) {

                try {
                    String camName = getGroupFromPattern(command, obsToLoadPerson, 1);
                    Coords coords = siloFrontend.camInfo(camName);
                    Cam cam = new Cam(camName, coords);
                    String id = getGroupFromPattern(command, obsToLoadPerson, 2);
                    Observation obs = new Observation(Observation.ObservationType.PERSON, id);
                    Report report = new Report(obs, cam, Instant.now());
                    listReports.add(report);
                } catch (EmptyCameraNameException
                        |InvalidCameraNameException e) {
                    System.err.println(e.getMessage());
                }

            } else {
                System.out.println("Unrecognized command, try again");
            }
        }
    }
}
