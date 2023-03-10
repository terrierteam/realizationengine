package org.terrier.realization.operations;


import javax.ws.rs.NotAllowedException;

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

public class SetParameters extends BigDataStackOperation{

	private String appID;
	private String owner;
	private String namepace;
	
	private String objectID;
	private String parameterSourceObjectID;
	
	public SetParameters() {
		this.className = this.getClass().getName();
	}
	
	public SetParameters(String appID, String owner, String namepace, String objectID, String parameterSourceObjectID) {
		super();
		this.objectID = objectID;
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.parameterSourceObjectID = parameterSourceObjectID;
		
		this.className = this.getClass().getName();
	}
	public String getObjectID() {
		return objectID;
	}
	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}
	public String getAppID() {
		return appID;
	}
	public void setAppID(String appID) {
		this.appID = appID;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getNamespace() {
		return namepace;
	}
	public void setNamespace(String namepace) {
		this.namepace = namepace;
	}
	
	public String getParameterSourceObjectID() {
		return parameterSourceObjectID;
	}

	public void setParameterSourceObjectID(String parameterSourceObjectID) {
		this.parameterSourceObjectID = parameterSourceObjectID;
	}

	@Override
	public String defaultDescription() {
		return "Requests a parameter set for "+objectID+" to fill in missing paramter values from the service "+parameterSourceObjectID+".";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		throw new NotAllowedException("");
	}
	
	@Override
	public void initalizeParameters(JsonNode configJson) {
		// TODO Auto-generated method stub
		
	}
}
