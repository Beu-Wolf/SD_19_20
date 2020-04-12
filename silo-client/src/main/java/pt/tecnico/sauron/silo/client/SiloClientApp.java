package pt.tecnico.sauron.silo.client;


import pt.tecnico.sauron.silo.client.exceptions.FrontendException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class SiloClientApp {
	
	public static void main(String[] args) {
		System.out.println(SiloClientApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length < 3) {
			System.out.println("Argument(s) missing!");
			System.out.printf("Usage: java %s zooHost zooPort%n", SiloClientApp.class.getName());
			return;
		}

		final String zooHost = args[0];
		final String zooPort = args[1];
		final String serverPath = args[2];

		try {
			SiloFrontend siloFrontend = new SiloFrontend(zooHost, zooPort, serverPath);
			String sentence = "friend";
			String response = siloFrontend.ctrlPing(sentence);
			System.out.println(response);
			siloFrontend.shutdown();
		} catch (FrontendException | ZKNamingException e) {
			System.err.println("Caught exception with description: " + e.getMessage());
		}
	}


	
}
