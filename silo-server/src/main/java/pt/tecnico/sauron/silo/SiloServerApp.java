package pt.tecnico.sauron.silo;


import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.io.IOException;

public class SiloServerApp {
	
	public static void main(String[] args) {
		System.out.println(SiloServerApp.class.getSimpleName());
		
		// receive and print arguments
		// System.out.printf("Received %d arguments%n", args.length);
		// for (int i = 0; i < args.length; i++) {
		//	System.out.printf("arg[%d] = %s%n", i, args[i]);
		// }

		if (args.length < 5) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", SiloServerApp.class.getName());
			return;
		}

		final String zooHost = args[0];
		final String zooPort = args[1];
		final String serverHost = args[2];
		final String serverPort = args[3];
		final String serverPath = args[4];

		ZKNaming zkNaming = null;
		try {
			zkNaming = new ZKNaming(zooHost, zooPort);
			// publish
			zkNaming.rebind(serverPath, serverHost, serverPort);
			SiloServer server = new SiloServer(Integer.parseInt(serverPort));
			server.start();
			server.awaitTermination();
		} catch(IOException e) {
			System.err.println("Error starting server at port: " + serverPort);
		} catch (InterruptedException e) {
			System.err.println("Error terminating server at port: " + serverPort);
			Thread.currentThread().interrupt();
		} catch (ZKNamingException e) {
			e.printStackTrace();

		} finally {
			if (zkNaming != null) {
				// remove
				try {
					zkNaming.unbind(serverPath, serverHost, serverPort);
				} catch (ZKNamingException e) {
					e.printStackTrace();
				}
			}
		}

		System.exit(0);
	}
	
}
