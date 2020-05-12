package eu.bigdatastack.gdt.operations;

import com.fasterxml.jackson.databind.JsonNode;

import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;

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
	public String describeOperation() {
		return "Waits for "+secondsToWait+" seconds.";
	}

	@Override
	public boolean execute(LXDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner) {
		
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
	public void initalizeFromJson(JsonNode configJson) {
		// TODO Auto-generated method stub
		
	}
	
}
