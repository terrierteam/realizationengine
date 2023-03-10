package org.terrier.realization.operations;

import java.util.Map;

import org.terrier.realization.openshift.OpenshiftOperationClient;
import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.prometheus.PrometheusDataClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.BigDataStackObjectIO;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
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

public class SetSequenceParameters extends BigDataStackOperation{

	private String appID;
	private String owner;
	private String namepace;
	
	private String instanceRef;
	
	public SetSequenceParameters() {
		this.className = this.getClass().getName();
	}
	
	public SetSequenceParameters(String appID, String owner, String namepace, String instanceRef) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.instanceRef = instanceRef;
		
		this.className = this.getClass().getName();
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

	public String getInstanceRef() {
		return instanceRef;
	}

	public void setInstanceRef(String instanceRef) {
		this.instanceRef = instanceRef;
	}

	@Override
	public String defaultDescription() {
		return "Replaces any parameter placeholders for '"+instanceRef+"' with values set the operation sequence this is part of.";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		
		try {
			
			if (!parentSequenceRunner.getSequence().getParameters().containsKey(instanceRef)) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Set Sequence Parameters Operation Failed: '"+getObjectID()+"'",
						"Attempted to find an instance with within-sequence reference '"+getObjectID()+"', but the parent sequence did not have an appropriate instance reference (did you Instantiate first?)",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			}
			
			String sourceObjectID = parentSequenceRunner.getSequence().getParameters().get(instanceRef).split(":")[0];
			int instance = Integer.valueOf(parentSequenceRunner.getSequence().getParameters().get(instanceRef).split(":")[1]);
			
			// Stage 1: Retrieve instance object
			BigDataStackObjectIO objectInstanceClient = new BigDataStackObjectIO(database, false);
			BigDataStackObjectDefinition instanceObject = objectInstanceClient.getObject(sourceObjectID, getOwner(), instance);
			if (instanceObject==null) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Set Sequence Parameters Operation Failed: '"+sourceObjectID+"("+instance+")'",
						"Attempted to set paramters for object instance '"+sourceObjectID+"("+instance+")', but was unable to find an associated object definition from available instances.",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			}
			
			// Stage 2: Set Parameters
			Map<String,String> parameters = parentSequenceRunner.getSequence().getParameters();
			String yaml = instanceObject.getYamlSource();
			for (String paramKey : parameters.keySet()) {
				yaml = yaml.replaceAll("\\$"+paramKey+"\\$", parameters.get(paramKey));
			}
			instanceObject.setYamlSource(yaml);
			
			if (objectInstanceClient.updateObject(instanceObject)) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Info,
						"Set Sequence Parameters Operation Completed: '"+sourceObjectID+"("+instance+")'",
						"Set paramters for object instance '"+sourceObjectID+"("+instance+")' based on operation sequence '"+parentSequenceRunner.getSequence().getSequenceID()+"'",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				Thread.sleep(2000); // add a short sleep here to make sure the update went through
			} else {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Set Sequence Parameters Operation Failed: '"+sourceObjectID+"("+instance+")'",
						"Attempted to set paramters for object instance '"+sourceObjectID+"("+instance+")', but was unable to write the instance back to the database.",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
		
	}
	
	@Override
	public void initalizeParameters(JsonNode configJson) {
		instanceRef = configJson.get("instanceRef").asText();
	}

	@Override
	public String getObjectID() {
		return instanceRef;
	}
}
