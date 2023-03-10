package org.terrier.realization.operations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.terrier.realization.openshift.OpenshiftOperationClient;
import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.prometheus.PrometheusDataClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.BigDataStackPodStatusIO;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.structures.data.BigDataStackPodStatus;
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

public class ExecuteCMD extends BigDataStackOperation {

	private String appID;
	private String owner;
	private String namespace;
	
	private String objectID;
	private String instancelookup;
	private String[][] commands;
	
	
	public ExecuteCMD() {
		this.className = this.getClass().getName();
	}
	
	public ExecuteCMD(String appID, String owner, String namespace, String objectID, String instancelookup,
			String[][] commands) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namespace = namespace;
		this.objectID = objectID;
		this.instancelookup = instancelookup;
		this.commands = commands;

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
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getObjectID() {
		return objectID;
	}

	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}

	public String getInstancelookup() {
		return instancelookup;
	}

	public void setInstancelookup(String instancelookup) {
		this.instancelookup = instancelookup;
	}

	public String[][] getCommands() {
		return commands;
	}

	public void setCommands(String[][] commands) {
		this.commands = commands;
	}

	@Override
	public String defaultDescription() {
		return "Executes a series of commands in pods of type "+objectID+" on the Openshift Cluster in "+namespace;
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		
		List<BigDataStackPodStatus> statuses = lookupInstances(database);
		if (statuses==null) return false;
		
		
		// replace any placeholders in the commands
		Map<String,String> parameters = parentSequenceRunner.getSequence().getParameters();
		for (int i =0; i<commands.length; i++) {
			String[] args = commands[i];
			for (int j =0; j<args.length; j++) {
				if (commands[i][j]==null) continue; 
				for (String paramKey : parameters.keySet()) {
					commands[i][j] = commands[i][j].replaceAll("\\$"+paramKey+"\\$", parameters.get(paramKey));
				}
			}
		}
		
		try {
			for (BigDataStackPodStatus status : statuses) {
				List<String> outputs = openshiftOperationClient.execCommands(status, commands);
				System.out.println(outputs2PrintFormat(outputs));
				if (outputs==null) {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Error,
							"ExecuteCMD Operation Failed on: '"+getObjectID()+"("+status.getInstance()+")'",
							"Attempted to execute a set of commands on '"+getObjectID()+"("+status.getInstance()+")', but failed. "+commands2PrintFormat(),
							parentSequenceRunner.getSequence().getSequenceID(),
							parentSequenceRunner.getSequence().getIndex()
							);
				} else {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Info,
							"ExecuteCMD Operation Completed on: '"+getObjectID()+"("+status.getInstance()+")'",
							"Executed the following commands on '"+getObjectID()+"("+status.getInstance()+")': "+commands2PrintFormat(),
							parentSequenceRunner.getSequence().getSequenceID(),
							parentSequenceRunner.getSequence().getIndex()
							);
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Openshift,
							BigDataStackEventSeverity.Info,
							"Pod '"+getObjectID()+"("+status.getInstance()+")' Output",
							outputs2PrintFormat(outputs),
							parentSequenceRunner.getSequence().getSequenceID(),
							parentSequenceRunner.getSequence().getIndex()
							);
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
		objectID = configJson.get("objectID").asText();
		instancelookup = configJson.get("instancelookupCriteria").asText();
		
		JsonNode firstLevel = configJson.get("commands");
		Iterator<JsonNode> firstLevelNodes = firstLevel.elements();
		int numCommands = 0;
		while (firstLevelNodes.hasNext()) {
			numCommands++;
			firstLevelNodes.next();
		}
		
		commands = new String[numCommands][];
		firstLevelNodes = firstLevel.elements();
		int i =0;
		while (firstLevelNodes.hasNext()) {
			JsonNode commandList = firstLevelNodes.next();
			List<String> commandParts = new ArrayList<String>();
			Iterator<JsonNode> secondLevelNodes = commandList.elements();
			while (secondLevelNodes.hasNext()) {
				String commandPart = secondLevelNodes.next().asText();
				commandParts.add(commandPart);
			}
			String[] command = new String[commandParts.size()];
			for (int j =0; j<commandParts.size(); j++) {
				command[j] = commandParts.get(j);
			}
			commands[i] = command;
			i++;
		}
		
	}
	
	
	/**
	 * Tries to match the given criteria string to one or more instances registered with the Realization Engine
	 * 
	 * Valid Formats:
	 *  - <instanceID> returns the specified instance if it exists)
	 *  - <instanceID>,<instanceID>,<instanceID>...
	 *  - all (returns all running instances)
	 *  - first (first instance found)
	 * @return
	 */
	public List<BigDataStackPodStatus> lookupInstances(JDBCDB database) {
		
		try {
			BigDataStackPodStatusIO podStatusClient = new BigDataStackPodStatusIO(database);
			List<BigDataStackPodStatus> podStatuses = podStatusClient.getPodStatuses(appID, owner, objectID, namespace, -1);
			List<BigDataStackPodStatus> matchedStatuses = new ArrayList<BigDataStackPodStatus>();
			
			if (instancelookup.equalsIgnoreCase("all")) {
				for (BigDataStackPodStatus status : podStatuses) {
					if (status.getStatus().contains("Running") || status.getStatus().contains("Ready")) matchedStatuses.add(status);
				}
				return matchedStatuses;
			}
			
			if (instancelookup.equalsIgnoreCase("first")) {
				for (BigDataStackPodStatus status : podStatuses) {
					if (status.getStatus().contains("Running") || status.getStatus().contains("Ready")) {
						matchedStatuses.add(status);
						return matchedStatuses;
					}
				}
			}
			
			Set<Integer> toMatch = new HashSet<Integer>();
			if (instancelookup.contains(",")) {
				for (String val : instancelookup.split(",")) {
					toMatch.add(Integer.parseInt(val));
				}
			} else {
				toMatch.add(Integer.parseInt(instancelookup));
			}
			
			
			for (BigDataStackPodStatus status : podStatuses) {
				if (toMatch.contains(status.getInstance())) {
					if (status.getStatus().contains("Running") || status.getStatus().contains("Ready")) matchedStatuses.add(status);
				}
			}

			
			return matchedStatuses;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String commands2PrintFormat() {
		StringBuilder builder = new StringBuilder();
		
		int i =1;
		for (String[] command : commands) {
			if (i>1) builder.append(", ");
			builder.append("CMD("+i+") [");
			for (String val : command) {
				builder.append(val);
				builder.append(" ");
			}
			builder.append("]");
			i++;
		}
		return builder.toString();
		
	}
	
	public String outputs2PrintFormat(List<String> outputs) {
		StringBuilder builder = new StringBuilder();
		
		int i =1;
		for (String output : outputs) {
			if (i>1) builder.append(", ");
			builder.append("CMD Output("+i+") [");
			builder.append(output);
			builder.append("]");
			i++;
		}
		return builder.toString();
	}
}
