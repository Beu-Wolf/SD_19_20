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

		try {
			Eye eye = new Eye(siloFrontend, name, lat, lon);
			eye.interactive();
		} catch(FrontendException e) {
			System.err.println(e.getMessage());
		}

	}
}
