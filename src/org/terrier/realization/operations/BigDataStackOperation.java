package org.terrier.realization.operations;

import org.terrier.realization.openshift.OpenshiftOperationClient;
import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.prometheus.PrometheusDataClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.threads.OperationSequenceThread;
import org.terrier.realization.util.EventUtil;

import com.fasterxml.jackson.databind.JsonNode;

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
 * This interface represents an operation that can be done on the openshift cluster.
 *
 */
public abstract class BigDataStackOperation {

	private BigDataStackOperationState state = BigDataStackOperationState.NotStarted;
	protected String className;
	protected String operationDescription;
	protected String objectID = null;
	protected JsonNode configJson;
	
	public abstract String getAppID();

	public abstract String getOwner();

	public abstract String getNamespace();
	
	/**
	 * Gets the target objectID of this operation, not all operations have a target, and hence may return null
	 * @return
	 */
	public String getObjectID() {
		return objectID;
	}
	
	public String describeOperation() {
		if (operationDescription==null) return defaultDescription();
		return operationDescription;
	}
	
	public String defaultDescription() {
		return this.className;
	}
	

	public BigDataStackOperationState getState() {
		return state;
	}

	public void setState(BigDataStackOperationState state) {
		this.state = state;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	
	/**
	 * Executes the operation
	 * @param database - provides access to object state (use this to instantiate IO clients) 
	 * @param openshiftOperationClient - enables actions to be taken on the cluster
	 * @param openshiftStatusClient - enables cluster state to be retrieved
	 * @param mailboxClient - use this to read/write events
	 * @param prometheusDataClient - use this to get application metrics
	 * @return
	 */
	public abstract boolean execute(
			JDBCDB database,
			OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient,
			RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient,
			OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil);
	
	/**
	 * Configures this operation from a provided json definition
	 * @return
	 */
	public void initalizeFromJson(JsonNode configJson) {
		this.configJson = configJson;
		
		if (configJson.has("description")) {
			operationDescription = configJson.get("description").asText();
		}
		
		initalizeParameters(configJson);
	}
	
	public abstract void initalizeParameters(JsonNode configJson);
	
	public abstract void setAppID(String appID);
	
	public abstract void setOwner(String owner);
	
	public abstract void setNamespace(String namespace);

	public String getOperationDescription() {
		return operationDescription;
	}

	public void setOperationDescription(String operationDescription) {
		this.operationDescription = operationDescription;
	}

	public JsonNode getConfigJson() {
		return configJson;
	}

	public void setConfigJson(JsonNode configJson) {
		this.configJson = configJson;
	}
}
