package pt.tecnico.sauron.silo;


import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.io.IOException;

public class SiloServerApp {
	
	public static void main(String[] args) {
		System.out.println(SiloServerApp.class.getSimpleName());
		
		 //receive and print arguments
//		 System.out.printf("Received %d arguments%n", args.length);
//		 for (int i = 0; i < args.length; i++) {
//			System.out.printf("arg[%d] = %s%n", i, args[i]);
//		 }

		if (args.length < 5) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s zooKeeperHost zooKeeperPort serverHost serverPort serverPath%n", SiloServerApp.class.getName());
			return;
		}

		// read arguments
		final String zooHost = args[0];
		final String zooPort = args[1];
		final String serverHost = args[2];
		final String serverPort = args[3];
		final String serverPath = args[4];
		final String[] pathSplit = serverPath.split("/");
		final int instance = Integer.parseInt(pathSplit[pathSplit.length-1]);

		ZKNaming zkNaming = null;
		try {
		    // connect to name server
			zkNaming = new ZKNaming(zooHost, zooPort);

			// register silo-server on name server
			zkNaming.rebind(serverPath, serverHost, serverPort);

			// run silo-server
			SiloServer server = new SiloServer(Integer.parseInt(serverPort), zkNaming, instance);
			server.start();
			server.awaitTermination();

		} catch(IOException e) {
			System.err.println("Error starting server at port: " + serverPort);
		} catch (InterruptedException e) {
			System.err.println("Error terminating server at port: " + serverPort);
			Thread.currentThread().interrupt();
		} catch (ZKNamingException e) {
		    System.err.println("Name server error: " + e.getMessage());

		} finally {
			if (zkNaming != null) {
				// sign out from name server
				try {
					zkNaming.unbind(serverPath, serverHost, serverPort);
				} catch (ZKNamingException e) {
					System.err.println("Name server error: " + e.getMessage());
				}
			}
		}

		// exit cleanly: avoid thread killing exception
		System.exit(0);
	}
}
