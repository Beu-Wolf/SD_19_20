package pt.tecnico.sauron.silo.client;


import pt.tecnico.sauron.silo.client.exceptions.FrontendException;

public class SiloClientApp {
	
	public static void main(String[] args) {
		System.out.println(SiloClientApp.class.getSimpleName());
		SiloFrontend siloFrontend = null;
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length < 2) {
			System.out.println("Argument(s) missing!");
			System.out.printf("Usage: java %s host port path%n", SiloClientApp.class.getName());
			return;
		}

		final String host = args[0];
		final String port = args[1];
		final String path = args[2];


		try {
			siloFrontend = new SiloFrontend(host, port, path);
			String sentence = "friend";
			String response = siloFrontend.ctrlPing(sentence);
			System.out.println(response);
			siloFrontend.shutdown();
		} catch (FrontendException e) {
			System.err.println("Caught exception with description: " + e.getMessage());
		} finally {
			if(siloFrontend != null)
				siloFrontend.shutdown();
		}


	}


	
}
