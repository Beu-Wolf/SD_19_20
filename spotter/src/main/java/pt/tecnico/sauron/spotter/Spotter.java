package pt.tecnico.sauron.spotter;

import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.CoordsDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.dto.ReportDto;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;

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
        List<ReportDto> reportList;
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
                         ReportDto reportDto = siloFrontend.track(ObservationDto.ObservationType.CAR, id);
                         System.out.println(reportDto.toString());
                     } else if (Pattern.matches(SPOT_PERSON, command)) {
                         String id = getGroupFromPattern(command, spotPerson, 1);
                         ReportDto reportDto = siloFrontend.track(ObservationDto.ObservationType.PERSON, id);
                         System.out.println(reportDto.toString());
                     } else if (Pattern.matches(SPOT_CAR_PARTIAL, command)) {
                         String id = getGroupFromPattern(command, spotCarPartial, 1);
                         reportList = siloFrontend.trackMatch(ObservationDto.ObservationType.CAR, id);
                         showReports(reportList, true);
                     } else if (Pattern.matches(SPOT_PERSON_PARTIAL, command)) {
                         String id = getGroupFromPattern(command, spotPersonPartial, 1);
                         reportList = siloFrontend.trackMatch(ObservationDto.ObservationType.PERSON, id);
                         showReports(reportList, true);
                     } else if (Pattern.matches(TRACE_CAR, command)) {
                         String id = getGroupFromPattern(command, traceCar, 1);
                         reportList = siloFrontend.trace(ObservationDto.ObservationType.CAR, id);
                         showReports(reportList, false);
                     } else if (Pattern.matches(TRACE_PERSON, command)) {
                         String id = getGroupFromPattern(command, tracePerson, 1);
                         reportList = siloFrontend.trace(ObservationDto.ObservationType.PERSON, id);
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

    private void showReports( List<ReportDto> reportList, boolean orderId) {
        if(orderId) {
            Collections.sort(reportList);
        }
        for(ReportDto reportDto: reportList) {
            System.out.println(reportDto.toString());
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

    private void initCameras(Scanner scanner) throws InterruptedException {
        System.out.println("Insert cameras: name,latitude,longitude . done when finished");
        LinkedList<CamDto> listCams = new LinkedList<>();
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
                    CamDto cam = new CamDto(camName, lat, lon);
                    listCams.add(cam);
                } else {
                    System.out.println("Unrecognized command, try again");
                }
        }
    }

    private void initObs(Scanner scanner) throws InterruptedException, FrontendException {
        System.out.println("Insert observations: cameraName,type,id . done when finished");
        LinkedList<ReportDto> listReports = new LinkedList<>();
        while(true) {
            System.out.print("initObservations $ ");

            String command = scanner.nextLine().trim();
            if(Pattern.matches(DONE, command)) {
                if(!listReports.isEmpty())
                    siloFrontend.ctrlInitObservations(listReports);
                break;
            } else if (Pattern.matches(OBS_TO_LOAD_CAR, command)) {
                String camName = getGroupFromPattern(command, obsToLoadCar, 1);
                CoordsDto coords = siloFrontend.camInfo(camName);
                CamDto cam = new CamDto(camName, coords.getLat(), coords.getLon());
                String id = getGroupFromPattern(command, obsToLoadCar, 2);
                ObservationDto obs = new ObservationDto(ObservationDto.ObservationType.CAR, id);
                ReportDto report = new ReportDto(obs, cam, Instant.now());
                listReports.add(report);
            } else if (Pattern.matches(OBS_TO_LOAD_PERSON, command)) {
                String camName = getGroupFromPattern(command, obsToLoadPerson, 1);
                CoordsDto coords = siloFrontend.camInfo(camName);
                CamDto cam = new CamDto(camName, coords.getLat(), coords.getLon());
                String id = getGroupFromPattern(command, obsToLoadPerson, 2);
                ObservationDto obs = new ObservationDto(ObservationDto.ObservationType.PERSON, id);
                ReportDto report = new ReportDto(obs, cam, Instant.now());
                listReports.add(report);
            } else {
                System.out.println("Unrecognized command, try again");
            }
        }
    }
}
