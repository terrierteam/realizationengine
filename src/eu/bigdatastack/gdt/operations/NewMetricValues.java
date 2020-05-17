package eu.bigdatastack.gdt.operations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import eu.bigdatastack.gdt.lxdb.BigDataStackMetricValueIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetricValue;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.EventUtil;

public class NewMetricValues extends BigDataStackOperation{

	
	String owner; 
	String namespace; 
	String appID;
	String instanceRef;
	List<String> metrics;

	public NewMetricValues() {
		this.className = this.getClass().getName();
	}
	
	public NewMetricValues(String owner, String namespace, String appID, String instanceRef, List<String> metrics) {
		super();
		this.owner = owner;
		this.namespace = namespace;
		this.appID = appID;
		this.instanceRef = instanceRef;
		this.metrics =metrics;
		
		this.className = this.getClass().getName();
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
	public String getAppID() {
		return appID;
	}
	public void setAppID(String appID) {
		this.appID = appID;
	}
	public String getObjectID() {
		return instanceRef;
	}
	
	public String getInstanceRef() {
		return instanceRef;
	}
	public void setInstanceRef(String instanceRef) {
		this.instanceRef = instanceRef;
	}
	public List<String> getMetrics() {
		return metrics;
	}
	public void setMetrics(List<String> metrics) {
		this.metrics = metrics;
	}
	
	
	@Override
	public String describeOperation() {
		return "Creates a new set of metric value objects in the database that can be used to maintain a set of metrics.";
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
						"New Metric Values Creation Operation Failed: '"+instanceRef+"'",
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
						"New Metric Values Creation Operation Failed: '"+sourceObjectID+"'",
						"Attempted to get an instance '"+sourceObjectID+"("+instance+")', but was unable to find an associated object definition from available instances.",
						sourceObjectID
						);
				return false;
			}
			
			BigDataStackMetricValueIO metricValueClient = new BigDataStackMetricValueIO(database);
			
			for (String metric : metrics) {
				
				BigDataStackMetricValue metricValue = new BigDataStackMetricValue(
						owner, namespace, appID, sourceObjectID+"-"+instance,
						metric);
				
				if (!metricValueClient.addMetricValue(metricValue)) {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Warning,
							"New Metric Values Creation Operation tried to add : '"+metric+"' for '"+sourceObjectID+"("+instance+")' but failed",
							"Attempted to register metric '"+metric+"' value for '"+sourceObjectID+"("+instance+")', but registration was rejected.",
							sourceObjectID
							);
				} else {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Info,
							"New Metric Value Creation Operation added : '"+metric+"' for '"+sourceObjectID+"("+instance+")'",
							"Registered metric '"+metric+"' value for '"+sourceObjectID+"("+instance+")'.",
							sourceObjectID
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
		instanceRef = configJson.get("instanceRef").asText();
		JsonNode metrics = configJson.get("metrics");
		Iterator<JsonNode> metricNames = metrics.iterator();
		this.metrics = new ArrayList<String>();
		while (metricNames.hasNext()) {
			String metricName = metricNames.next().textValue();
			this.metrics.add(metricName);
		}
		
		
	}
	
	
	
}
