package pt.tecnico.sauron.silo;



import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.io.IOException;

public class SiloServerApp {

	public static void main(String[] args) {
		System.out.println(SiloServerApp.class.getSimpleName());
		ZKNaming zkNaming = null;

		// receive and print arguments
		// System.out.printf("Received %d arguments%n", args.length);
		// for (int i = 0; i < args.length; i++) {
		//	System.out.printf("arg[%d] = %s%n", i, args[i]);
		// }

		if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", SiloServerApp.class.getName());
			return;
		}

		final String zooHost = args[0];
		final String zooPort = args[1];
		final String host = args[2];
		final String port = args[3];
		final String path = args[4];

		try {
			zkNaming = new ZKNaming(zooHost, zooPort);
			zkNaming.rebind(path, host, port);
			SiloServer server = new SiloServer(Integer.parseInt(port));
			server.start();
			server.awaitTermination();
		} catch(IOException e) {
			System.err.println("Error starting server at port: " + port);
		} catch (InterruptedException e) {
			System.err.println("Error terminating server at port: " + port);
			Thread.currentThread().interrupt();
		} catch (ZKNamingException e) {
			e.printStackTrace();
		} finally {
			try {
				if(zkNaming != null)
					zkNaming.unbind(path, host, port);
			} catch (ZKNamingException e) {
				System.err.println(e.getMessage());
			}
		}



	}
	
}
