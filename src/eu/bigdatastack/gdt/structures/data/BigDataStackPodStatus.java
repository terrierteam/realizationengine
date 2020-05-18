package eu.bigdatastack.gdt.structures.data;

/**
 * This represents the current state of Pod running on the cluster.
 * @author EbonBlade
 *
 */
public class BigDataStackPodStatus {

	
	private String appID;
	private String owner;
	private String namespace;
	private String objectID;
	private int instance;
	
	private String podID;
	private String status;
	private String hostIP;
	private String podIP;
	
	
	public BigDataStackPodStatus() {}
	
	public BigDataStackPodStatus(String appID, String owner, String namespace, String objectID, int instance, String podID,
			String status, String hostIP, String podIP) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namespace = namespace;
		this.objectID = objectID;
		this.podID = podID;
		this.status = status;
		this.hostIP = hostIP;
		this.podIP = podIP;
		this.instance =instance;
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
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	public String getPodID() {
		return podID;
	}
	public void setPodID(String podID) {
		this.podID = podID;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getHostIP() {
		return hostIP;
	}
	public void setHostIP(String hostIP) {
		this.hostIP = hostIP;
	}
	public String getPodIP() {
		return podIP;
	}
	public void setPodIP(String podIP) {
		this.podIP = podIP;
	}

	public int getInstance() {
		return instance;
	}

	public void setInstance(int instance) {
		this.instance = instance;
	}
	
	
	
	
}
