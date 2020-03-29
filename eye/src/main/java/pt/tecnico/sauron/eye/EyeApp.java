package pt.tecnico.sauron.eye;

import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.client.dto.CamDto;
import pt.tecnico.sauron.silo.client.dto.ObservationDto;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EyeApp {
	private static CamDto cam;
	private static List<ObservationDto> observationBuffer;


	// command formats
	private final static String COMMENT = "^#.*$";
	private final static String SLEEP = "^zzz,(\\d+)$";
	private final static String OBSERVATION = "^(\\w+),(\\w+)$";

	// patterns
	private final static Pattern commentPattern     = Pattern.compile(COMMENT);
	private final static Pattern sleepPattern       = Pattern.compile(SLEEP);
	private final static Pattern observationPattern = Pattern.compile(OBSERVATION);

	public static void main(String[] args) {
		System.out.println(EyeApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if(args.length < 5) {
			System.out.println("Argument(s) missing!");
			System.out.printf("Usage: java %s host port camName latitude longitude%n", EyeApp.class.getName());
			return;
		}

		String host = args[0];
		int port = Integer.parseInt(args[1]);
		SiloFrontend siloFrontend = new SiloFrontend(host, port);

		// register cam
		final String name = args[2];
		final double lat = Double.parseDouble(args[3]);
		final double lon = Double.parseDouble(args[4]);
		cam = new CamDto(name, lat, lon);
		siloFrontend.camJoin(cam);

		// init buffer
		observationBuffer = new LinkedList<>();

		interactive(siloFrontend);
	}

	private static void interactive(SiloFrontend siloFrontend) {
		try {
			Scanner scanner = new Scanner(System.in);
			String line;
			while ((line = scanner.nextLine().trim()) != null) {

				if(line.isEmpty()) {
					sendObservations();

				} else if(Pattern.matches(COMMENT, line)) {
					continue;

				} else if(Pattern.matches(SLEEP, line)) {
					Matcher m = sleepPattern.matcher(line);
					m.find();
					int sleepAmt = Integer.parseInt(m.group(1));

				} else if(Pattern.matches(OBSERVATION, line)) {
					Matcher m = observationPattern.matcher(line);
					m.find();
					registerObservation(m.group(1), m.group(2));
					
				} else {
					System.out.println("Input not recognized");
				}
			}

			scanner.close();
		} catch (NoSuchElementException e) {
			// no more lines in input
			sendObservations();
		}
	}

	private static void registerObservation(String type, String id) {
		System.out.println("[DBG] Observed a " + type + " with id " + id);
		ObservationDto.ObservationType observationType;
		switch (type) {
			case "person":
				observationType = ObservationDto.ObservationType.PERSON;
				break;
			case "car":
				observationType = ObservationDto.ObservationType.CAR;
				break;
			default:
				observationType = ObservationDto.ObservationType.UNSPEC;
				break;
		}

		ObservationDto observation = new ObservationDto(observationType, id);
		observationBuffer.add(observation);
	}

	private static void sendObservations() {}
}
