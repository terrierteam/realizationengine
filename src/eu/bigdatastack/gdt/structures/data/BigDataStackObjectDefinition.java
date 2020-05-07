package eu.bigdatastack.gdt.structures.data;

import java.util.Set;

/**
 * This is a representation of a BigDataStack/Kubernetes object definition, representing either a DeploymentConfig, 
 * Job, Service, Route, Volume, VolumeClaim, etc
 * 
 * These represent the initial definition of the object and will not be altered. 
 * @author EbonBlade
 *
 */
public class BigDataStackObjectDefinition {

	private String objectID;
	private String owner;
	private BigDataStackObjectType type;
	private String yamlSource;
	private Set<String> status;
	private int instance;
	
	public BigDataStackObjectDefinition() {}
	
	public BigDataStackObjectDefinition(String objectID, String owner, BigDataStackObjectType type,
			String yamlSource, Set<String> status) {
		super();
		this.objectID = objectID;
		this.owner = owner;
		this.type = type;
		this.yamlSource = yamlSource;
		this.status = status;
		instance = 0;
	}
	
	public BigDataStackObjectDefinition(String objectID, String owner, BigDataStackObjectType type,
			String yamlSource, Set<String> status, int instance) {
		super();
		this.objectID = objectID;
		this.owner = owner;
		this.type = type;
		this.yamlSource = yamlSource;
		this.status = status;
		this.instance = instance;
	}
	
	public String getObjectID() {
		return objectID;
	}
	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public BigDataStackObjectType getType() {
		return type;
	}
	public void setType(BigDataStackObjectType type) {
		this.type = type;
	}
	public String getYamlSource() {
		return yamlSource;
	}
	public void setYamlSource(String yamlSource) {
		this.yamlSource = yamlSource;
	}

	public Set<String> getStatus() {
		return status;
	}

	public void setStatus(Set<String> status) {
		this.status = status;
	}
	
	public int getInstance() {
		return instance;
	}

	public void setInstance(int instance) {
		this.instance = instance;
	}

	public BigDataStackObjectDefinition clone() {
		return new BigDataStackObjectDefinition(objectID, owner, type,
			yamlSource, status, instance);
	}
	
	
	
	
}
