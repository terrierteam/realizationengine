package org.terrier.realization.structures.data;

import java.util.List;

import org.terrier.realization.operations.BigDataStackOperation;

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
 * This class represents a Service Level Objective defined by a user that describes a quality
 * that they want an Object (of type Job or DeploymentConfig) should have. An SLO is linked to
 * a particular metric to be tracked.
 *
 */
public class BigDataStackSLO {

	private String appID;
	private String owner;
	private String namespace;

	
	private String triggerID;
	private String metricName;
	private String triggerMessage;
	
	private String type;
	private double value;
	
	private BigDataStackEventSeverity breachSeverity;

	private String action;
	private List<BigDataStackOperation> safetyChecks;
	private int coolDownMins;
	
	public BigDataStackSLO() {}

	public BigDataStackSLO(String appID, String owner, String namespace, String triggerID, String metricName,
			String triggerMessage, String type, double value, BigDataStackEventSeverity breachSeverity, String action,
			List<BigDataStackOperation> safetyChecks, int coolDownMins) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namespace = namespace;
		this.triggerID = triggerID;
		this.metricName = metricName;
		this.triggerMessage = triggerMessage;
		this.type = type;
		this.value = value;
		this.breachSeverity = breachSeverity;
		this.action = action;
		this.safetyChecks = safetyChecks;
		this.coolDownMins = coolDownMins;
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
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getTriggerID() {
		return triggerID;
	}

	public void setTriggerID(String triggerID) {
		this.triggerID = triggerID;
	}

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	public String getTriggerMessage() {
		return triggerMessage;
	}

	public void setTriggerMessage(String triggerMessage) {
		this.triggerMessage = triggerMessage;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public BigDataStackEventSeverity getBreachSeverity() {
		return breachSeverity;
	}

	public void setBreachSeverity(BigDataStackEventSeverity breachSeverity) {
		this.breachSeverity = breachSeverity;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public List<BigDataStackOperation> getSafetyChecks() {
		return safetyChecks;
	}

	public void setSafetyChecks(List<BigDataStackOperation> safetyChecks) {
		this.safetyChecks = safetyChecks;
	}

	public int getCoolDownMins() {
		return coolDownMins;
	}

	public void setCoolDownMins(int coolDownMins) {
		this.coolDownMins = coolDownMins;
	}
	
	
	
}
