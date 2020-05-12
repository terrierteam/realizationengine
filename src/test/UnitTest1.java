package test;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.jupiter.api.Test;

import eu.bigdatastack.gdt.application.GDTManager;
import eu.bigdatastack.gdt.structures.config.GDTConfig;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;

/**
 * Performs the Hello World unit test
 * @author EbonBlade
 *
 */
public class UnitTest1 {

	
	@Test
	public void newAppAndDeployPod() throws Exception{
		
		GDTConfig config = new GDTConfig(new File("gdt.config.json"));
		TestUtil.clearDatabase(config.getDatabase());
		
		// Manager
		GDTManager manager = new GDTManager(config);
		
		// Create Application
		assertNotNull(manager.registerApplication(new File("resources/bigdatastack/unitTest1/unitTest1.app.yaml")));
		
		// Get Application
		BigDataStackApplication app = manager.getAppClient().getApp("unittest1", "richardm", "richardmproject");
		assertNotNull(app);
		
		// Create Hello World Pod Object
		assertNotNull(manager.registerObject(new File("resources/bigdatastack/unitTest1/helloWorld.pod.yaml")));
		
		// Get Object Template
		BigDataStackObjectDefinition object = manager.getObjectTemplateClient().getObject("helloworldpod", "richardm");
		assertNotNull(object);
		
		// Build the Operation Sequence Template
		assertTrue(manager.createOperationSequence(app, object, "seq-helloworldpod"));
		
		// Get Sequence Template
		BigDataStackOperationSequence sequence = manager.getSequenceTemplateClient().getSequence(app.getAppID(), "seq-helloworldpod");
		assertNotNull(sequence);
		
		// Execute Sequence
		assertTrue(manager.executeSequenceFromTemplateSync(sequence));
		
		manager.shutdown();
	}
	
	
}
