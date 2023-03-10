package org.terrier.realization.operations;

import java.util.HashSet;
import java.util.Set;

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

public class WaitFor extends BigDataStackOperation{

	private String appID;
	private String owner;
	private String namepace;
	
	private String instanceRef;
	private String waitForStatus;
	
	private int maxWaitTime = -1;
	
	public WaitFor() {
		this.className = this.getClass().getName();
	}
	
	public WaitFor(String appID, String owner, String namepace, String instanceRef, String waitForStatus) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.instanceRef = instanceRef;
		this.waitForStatus = waitForStatus;
		
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
	public String getObjectID() {
		return instanceRef;
	}
	public String getWaitForStatus() {
		return waitForStatus;
	}
	public void setWaitForStatus(String waitForStatus) {
		this.waitForStatus = waitForStatus;
	}
	
	public String getNamepace() {
		return namepace;
	}

	public void setNamepace(String namepace) {
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
		return "Waits until "+instanceRef+" reaches "+waitForStatus+" status.";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		long start = System.currentTimeMillis();
		long deadline = start + (maxWaitTime*60*1000);
		
		try {
			
			if (!parentSequenceRunner.getSequence().getParameters().containsKey(instanceRef)) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Wait-For Operation Failed: '"+instanceRef+"'",
						"Attempted to find an instance with within-sequence reference '"+instanceRef+"', but the parent sequence did not have an appropriate instance reference (did you Instantiate first?)",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			}
			
			String sourceObjectID = parentSequenceRunner.getSequence().getParameters().get(instanceRef).split(":")[0];
			int instance = Integer.valueOf(parentSequenceRunner.getSequence().getParameters().get(instanceRef).split(":")[1]);
			
			Set<String> failureStates = new HashSet<String>();
			failureStates.add("Failed");
			failureStates.add("Deleted");
			
			BigDataStackObjectIO objectInstanceClient = new BigDataStackObjectIO(database, false);
			boolean inTargetState = false;
			boolean inFailState = false;
			while (!inTargetState && !inFailState) {
				BigDataStackObjectDefinition instanceObject = objectInstanceClient.getObject(sourceObjectID, getOwner(), instance);
				if (instanceObject==null) {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Error,
							"Wait-For Operation Failed: '"+sourceObjectID+"("+instance+")'",
							"Attempted to get an instance '"+sourceObjectID+"("+instance+")', but was unable to find an associated object definition from available instances.",
							parentSequenceRunner.getSequence().getSequenceID(),
							parentSequenceRunner.getSequence().getIndex()
							);
					return false;
				}
				
				for (String status : instanceObject.getStatus()) {
					if (status.equalsIgnoreCase(waitForStatus)) inTargetState=true;
					if (failureStates.contains(status)) {
						inFailState = true;
						
						eventUtil.registerEvent(
								getAppID(),
								getOwner(),
								getNamespace(),
								BigDataStackEventType.Stage,
								BigDataStackEventSeverity.Warning,
								"Wait-For Operation Aborted for: '"+sourceObjectID+"("+instance+")'",
								"The underlying object that we were waiting for '"+sourceObjectID+"("+instance+")' entered a failure state",
								parentSequenceRunner.getSequence().getSequenceID(),
								parentSequenceRunner.getSequence().getIndex()
								);
						
						return false;
					}
				}
				
				
				if (maxWaitTime>=0 && System.currentTimeMillis()>=deadline) {
					return false;
				}
				
				Thread.sleep(5000);
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		

		return true;
	}
	
	@Override
	public void initalizeParameters(JsonNode configJson) {
		instanceRef = configJson.get("instanceRef").asText();
		waitForStatus = configJson.get("waitForStatus").asText();
		if (configJson.has("maxWaitTime")) {
			maxWaitTime = Integer.parseInt(configJson.get("maxWaitTime").asText());
		}
	}
	
}
