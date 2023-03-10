package org.terrier.realization.operations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.terrier.realization.openshift.OpenshiftOperationClient;
import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.prometheus.PrometheusDataClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.BigDataStackMetricValueIO;
import org.terrier.realization.state.jdbc.BigDataStackObjectIO;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.structures.data.BigDataStackMetricValue;
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

public class RecordMetricsUntil extends BigDataStackOperation{

	
	String owner; 
	String namespace; 
	String appID;
	String instanceRef;
	List<String> metrics;
	int gracePeriodSec;
	String waitForStatus;
	
	public RecordMetricsUntil() {
		this.className = this.getClass().getName();
	}

	public RecordMetricsUntil(String owner, String namespace, String appID, String instanceRef, List<String> metrics, String waitForStatus, int gracePeriodSec) {
		this.owner = owner;
		this.namespace = namespace;
		this.appID = appID;
		this.instanceRef = instanceRef;
		this.metrics =metrics;
		this.waitForStatus = waitForStatus;
		this.gracePeriodSec = gracePeriodSec;
		
		this.className = this.getClass().getName();
	}
	
	public RecordMetricsUntil(String owner, String namespace, String appID, String instanceRef, List<String> metrics, String waitForStatus) {
		this.owner = owner;
		this.namespace = namespace;
		this.appID = appID;
		this.instanceRef = instanceRef;
		this.metrics =metrics;
		this.waitForStatus = waitForStatus;
		this.gracePeriodSec = 0;
		
		this.className = this.getClass().getName();
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	public String getAppID() {
		return appID;
	}
	public void setAppID(String appID) {
		this.appID = appID;
	}
	public String getObjectID() {
		return instanceRef;
	}
	
	public String getInstanceRef() {
		return instanceRef;
	}
	public void setInstanceRef(String instanceRef) {
		this.instanceRef = instanceRef;
	}
	public List<String> getMetrics() {
		return metrics;
	}
	public void setMetrics(List<String> metrics) {
		this.metrics = metrics;
	}
	public String getWaitForStatus() {
		return waitForStatus;
	}
	public void setWaitForStatus(String waitForStatus) {
		this.waitForStatus = waitForStatus;
	}
	public int getGracePeriodSec() {
		return gracePeriodSec;
	}

	public void setGracePeriodSec(int gracePeriodSec) {
		this.gracePeriodSec = gracePeriodSec;
	}

	@Override
	public String defaultDescription() {
		return "Iteratively checks and Stores metric values until "+instanceRef+" reaches status "+waitForStatus;
	}
	
	
	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		try {
			
			BigDataStackMetricValueIO metricValueClient = new BigDataStackMetricValueIO(database);
			
			if (!parentSequenceRunner.getSequence().getParameters().containsKey(instanceRef)) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Record-Metrics-Until Operation Failed: '"+instanceRef+"'",
						"Attempted to find an instance with within-sequence reference '"+instanceRef+"', but the parent sequence did not have an appropriate instance reference (did you Instantiate first?)",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			}
			
			String sourceObjectID = parentSequenceRunner.getSequence().getParameters().get(instanceRef).split(":")[0];
			int instance = Integer.valueOf(parentSequenceRunner.getSequence().getParameters().get(instanceRef).split(":")[1]);
			
			BigDataStackObjectIO objectInstanceClient = new BigDataStackObjectIO(database, false);
			BigDataStackObjectDefinition instanceObject = null;
			
			Set<String> failureStates = new HashSet<String>();
			failureStates.add("Failed");
			failureStates.add("Deleted");
			
			eventUtil.registerEvent(
					getAppID(),
					getOwner(),
					getNamespace(),
					BigDataStackEventType.Stage,
					BigDataStackEventSeverity.Info,
					"Record-Metrics-Until Operation Started: '"+instanceRef+"'",
					"Record-Metrics-Until Operation Started for within-sequence reference '"+instanceRef+"'",
					parentSequenceRunner.getSequence().getSequenceID(),
					parentSequenceRunner.getSequence().getIndex()
					);
			
			boolean inTargetState = false;
			boolean inFailState = false;
			while (!inTargetState && !inFailState) {
				instanceObject = objectInstanceClient.getObject(sourceObjectID, getOwner(), instance);
				if (instanceObject==null) {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Error,
							"Record-Metrics-Until Operation Failed: '"+sourceObjectID+"("+instance+")'",
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
								"Record-Metrics-Until Operation Aborted for: '"+sourceObjectID+"("+instance+")'",
								"The underlying object that we were waiting for '"+sourceObjectID+"("+instance+")' entered a failure state",
								parentSequenceRunner.getSequence().getSequenceID(),
								parentSequenceRunner.getSequence().getIndex()
								);
						
						return false;
					}
				}
				
				// update metrics
				//System.err.println("Check Metrics");
				List<BigDataStackMetricValue> metricsToUpdate = metricValueClient.getMetricValues(instanceObject.getAppID(), instanceObject.getOwner(), instanceObject.getNamespace(), sourceObjectID+"-"+instance, null);
				//System.err.println(metricsToUpdate.size());
				for (BigDataStackMetricValue metricValue : metricsToUpdate) {
					if (prometheusDataClient.update(metricValue)) {
						//System.err.println(metricValue.getValue());
						metricValueClient.updateMetricValue(metricValue);
					}
				}
				
				Thread.sleep(10000);
			}
		
			
			Thread.sleep(gracePeriodSec*1000);
			
			// update metrics
			List<BigDataStackMetricValue> metricsToUpdate = metricValueClient.getMetricValues(instanceObject.getAppID(), instanceObject.getOwner(), instanceObject.getNamespace(), instanceObject.getObjectID(), null);
			for (BigDataStackMetricValue metricValue : metricsToUpdate) {
				if (prometheusDataClient.update(metricValue)) {
					metricValueClient.updateMetricValue(metricValue);
				}
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
		JsonNode metrics = configJson.get("metrics");
		Iterator<JsonNode> metricNames = metrics.iterator();
		this.metrics = new ArrayList<String>();
		while (metricNames.hasNext()) {
			String metricName = metricNames.next().textValue();
			this.metrics.add(metricName);
		}
		gracePeriodSec = 0;
		if (configJson.has("gracePeriodSec")) gracePeriodSec = configJson.get("gracePeriodSec").asInt();
		waitForStatus = configJson.get("waitForStatus").asText();
		
	}
}
