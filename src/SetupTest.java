

import java.io.File;

import eu.bigdatastack.gdt.application.GDTManager;
import eu.bigdatastack.gdt.structures.config.GDTConfig;
import eu.bigdatastack.gdt.structures.data.BigDataStackNamespaceState;
import test.TestUtil;

public class SetupTest {

	public static void main(String[] args) throws Exception {
		
		// Config
				GDTConfig config = new GDTConfig(new File("gdt.config.json"));
				TestUtil.clearDatabase(config.getDatabase());

				// Manager
				/*GDTManager manager = new GDTManager(config);

				// Register Namespace
				BigDataStackNamespaceState namespace = manager.registerNamespace(new File("resources/bigdatastack/unitTest2/unitTest2.namespace,yaml"));

				manager.cleanUpEndedSequenceTemplates(namespace.getNamespace()); // clean any old sequence template runners

				// Start Namespace Monitoring
				manager.startMonitoringNamespace(namespace, "richardm");
				
				manager.printTimings();
				
				manager.shutdown();*/
				
				
				
		
	}
	
}
