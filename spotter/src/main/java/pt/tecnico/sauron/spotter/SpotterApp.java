package pt.tecnico.sauron.spotter;


import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class SpotterApp {
	
	public static void main(String[] args) {
		System.out.println(SpotterApp.class.getSimpleName());
		Integer instance = null;
		
		// receive and print arguments
//		 System.out.printf("Received %d arguments%n", args.length);
//		 for (int i = 0; i < args.length; i++) {
//		 	System.out.printf("arg[%d] = %s%n", i, args[i]);
//		 }

		if (args.length < 2) {
			System.out.println("Arguments missing");
			System.out.printf("Usage: %s zooHost zooPort [instance] %n", Spotter.class.getName());
			return;
		}

		String zooHost = args[0];
		String zooPort = args[1];

		if(args.length >= 3) {
			instance = Integer.parseInt(args[2]);
		}

		SiloFrontend siloFrontend;
		try {
			if (instance != null)
				siloFrontend = new SiloFrontend(zooHost, zooPort, instance);
			else
				siloFrontend = new SiloFrontend(zooHost, zooPort);

			Spotter spotter = new Spotter(siloFrontend);

			spotter.begin();
		} catch (ZKNamingException e) {
			System.out.println("Could not find server in given path. Make sure the server is up and running.");
		} catch (FrontendException e) {
			System.out.println(e.getMessage());
		}
	}
}
