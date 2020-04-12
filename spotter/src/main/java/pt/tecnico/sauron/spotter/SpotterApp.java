package pt.tecnico.sauron.spotter;


import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;

public class SpotterApp {
	
	public static void main(String[] args) {
		System.out.println(SpotterApp.class.getSimpleName());
		
		// receive and print arguments
		// System.out.printf("Received %d arguments%n", args.length);
		// for (int i = 0; i < args.length; i++) {
		// 	System.out.printf("arg[%d] = %s%n", i, args[i]);
		// }

		if (args.length != 3) {
			System.out.println("Arguments missing");
			System.out.printf("Usage: %s host port%n", Spotter.class.getName());
			return;
		}

		String host = args[0];
		String port = args[1];
		String path = args[2];
		try {
			SiloFrontend siloFrontend = new SiloFrontend(host, port, path);

			Spotter spotter = new Spotter(siloFrontend);

			spotter.begin();
		} catch (FrontendException e) {
			System.err.println(e.getMessage());
		}


	}

}
