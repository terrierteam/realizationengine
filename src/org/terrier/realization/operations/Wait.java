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

public class Wait extends BigDataStackOperation {

	private String appID;
	private String owner;
	private String namepace;
	
	private int secondsToWait;

	public Wait() {
		this.className = this.getClass().getName();
	};
	
	public Wait(String appID, String owner, String namepace, int secondsToWait) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.secondsToWait = secondsToWait;
		
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

	public int getSecondsToWait() {
		return secondsToWait;
	}

	public void setSecondsToWait(int secondsToWait) {
		this.secondsToWait = secondsToWait;
	}
	
	@Override
	public String defaultDescription() {
		return "Waits for "+secondsToWait+" seconds.";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		try {
			Thread.sleep(1000*secondsToWait);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	@Override
	public String getObjectID() {
		return null;
	}
	
	@Override
	public void initalizeParameters(JsonNode configJson) {
		secondsToWait = Integer.parseInt(configJson.get("secondsToWait").asText());
		
	}
	
}
