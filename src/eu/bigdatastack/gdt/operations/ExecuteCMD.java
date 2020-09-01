package eu.bigdatastack.gdt.operations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import com.fasterxml.jackson.databind.JsonNode;

import eu.bigdatastack.gdt.lxdb.BigDataStackPodStatusIO;
import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackPodStatus;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.EventUtil;

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
	public String describeOperation() {
		return "Executes a series of commands in pods of type "+objectID+" on the Openshift Cluster in "+namespace;
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		
		List<BigDataStackPodStatus> statuses = lookupInstances(database);
		if (statuses==null) return false;
		
		try {
			for (BigDataStackPodStatus status : statuses) {
				List<String> outputs = openshiftOperationClient.execCommands(status, commands);
				if (outputs==null) {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Error,
							"ExecuteCMD Operation Failed on: '"+getObjectID()+"("+status.getInstance()+")'",
							"Attempted to execute a set of commands on '"+getObjectID()+"("+status.getInstance()+")', but failed. "+commands2PrintFormat(),
							getObjectID()
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
							getObjectID()
							);
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Openshift,
							BigDataStackEventSeverity.Info,
							"Pod '"+getObjectID()+"("+status.getInstance()+")' Output",
							outputs2PrintFormat(outputs),
							getObjectID()
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
	public void initalizeFromJson(JsonNode configJson) {
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
			String[] command = (String[])commandParts.toArray();
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
