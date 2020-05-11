package eu.bigdatastack.gdt.application;

import java.io.File;

import eu.bigdatastack.gdt.lxdb.BigDataStackApplicationIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackOperationSequenceIO;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;

public interface Manager {

	// Creation of objects
	public BigDataStackApplication registerApplication(String yaml);
	public BigDataStackApplication registerApplication(File yamlFile);
	public BigDataStackObjectDefinition registerObject(String yaml);
	public BigDataStackObjectDefinition registerObject(File yamlFile);
	
	// Operation sequences
	public boolean createOperationSequence(BigDataStackApplication app, BigDataStackObjectDefinition object, String sequenceID);
	public boolean executeSequenceFromTemplateSync(BigDataStackOperationSequence sequenceTemplate);
	
	// Clients
	public BigDataStackApplicationIO getAppClient();
	public BigDataStackObjectIO getObjectTemplateClient();
	public BigDataStackOperationSequenceIO getSequenceTemplateClient();
	
	/**
	 * Shutdown the manager
	 */
	public void shutdown();
}
