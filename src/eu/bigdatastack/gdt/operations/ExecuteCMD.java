package eu.bigdatastack.gdt.operations;

import org.apache.commons.lang.NotImplementedException;

import com.fasterxml.jackson.databind.JsonNode;

import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.EventUtil;

public class ExecuteCMD extends BigDataStackOperation {

	private String appID;
	private String owner;
	private String namepace;
	
	private String objectID;
	private String podID;
	private String command;
	
	
	public ExecuteCMD() {
		this.className = this.getClass().getName();
	}
	
	public ExecuteCMD(String appID, String owner, String namepace, String objectID, String podID,
			String command) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.objectID = objectID;
		this.podID = podID;
		this.command = command;

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
		return objectID;
	}

	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}

	public String getPodID() {
		return podID;
	}

	public void setPodID(String podID) {
		this.podID = podID;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}
	
	@Override
	public String describeOperation() {
		return "Executes the following command in "+objectID+":"+podID+"on the Openshift Cluster in "+namepace+". CMD: '"+command+"'";
	}

	@Override
	public boolean execute(LXDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		throw new NotImplementedException();
	}
	
	@Override
	public void initalizeFromJson(JsonNode configJson) {
		// TODO Auto-generated method stub
		
	}
}
