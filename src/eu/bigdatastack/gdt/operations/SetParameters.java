package eu.bigdatastack.gdt.operations;

import org.apache.commons.lang.NotImplementedException;

import com.fasterxml.jackson.databind.JsonNode;

import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.EventUtil;

public class SetParameters extends BigDataStackOperation{

	private String appID;
	private String owner;
	private String namepace;
	
	private String objectID;
	private String parameterSourceObjectID;
	
	public SetParameters() {
		this.className = this.getClass().getName();
	}
	
	public SetParameters(String appID, String owner, String namepace, String objectID, String parameterSourceObjectID) {
		super();
		this.objectID = objectID;
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.parameterSourceObjectID = parameterSourceObjectID;
		
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
	
	public String getParameterSourceObjectID() {
		return parameterSourceObjectID;
	}

	public void setParameterSourceObjectID(String parameterSourceObjectID) {
		this.parameterSourceObjectID = parameterSourceObjectID;
	}

	@Override
	public String describeOperation() {
		return "Requests a parameter set for "+objectID+" to fill in missing paramter values from the service "+parameterSourceObjectID+".";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
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
