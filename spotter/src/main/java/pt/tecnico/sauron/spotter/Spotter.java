package pt.tecnico.sauron.spotter;

import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;
import pt.tecnico.sauron.silo.client.dto.ReportDto;
import pt.tecnico.sauron.silo.client.exceptions.QueryException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Spotter {
    private SiloFrontend siloFrontend;
    List<ReportDto> reportOrderedList = new LinkedList<>();

    // commands
    private static final String SPOT_CAR = "^spot car (\\w+)$";
    private static final String SPOT_PERSON = "^spot person (\\w+)$";
    private static final String SPOT_CAR_PARTIAL = "^spot car ([\\w*]+)$";
    private static final String SPOT_PERSON_PARTIAL = "^spot person ([\\w*]+)$";
    private static final String TRACE_CAR = "^trail car (\\w+)$";
    private static final String TRACE_PERSON = "^trail person (\\w+)$";
    private static final String EXIT = "^exit$";

    // patterns
    private final Pattern spotCar = Pattern.compile(SPOT_CAR);
    private final Pattern spotPerson = Pattern.compile(SPOT_PERSON);
    private final Pattern spotCarPartial = Pattern.compile(SPOT_CAR_PARTIAL);
    private final Pattern spotPersonPartial = Pattern.compile(SPOT_PERSON_PARTIAL);
    private final Pattern traceCar = Pattern.compile(TRACE_CAR);
    private final Pattern tracePerson = Pattern.compile(TRACE_PERSON);


    public Spotter(SiloFrontend siloFrontend) {
        this.siloFrontend = siloFrontend;
    }

    public void begin() {
        System.out.println("Spotter started, write 'exit' to quit");
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.print("> ");
            try {
                String command = scanner.nextLine().trim();

                if(Pattern.matches(EXIT, command)) {
                    break;
                } else if (Pattern.matches(SPOT_CAR, command)) {
                    String id = getIdfromPattern(command, spotCar, 1);
                    ReportDto reportDto = siloFrontend.track(ObservationDto.ObservationType.CAR, id);
                    System.out.println(reportDto.toString());
                } else if (Pattern.matches(SPOT_PERSON, command)) {
                    String id = getIdfromPattern(command, spotPerson, 1);
                    ReportDto reportDto = siloFrontend.track(ObservationDto.ObservationType.PERSON, id);
                    System.out.println(reportDto);
                } else if (Pattern.matches(SPOT_CAR_PARTIAL, command)) {
                    String id = getIdfromPattern(command, spotCarPartial, 1);
                    Iterator<ReportDto> it = siloFrontend.trackMatch(ObservationDto.ObservationType.CAR, id);
                    showReports(it, true);
                } else if (Pattern.matches(SPOT_PERSON_PARTIAL, command)) {
                    String id = getIdfromPattern(command, spotPersonPartial, 1);
                    Iterator<ReportDto> it = siloFrontend.trackMatch(ObservationDto.ObservationType.PERSON, id);
                    showReports(it, true);
                } else if (Pattern.matches(TRACE_CAR, command)) {
                    String id = getIdfromPattern(command, traceCar, 1);
                    Iterator<ReportDto> it = siloFrontend.trace(ObservationDto.ObservationType.CAR, id);
                    showReports(it, false);
                } else if (Pattern.matches(TRACE_PERSON, command)) {
                    String id = getIdfromPattern(command, tracePerson, 1);
                    Iterator<ReportDto> it = siloFrontend.trace(ObservationDto.ObservationType.PERSON, id);
                    showReports(it, false);
                } else {
                    System.out.println("Unrecognized command, try again");
                }

            }catch (StatusRuntimeException e) {
                System.out.println(e.getStatus().getDescription());
            } catch (QueryException e) {
                System.out.println(e.getMessage());
            }

        }
        scanner.close();
    }

    private String getIdfromPattern(String command, Pattern pattern, int index) {
        Matcher m = pattern.matcher(command);
        m.find();
        return m.group(index);
    }

    private void showReports(Iterator<ReportDto> it, boolean orderId) {
        if(orderId) {
            it.forEachRemaining(reportOrderedList::add);
            Collections.sort(reportOrderedList);
            for(ReportDto reportDto: reportOrderedList) {
                System.out.println(reportDto.toString());
            }
        }
        while(it.hasNext()) {
            System.out.println(it.next().toString());
        }
    }
}
