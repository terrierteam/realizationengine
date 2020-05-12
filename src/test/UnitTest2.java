package test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import eu.bigdatastack.gdt.application.GDTManager;
import eu.bigdatastack.gdt.structures.config.GDTConfig;
import eu.bigdatastack.gdt.structures.data.BigDataStackNamespaceState;

public class UnitTest2 {

	@Test
	public void monitoringDeploy() throws Exception{
		
		// Config
		GDTConfig config = new GDTConfig(new File("gdt.config.json"));
		TestUtil.clearDatabase(config.getDatabase());
		
		// Manager
		GDTManager manager = new GDTManager(config);
		
		BigDataStackNamespaceState namespace = manager.registerNamespace(new File("resources/bigdatastack/unitTest2/unitTest2.namespace,yaml"));
		
		assertNotNull(namespace);
		
		assertTrue(manager.startMonitoringNamespace(namespace, "richardm"));
				
		manager.shutdown();
		
		
	}
	
}
