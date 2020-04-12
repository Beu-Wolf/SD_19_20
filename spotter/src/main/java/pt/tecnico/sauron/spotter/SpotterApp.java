package pt.tecnico.sauron.spotter;


import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class SpotterApp {
	
	public static void main(String[] args) {
		System.out.println(SpotterApp.class.getSimpleName());
		
		// receive and print arguments
//		 System.out.printf("Received %d arguments%n", args.length);
//		 for (int i = 0; i < args.length; i++) {
//		 	System.out.printf("arg[%d] = %s%n", i, args[i]);
//		 }

		if (args.length < 3) {
			System.out.println("Arguments missing");
			System.out.printf("Usage: %s zooHost zooPort serverPath%n", Spotter.class.getName());
			return;
		}

		String zooHost = args[0];
		String zooPort = args[1];
		String serverPath = args[2];

		SiloFrontend siloFrontend;
		try {
			siloFrontend = new SiloFrontend(zooHost, zooPort, serverPath);
			Spotter spotter = new Spotter(siloFrontend);

			spotter.begin();
		} catch (ZKNamingException e) {
			e.printStackTrace();
		}
	}

}
