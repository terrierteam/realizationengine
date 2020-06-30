package eu.bigdatastack.gdt.operations;

import java.sql.SQLException;

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

public class Delete extends BigDataStackOperation{

	private String instanceRef;
	private String appID;
	private String owner;
	private String namepace;


	public Delete() {
		this.className = this.getClass().getName();
	}

	public Delete(String appID, String owner, String namepace, String instanceRef) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.instanceRef = instanceRef;

		this.className = this.getClass().getName();
	}
	public String getObjectID() {
		return instanceRef;
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

	public String getNamepace() {
		return namepace;
	}

	public void setNamepace(String namepace) {
		this.namepace = namepace;
	}

	@Override
	public String describeOperation() {
		return "Deletes "+instanceRef+" on the Openshift Cluster in "+namepace+".";
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
						"Delete Operation Failed: '"+instanceRef+"'",
						"Attempted to find an instance with within-sequence reference '"+instanceRef+"', but the parent sequence did not have an appropriate instance reference (did you Instantiate first?)",
						getObjectID()
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
						"Delete Operation Failed: '"+sourceObjectID+"'",
						"Attempted to get an instance '"+sourceObjectID+"("+instance+")', but was unable to find an associated object definition from available instances.",
						sourceObjectID
						);
				return false;
			}

			// Delete object
			boolean deleteSuccessful = openshiftOperationClient.deleteOperation(instanceObject);
			if (deleteSuccessful) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Info,
						"Delete Operation Excecuted for: '"+sourceObjectID+"("+instance+")' ref '"+getObjectID()+"', instance "+instanceObject.getInstance(),
						"Executed an delete operation for '"+sourceObjectID+"("+instance+")' ref '"+getObjectID()+"' instance "+instanceObject.getInstance(),
						getObjectID()
						);
			} else {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Delete Operation Failed: '"+sourceObjectID+"("+instance+")' ref '"+getObjectID()+"'",
						"Attempted to delete an object '"+sourceObjectID+"("+instance+")' ref '"+getObjectID()+"', failed when communicating with Openshift, the object may already exist and cannot be replaced, or the request was otherwise rejected",
						getObjectID()
						);
				return false;
			}

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}


		return true;
	}

	@Override
	public void initalizeFromJson(JsonNode configJson) {
		instanceRef = configJson.get("instanceRef").asText();
	}
}
