package eu.bigdatastack.gdt.operations;

import org.apache.commons.lang.NotImplementedException;

import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;

public class WaitFor extends BigDataStackOperation{

	private String appID;
	private String owner;
	private String namepace;
	
	private String objectID;
	private String waitForStatus;
	
	public WaitFor() {}
	
	public WaitFor(String appID, String owner, String namepace, String objectID, String waitForStatus) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.objectID = objectID;
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
	public void setNamepace(String namepace) {
		this.namepace = namepace;
	}
	public String getObjectID() {
		return objectID;
	}
	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}
	public String getWaitForStatus() {
		return waitForStatus;
	}
	public void setWaitForStatus(String waitForStatus) {
		this.waitForStatus = waitForStatus;
	}
	
	@Override
	public String describeOperation() {
		return "Waits until "+objectID+" reaches "+waitForStatus+" status.";
	}

	@Override
	public boolean execute(LXDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner) {
		throw new NotImplementedException();
	}
	
}
