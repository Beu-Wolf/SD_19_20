package pt.tecnico.sauron.spotter;

import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.client.domain.FrontendCam;
import pt.tecnico.sauron.silo.client.domain.FrontendCoords;
import pt.tecnico.sauron.silo.client.domain.FrontendObservation;
import pt.tecnico.sauron.silo.client.domain.FrontendReport;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Spotter {
    private SiloFrontend siloFrontend;

    // command formats
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

    private final Pattern camsToLoad = Pattern.compile(CAMS_TO_LOAD);
    private final Pattern obsToLoadCar = Pattern.compile(OBS_TO_LOAD_CAR);
    private final Pattern obsToLoadPerson = Pattern.compile(OBS_TO_LOAD_PERSON);


    public Spotter(SiloFrontend siloFrontend) {
        this.siloFrontend = siloFrontend;
    }

    public void begin() {
        System.out.println("Spotter started, write 'exit' to quit and 'help' for a list of commands");
        Scanner scanner = new Scanner(System.in);
        List<FrontendReport> reportList;
         try {
             while (true) {
                 // prompt
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
                         FrontendReport frontendReport = siloFrontend.track(FrontendObservation.ObservationType.CAR, id);
                         System.out.println(frontendReport.toString());
                     } else if (Pattern.matches(SPOT_PERSON, command)) {
                         String id = getGroupFromPattern(command, spotPerson, 1);
                         FrontendReport frontendReport = siloFrontend.track(FrontendObservation.ObservationType.PERSON, id);
                         System.out.println(frontendReport.toString());
                     } else if (Pattern.matches(SPOT_CAR_PARTIAL, command)) {
                         String id = getGroupFromPattern(command, spotCarPartial, 1);
                         reportList = siloFrontend.trackMatch(FrontendObservation.ObservationType.CAR, id);
                         showReports(reportList, true);
                     } else if (Pattern.matches(SPOT_PERSON_PARTIAL, command)) {
                         String id = getGroupFromPattern(command, spotPersonPartial, 1);
                         reportList = siloFrontend.trackMatch(FrontendObservation.ObservationType.PERSON, id);
                         showReports(reportList, true);
                     } else if (Pattern.matches(TRACE_CAR, command)) {
                         String id = getGroupFromPattern(command, traceCar, 1);
                         reportList = siloFrontend.trace(FrontendObservation.ObservationType.CAR, id);
                         showReports(reportList, false);
                     } else if (Pattern.matches(TRACE_PERSON, command)) {
                         String id = getGroupFromPattern(command, tracePerson, 1);
                         reportList = siloFrontend.trace(FrontendObservation.ObservationType.PERSON, id);
                         showReports(reportList, false);
                     } else {
                         System.out.println("Unrecognized command, try again");
                     }

                 } catch (StatusRuntimeException e) {
                     System.err.println(e.getStatus().getDescription());
                 } catch (FrontendException e) {
                     System.err.println(e.getMessage());
                 } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                     System.err.println(e.getMessage());
                 } catch (ZKNamingException e) {
                     System.err.println("Could not find server in given path. Make sure the server is up and running.");
                 }

             }
             // after reading all the input, close the scanner
             scanner.close();
         } catch (NoSuchElementException e) {
             System.err.println("Reached enf of input. Exiting...");
         }
    }

    // parse pattern
    private String getGroupFromPattern(String command, Pattern pattern, int index) {
        Matcher m = pattern.matcher(command);
        m.find();
        return m.group(index);
    }

    // print reports
    private void showReports(List<FrontendReport> reportList, boolean orderId) {
        if(orderId) {
            Collections.sort(reportList);
        }
        for(FrontendReport frontendReport : reportList) {
            System.out.println(frontendReport.toString());
        }
    }

    // print help menu
    private void showHelp() {
        System.out.print("Spotter client:\n" +
                "help                    | Show this screen\n" +
                "\n" +
                "ping [string]           | Ping server\n" +
                "init cams               | Enter an interactive mode to upload cameras to server\n" +
                "init obs                | Enter an interactive mode to upload observations to server\n" +
                "clear                   | Clear all server cameras and observations\n" +
                "\n" +
                "spot [car|person] [id]  | Spot a car ou a person with a given id (partial or not)\n" +
                "trail [car|person] [id] | Find all observations of person or car, must use a valid id\n" +

        "");
    }

    private void initCameras(Scanner scanner) throws ZKNamingException, FrontendException, InterruptedException {
        System.out.println("Insert cameras: name,latitude,longitude . done when finished");
        LinkedList<FrontendCam> listCams = new LinkedList<>();
        while(true) {
            System.out.print("initCams $ ");

                String command = scanner.nextLine().trim();
                if(Pattern.matches(DONE, command)) {
                    if(!listCams.isEmpty())
                        siloFrontend.ctrlInitCams(listCams);
                    break;
                } else if (Pattern.matches(CAMS_TO_LOAD, command)) {
                    String camName = getGroupFromPattern(command, camsToLoad, 1);
                    double lat = Double.parseDouble(getGroupFromPattern(command, camsToLoad, 2));
                    double lon = Double.parseDouble(getGroupFromPattern(command, camsToLoad, 3));
                    FrontendCam cam = new FrontendCam(camName, lat, lon);
                    listCams.add(cam);
                } else {
                    System.out.println("Unrecognized command, try again");
                }
        }
    }

    private void initObs(Scanner scanner) throws ZKNamingException, FrontendException, InterruptedException {
        System.out.println("Insert observations: cameraName,type,id . done when finished");
        LinkedList<FrontendReport> listReports = new LinkedList<>();
        while(true) {
            System.out.print("initObservations $ ");

            String command = scanner.nextLine().trim();
            if(Pattern.matches(DONE, command)) {
                if(!listReports.isEmpty())
                    siloFrontend.ctrlInitObservations(listReports);
                break;
            } else if (Pattern.matches(OBS_TO_LOAD_CAR, command)) {
                String camName = getGroupFromPattern(command, obsToLoadCar, 1);
                FrontendCoords coords = siloFrontend.camInfo(camName);
                FrontendCam cam = new FrontendCam(camName, coords.getLat(), coords.getLon());
                String id = getGroupFromPattern(command, obsToLoadCar, 2);
                FrontendObservation obs = new FrontendObservation(FrontendObservation.ObservationType.CAR, id);
                FrontendReport report = new FrontendReport(obs, cam, Instant.now());
                listReports.add(report);
            } else if (Pattern.matches(OBS_TO_LOAD_PERSON, command)) {
                String camName = getGroupFromPattern(command, obsToLoadPerson, 1);
                FrontendCoords coords = siloFrontend.camInfo(camName);
                FrontendCam cam = new FrontendCam(camName, coords.getLat(), coords.getLon());
                String id = getGroupFromPattern(command, obsToLoadPerson, 2);
                FrontendObservation obs = new FrontendObservation(FrontendObservation.ObservationType.PERSON, id);
                FrontendReport report = new FrontendReport(obs, cam, Instant.now());
                listReports.add(report);
            } else {
                System.out.println("Unrecognized command, try again");
            }
        }
    }
}
