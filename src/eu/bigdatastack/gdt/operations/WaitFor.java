package eu.bigdatastack.gdt.operations;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.EventUtil;

public class WaitFor extends BigDataStackOperation{

	private String appID;
	private String owner;
	private String namepace;
	
	private String instanceRef;
	private String waitForStatus;
	
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
	public String describeOperation() {
		return "Waits until "+instanceRef+" reaches "+waitForStatus+" status.";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
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
				
				Thread.sleep(5000);
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		

		return true;
	}
	
	@Override
	public void initalizeFromJson(JsonNode configJson) {
		instanceRef = configJson.get("instanceRef").asText();
		waitForStatus = configJson.get("waitForStatus").asText();
	}
	
}
