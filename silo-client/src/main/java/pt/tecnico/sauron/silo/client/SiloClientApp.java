package pt.tecnico.sauron.silo.client;


import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.grpc.Silo;

public class SiloClientApp {
	
	public static void main(String[] args) {
		System.out.println(SiloClientApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length < 2) {
			System.out.println("Argument(s) missing!");
			System.out.printf("Usage: java %s host port%n", SiloClientApp.class.getName());
			return;
		}

		final String host = args[0];
		final int port = Integer.parseInt(args[1]);

		SiloFrontend siloFrontend = new SiloFrontend(host, port);
		try {
			Silo.PingRequest request = Silo.PingRequest.newBuilder().setText("friend").build();
			Silo.PingResponse response = siloFrontend.ctrlPing(request);
			System.out.println(response.getText());
		} catch (StatusRuntimeException e) {
			System.err.println("Caught exception with description: " + e.getStatus().getDescription());
		}

		siloFrontend.shutdown();
	}


	
}
