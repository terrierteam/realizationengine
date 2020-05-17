package eu.bigdatastack.gdt.structures.data;

import java.util.HashMap;
import java.util.Map;

public class BigDataStackMetricValue {

	
	private String owner;
	private String namespace;
	private String appID;
	private String objectID;
	private String metricname;
	private String value;
	private long lastUpdated;
	private Map<String,String> labels;
	
	public BigDataStackMetricValue() {}
	
	public BigDataStackMetricValue(String owner, String namespace, String appID, String objectID,
			String metricname, String value, long lastUpdated, Map<String,String> labels) {
		super();
		this.owner = owner;
		this.namespace = namespace;
		this.appID = appID;
		this.objectID = objectID;
		this.metricname = metricname;
		this.value = value;
		this.lastUpdated = lastUpdated;
		this.labels = labels;
	}
	
	public BigDataStackMetricValue(String owner, String namespace, String appID, String objectID,
			String metricname) {
		super();
		this.owner = owner;
		this.namespace = namespace;
		this.appID = appID;
		this.objectID = objectID;
		this.metricname = metricname;
		this.value = "0";
		this.lastUpdated = -1;
		this.labels = new HashMap<String,String>();
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
		return objectID;
	}

	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}

	public String getMetricname() {
		return metricname;
	}

	public void setMetricname(String metricname) {
		this.metricname = metricname;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public long getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(long lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public Map<String, String> getLabels() {
		return labels;
	}

	public void setLabels(Map<String, String> labels) {
		this.labels = labels;
	}
	
	
}
