package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import pt.tecnico.sauron.silo.client.exceptions.FrontendException;

import java.io.IOException;
import java.util.Properties;


public class BaseIT {

	private static final String TEST_PROP_FILE = "/test.properties";
	protected static Properties testProps;
	protected static SiloFrontend siloFrontend;
	
	@BeforeAll
	public static void oneTimeSetup () throws IOException {
		testProps = new Properties();

		try {
			siloFrontend = new SiloFrontend(testProps.getProperty("server.host"), testProps.getProperty("server.port"), testProps.getProperty("server.path"));
			testProps.load(BaseIT.class.getResourceAsStream(TEST_PROP_FILE));
			System.out.println("Test properties:");
			System.out.println(testProps);
		}catch (IOException e) {
			final String msg = String.format("Could not load properties file {}", TEST_PROP_FILE);
			System.out.println(msg);
			throw e;
		} catch (FrontendException e) {
			System.out.println(e.getMessage());
		}


	}
	
	@AfterAll
	public static void cleanup() {
		siloFrontend.shutdown();
	}

}
