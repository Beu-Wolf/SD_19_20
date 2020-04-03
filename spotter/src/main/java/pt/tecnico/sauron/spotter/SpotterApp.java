package pt.tecnico.sauron.spotter;


import pt.tecnico.sauron.silo.client.SiloFrontend;

public class SpotterApp {
	
	public static void main(String[] args) {
		System.out.println(SpotterApp.class.getSimpleName());
		
		// receive and print arguments
		// System.out.printf("Received %d arguments%n", args.length);
		// for (int i = 0; i < args.length; i++) {
		// 	System.out.printf("arg[%d] = %s%n", i, args[i]);
		// }

		if (args.length != 2) {
			System.out.println("Arguments missing");
			System.out.printf("Usage: %s host port%n", Spotter.class.getName());
			return;
		}

		String host = args[0];
		int port = Integer.parseInt(args[1]);

		SiloFrontend siloFrontend = new SiloFrontend(host, port);

		Spotter spotter = new Spotter(siloFrontend);

		spotter.begin();


	}

}
