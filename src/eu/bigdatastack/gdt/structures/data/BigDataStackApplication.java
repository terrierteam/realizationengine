package eu.bigdatastack.gdt.structures.data;

import java.util.List;

/**
 * Represents a BigDataStack deployable application
 * @author EbonBlade
 *
 */
public class BigDataStackApplication {

	private String appID;
	private String name;
	private String description;
	private String owner;
	private String namespace;
	private List<BigDataStackApplicationType> types;
	
	public BigDataStackApplication() {}
	
	public BigDataStackApplication(String appID, String name, String description, String owner, String namespace,
			List<BigDataStackApplicationType> types) {
		super();
		this.appID = appID;
		this.name = name;
		this.description = description;
		this.owner = owner;
		this.namespace = namespace;
		this.types = types;
	}
	public String getAppID() {
		return appID;
	}
	public void setAppID(String appID) {
		this.appID = appID;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
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
	public List<BigDataStackApplicationType> getTypes() {
		return types;
	}
	public void setTypes(List<BigDataStackApplicationType> types) {
		this.types = types;
	}
	
	
	
	
}
