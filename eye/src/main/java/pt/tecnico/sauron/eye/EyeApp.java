package pt.tecnico.sauron.eye;

import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;

public class EyeApp {

	public static void main(String[] args) {
		System.out.println(EyeApp.class.getSimpleName());
		
		// receive and print arguments
		// System.out.printf("Received %d arguments%n", args.length);
		// for (int i = 0; i < args.length; i++) {
		//   System.out.printf("arg[%d] = %s%n", i, args[i]);
		// }

		if(args.length < 5) {
			System.out.println("Argument(s) missing!");
			System.out.printf("Usage: java %s host port path camName latitude longitude%n", EyeApp.class.getName());
			return;
		}

		String host = args[0];
		String port = args[1];
		String path = args[2];

		// register cam
		final String name = args[3];
		final double lat = Double.parseDouble(args[4]);
		final double lon = Double.parseDouble(args[5]);

		try {
			SiloFrontend siloFrontend = new SiloFrontend(host, port, path);
			Eye eye = new Eye(siloFrontend, name, lat, lon);
			eye.interactive();
		} catch(FrontendException e) {
			System.err.println(e.getMessage());
		}

	}
}
