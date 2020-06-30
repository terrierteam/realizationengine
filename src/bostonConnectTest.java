import java.io.File;

import eu.bigdatastack.gdt.application.GDTManager;
import eu.bigdatastack.gdt.structures.config.GDTConfig;

public class bostonConnectTest {

	public static void main(String[] args) throws Exception {
		
		GDTConfig config = new GDTConfig(new File("boston.gdt.config.json"));
		
		GDTManager manager = new GDTManager(config);
		
		manager.registerNamespace(new File("resources/boston/realization.namespace,yaml"));
		
		
	}
	
	
}
