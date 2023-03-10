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
 * Deploys a BigDataStack object. This is a short hand way of calling Instantiate->SetSequenceParamters->Apply
 *
 */
public class Deploy extends BigDataStackOperation{

	private String appID;
	private String owner;
	private String namespace;

	private String instanceRef;

	public Deploy() {
		this.className = this.getClass().getName();
	}

	public Deploy(String appID, String owner, String namespace, String objectID, String defineInstanceRef) {
		super();
		this.objectID = objectID;
		this.appID = appID;
		this.owner = owner;
		this.namespace = namespace;
		this.instanceRef = defineInstanceRef;

		this.className = this.getClass().getName();
	}

	@Override
	public String getAppID() {
		return appID;
	}

	@Override
	public String getOwner() {
		return owner;
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public String defaultDescription() {
		return "Deploys '"+objectID+"' on the cluster.";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		
		Instantiate instantiate = new Instantiate(appID, owner, namespace, objectID, instanceRef);
		if (!instantiate.execute(database, openshiftOperationClient, openshiftStatusClient, mailboxClient, prometheusDataClient, parentSequenceRunner, eventUtil)) return false;
		
		SetSequenceParameters ssp = new SetSequenceParameters(appID, owner, namespace, instanceRef);
		if (!ssp.execute(database, openshiftOperationClient, openshiftStatusClient, mailboxClient, prometheusDataClient, parentSequenceRunner, eventUtil)) return false;
		
		Apply apply = new Apply(appID, owner, namespace, instanceRef);
		if (!apply.execute(database, openshiftOperationClient, openshiftStatusClient, mailboxClient, prometheusDataClient, parentSequenceRunner, eventUtil)) return false;
		
		return true;
	}

	@Override
	public void initalizeParameters(JsonNode configJson) {
		instanceRef = configJson.get("instanceRef").asText();
		objectID = configJson.get("objectID").asText();
	}

	@Override
	public void setAppID(String appID) {
		this.appID = appID;
		
	}

	@Override
	public void setOwner(String owner) {
		this.owner =owner;
		
	}

	@Override
	public void setNamespace(String namespace) {
		this.namespace =namespace;
		
	}

	public String getInstanceRef() {
		return instanceRef;
	}

	public void setInstanceRef(String instanceRef) {
		this.instanceRef = instanceRef;
	}
}
