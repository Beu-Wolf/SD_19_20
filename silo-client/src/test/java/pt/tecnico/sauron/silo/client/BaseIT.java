package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

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
			testProps.load(BaseIT.class.getResourceAsStream(TEST_PROP_FILE));
			System.out.println("Test properties:");
			System.out.println(testProps);
			siloFrontend = new SiloFrontend(testProps.getProperty("zoo.host"), testProps.getProperty("zoo.port"), Integer.parseInt(testProps.getProperty("instance")));
		}catch (IOException e) {
			final String msg = String.format("Could not load properties file {}", TEST_PROP_FILE);
			System.out.println(msg);
			throw e;
		} catch (ZKNamingException e) {
			System.err.println("Could not create frontend: error in lookup");
		}
	}
	
	@AfterAll
	public static void cleanup() {
		siloFrontend.shutdown();
	}

}
