package org.terrier.realization.openshift;

import java.util.List;

import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackPodStatus;


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
 
/**
 * This is a standard interface for communicating with Openshift to
 * perform operations. This can be implemented by different underlying
 * implementations, which typically handle the authentication with the
 * openshift API.
 *
 */
public interface OpenshiftOperationClient {

	/**
	 * Attempt to generate a connection to Openshift. Operations can be
	 * performed after this method is called (assuming it returns true.
	 * Under the hood this will authenticate with openshift and get a
	 * token,
	 * @return was successful?
	 */
	public boolean connectToOpenshift();
	
	
	
	/**
	 * Takes a BigDataStackObjectDefinition and calls the apply operation on
	 * the cluster to create the associated Kubernetes/Openshift object.
	 * @param object
	 * @return was successful?
	 */
	public boolean applyOperation(BigDataStackObjectDefinition object);
	
	
	/**
	 * Deletes any associated objects to the given BigDataStackObjectDefinition
	 * on the cluster. 
	 * @param object
	 * @return was successful?
	 */
	public boolean deleteOperation(BigDataStackObjectDefinition object);
	
	
	
	/**
	 * Close the connection to Openshift.
	 */
	public void close();
	
	/**
	 * Executes a command on a specified Pod object
	 * @param object
	 * @param instance
	 * @param command
	 * @return
	 */
	public List<String> execCommands(BigDataStackPodStatus pod, String[][] commands);
}
