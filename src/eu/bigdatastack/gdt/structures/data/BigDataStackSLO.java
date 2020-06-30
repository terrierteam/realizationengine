package eu.bigdatastack.gdt.structures.data;

/**
 * This class represents a Service Level Objective defined by a user that describes a quality
 * that they want an Object (of type Job or DeploymentConfig) should have. An SLO is linked to
 * a particular metric to be tracked.
 * @author EbonBlade
 *
 */
public class BigDataStackSLO {

	private String appID;
	private String owner;
	private String namespace;
	private String objectID;
	private int instance;
	
	private String metricName;
	
	private int sloIndex;
	
	private String type;
	private double value;
	
	private BigDataStackEventSeverity breachSeverity;
	private boolean isRequirement;
	
	public BigDataStackSLO() {}
	
	
	public BigDataStackSLO(String appID, String owner, String namespace, String objectID, int instance,
			String metricName, int sloIndex, String type, double value, BigDataStackEventSeverity breachSeverity,
			boolean isRequirement) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namespace = namespace;
		this.objectID = objectID;
		this.instance = instance;
		this.metricName = metricName;
		this.sloIndex = sloIndex;
		this.type = type;
		this.value = value;
		this.breachSeverity = breachSeverity;
		this.isRequirement = isRequirement;
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
		return namespace;
	}


	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}


	public String getObjectID() {
		return objectID;
	}


	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}


	public int getInstance() {
		return instance;
	}


	public void setInstance(int instance) {
		this.instance = instance;
	}


	public String getMetricName() {
		return metricName;
	}


	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}


	public int getSloIndex() {
		return sloIndex;
	}


	public void setSloIndex(int sloIndex) {
		this.sloIndex = sloIndex;
	}


	public String getType() {
		return type;
	}


	public void setType(String type) {
		this.type = type;
	}


	public double getValue() {
		return value;
	}


	public void setValue(double value) {
		this.value = value;
	}


	public BigDataStackEventSeverity getBreachSeverity() {
		return breachSeverity;
	}


	public void setBreachSeverity(BigDataStackEventSeverity breachSeverity) {
		this.breachSeverity = breachSeverity;
	}


	public boolean isRequirement() {
		return isRequirement;
	}


	public void setRequirement(boolean isRequirement) {
		this.isRequirement = isRequirement;
	}
	
	
}
