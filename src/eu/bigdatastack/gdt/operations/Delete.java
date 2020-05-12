package eu.bigdatastack.gdt.operations;

import org.apache.commons.lang.NotImplementedException;

import com.fasterxml.jackson.databind.JsonNode;

import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;

public class Delete extends BigDataStackOperation{

	private String objectID;
	private String appID;
	private String owner;
	private String namepace;
	
	public Delete() {}
	
	public Delete(String appID, String owner, String namepace, String objectID) {
		super();
		this.objectID = objectID;
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		
		this.className = this.getClass().getName();
	}
	public String getObjectID() {
		return objectID;
	}
	public void setObjectID(String objectID) {
		this.objectID = objectID;
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
	public String describeOperation() {
		return "Creates "+objectID+" on the Openshift Cluster in "+namepace+".";
	}

	@Override
	public boolean execute(LXDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner) {
		throw new NotImplementedException();
	}

	@Override
	public void initalizeFromJson(JsonNode configJson) {
		// TODO Auto-generated method stub
		
	}
}
