package pt.tecnico.sauron.silo;


import io.grpc.*;

import java.io.IOException;

public class SiloServerApp {
	
	public static void main(String[] args) {
		System.out.println(SiloServerApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", SiloServerApp.class.getName());
			return;
		}

		final int port = Integer.parseInt(args[0]);

		try {
			SiloServer server = new SiloServer(port);
			server.start();
			server.awaitTermination();
		} catch(IOException e) {
			System.err.println("Error starting server at port: " + port);
		} catch (InterruptedException e) {
			System.err.println("Error terminating server at port: " + port);
			Thread.currentThread().interrupt();
		}

	}
	
}
