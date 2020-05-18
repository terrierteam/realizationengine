package eu.bigdatastack.gdt.structures.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BigDataStackMetricValue {

	
	private String owner;
	private String namespace;
	private String appID;
	private String objectID;
	private String metricname;
	private List<String> value;
	private List<Long> lastUpdated;
	private List<Map<String,String>> labels;
	
	public BigDataStackMetricValue() {}
	
	public BigDataStackMetricValue(String owner, String namespace, String appID, String objectID,
			String metricname, List<String> value, List<Long> lastUpdated, List<Map<String,String>> labels) {
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
		this.value = new ArrayList<String>();
		this.lastUpdated = new ArrayList<Long>();;
		this.labels = new ArrayList<Map<String,String>>();
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

	public List<String> getValue() {
		return value;
	}

	public void setValue(List<String> value) {
		this.value = value;
	}

	public List<Map<String, String>> getLabels() {
		return labels;
	}

	public void setLabels(List<Map<String, String>> labels) {
		this.labels = labels;
	}

	public List<Long> getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(List<Long> lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	
	
}
