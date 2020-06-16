package eu.bigdatastack.gdt.operations;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.EventUtil;

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
	public String describeOperation() {
		return "Replaces any parameter placeholders for '"+instanceRef+"' with values set the operation sequence this is part of.";
	}

	@Override
	public boolean execute(LXDB database, OpenshiftOperationClient openshiftOperationClient,
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
						instanceRef
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
						sourceObjectID
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
						sourceObjectID
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
						sourceObjectID
						);
				return false;
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
		
	}
	
	@Override
	public void initalizeFromJson(JsonNode configJson) {
		instanceRef = configJson.get("instanceRef").asText();
	}

	@Override
	public String getObjectID() {
		return instanceRef;
	}
}
