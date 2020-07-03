package eu.bigdatastack.gdt.openshift;

import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;

/**
 * This is a standard interface for communicating with Openshift to
 * perform operations. This can be implemented by different underlying
 * implementations, which typically handle the authentication with the
 * openshift API.
 * @author EbonBlade
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
}
