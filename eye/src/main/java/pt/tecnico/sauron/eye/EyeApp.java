package pt.tecnico.sauron.eye;

import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class EyeApp {

	public static void main(String[] args) {
		System.out.println(EyeApp.class.getSimpleName());
		Integer instance = null;
		
		// receive and print arguments
//		 System.out.printf("Received %d arguments%n", args.length);
//		 for (int i = 0; i < args.length; i++) {
//		   System.out.printf("arg[%d] = %s%n", i, args[i]);
//		 }

		if(args.length < 5) {
			System.out.println("Argument(s) missing!");
			System.out.printf("Usage: java %s zooHost zooPort camName latitude longitude [instance]%n", EyeApp.class.getName());
			return;
		}

		String zooHost = args[0];
		String zooPort = args[1];

		// register cam
		final String name = args[2];
		final double lat = Double.parseDouble(args[3]);
		final double lon = Double.parseDouble(args[4]);

		if (args.length >= 6) {
			instance = Integer.parseInt(args[5]);
		}

		SiloFrontend siloFrontend;
		try {
			if (instance == null)
				siloFrontend = new SiloFrontend(zooHost, zooPort);
			else {
				siloFrontend = new SiloFrontend(zooHost, zooPort, instance);
			}
			Eye eye = new Eye(siloFrontend, name, lat, lon);
			eye.interactive();
		} catch(FrontendException e) {
			System.err.println(e.getMessage());
		} catch (ZKNamingException e) {
			System.out.println("Could not find server in given path. Make sure the server is up and running.");
		}

	}
}
