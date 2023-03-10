package org.terrier.realization.application;

import java.io.File;

import org.terrier.realization.state.jdbc.BigDataStackApplicationIO;
import org.terrier.realization.state.jdbc.BigDataStackObjectIO;
import org.terrier.realization.state.jdbc.BigDataStackOperationSequenceIO;
import org.terrier.realization.structures.data.BigDataStackApplication;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackOperationSequence;

 /*
 * Realization Engine 
 * Webpage: https://github.com/terrierteam/realizationengine
 * Contact: richard.mccreadie@glasgow.ac.uk
 * University of Glasgow - School of Computing Science
 * http://www.gla.ac.uk/
 * 
 * The contents of this file are subject to the Apache License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 *
 * The Original Code is Copyright (C) to the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richard.mccreadie@glasgow.ac.uk> (original author)
 */

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
