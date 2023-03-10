package org.terrier.realization.operations;

import java.io.File;
import java.sql.SQLException;

import org.terrier.realization.openshift.OpenshiftOperationClient;
import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.prometheus.PrometheusDataClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
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
 
public class WaitForFile extends BigDataStackOperation {

	private String appID;
	private String owner;
	private String namepace;
	
	private String targetFile;
	private int maxWaitTime = -1;

	public WaitForFile() {
		this.className = this.getClass().getName();
	};
	
	public WaitForFile(String appID, String owner, String namepace, String targetFile) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.targetFile = targetFile;
		
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

	
	@Override
	public String defaultDescription() {
		return "Waits until a file appears on disk.";
	}

	public String getNamepace() {
		return namepace;
	}

	public void setNamepace(String namepace) {
		this.namepace = namepace;
	}

	public String getTargetFile() {
		return targetFile;
	}

	public void setTargetFile(String targetFile) {
		this.targetFile = targetFile;
	}

	public int getMaxWaitTime() {
		return maxWaitTime;
	}

	public void setMaxWaitTime(int maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		for (String paramKey : parentSequenceRunner.getSequence().getParameters().keySet()) {
			targetFile = targetFile.replaceAll("\\$"+paramKey+"\\$", parentSequenceRunner.getSequence().getParameters().get(paramKey));
		}
		
		long start = System.currentTimeMillis();
		long deadline = start + (maxWaitTime*60*1000);
		
		boolean fileFound = false;
		while (!fileFound) {
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			File f = new File(targetFile);
			fileFound = f.exists();
			
			if (maxWaitTime>=0 && System.currentTimeMillis()>=deadline) {
				break;
			}
			
		}
		
		if (fileFound) {
			try {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Info,
						"Wait-For-File Operation Completed: '"+targetFile+"' exists",
						"The underlying file that we were waiting for '"+targetFile+"' was found in the file system",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			try {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Wait-For-File Operation Failed: '"+targetFile+"' was not found by the deadline ("+maxWaitTime+" mins)",
						"The underlying file that we were waiting for '"+targetFile+"' was not found in the file system after the specified deadline ("+maxWaitTime+" mins)",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return fileFound;
	}

	@Override
	public String getObjectID() {
		return null;
	}
	
	@Override
	public void initalizeParameters(JsonNode configJson) {
		targetFile = configJson.get("targetFile").asText();
		if (configJson.has("maxWaitTime")) {
			maxWaitTime = Integer.parseInt(configJson.get("maxWaitTime").asText());
		}
		
	}

}
