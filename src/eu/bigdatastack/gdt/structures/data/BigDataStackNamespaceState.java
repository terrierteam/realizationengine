package eu.bigdatastack.gdt.structures.data;

public class BigDataStackNamespaceState {

	String namespace;
	
	boolean clusterMonitoringActive;
	String clusterMonitoringHost;
	int clusterMonitoringPort;
	
	boolean metricStoreActive;
	String metricStoreHost;
	int metricStorePort;
	
	boolean logSearchActive;
	String logSearchHost;
	int logSearchPort;
	
	boolean eventExchangeActive;
	String eventExchangeHost;
	int eventExchangePort;
	
	public BigDataStackNamespaceState() {}
	
	public BigDataStackNamespaceState(String namespace) {
		this.namespace =namespace;
		clusterMonitoringActive = false;
		clusterMonitoringHost = null;
		clusterMonitoringPort = -1;
		metricStoreActive = false;
		metricStoreHost = null;
		metricStorePort = -1;
		logSearchActive = false;
		logSearchHost = null;
		logSearchPort = -1;
		eventExchangeActive = false;
		eventExchangeHost = null;
		eventExchangePort = -1;
	}
	
	

	public BigDataStackNamespaceState(String namespace, boolean clusterMonitoringActive, String clusterMonitoringHost,
			int clusterMonitoringPort, boolean metricStoreActive, String metricStoreHost, int metricStorePort,
			boolean logSearchActive, String logSearchHost, int logSearchPort, boolean eventExchangeActive,
			String eventExchangeHost, int eventExchangePort) {
		super();
		this.namespace = namespace;
		this.clusterMonitoringActive = clusterMonitoringActive;
		this.clusterMonitoringHost = clusterMonitoringHost;
		this.clusterMonitoringPort = clusterMonitoringPort;
		this.metricStoreActive = metricStoreActive;
		this.metricStoreHost = metricStoreHost;
		this.metricStorePort = metricStorePort;
		this.logSearchActive = logSearchActive;
		this.logSearchHost = logSearchHost;
		this.logSearchPort = logSearchPort;
		this.eventExchangeActive = eventExchangeActive;
		this.eventExchangeHost = eventExchangeHost;
		this.eventExchangePort = eventExchangePort;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public boolean isClusterMonitoringActive() {
		return clusterMonitoringActive;
	}

	public void setClusterMonitoringActive(boolean clusterMonitoringActive) {
		this.clusterMonitoringActive = clusterMonitoringActive;
	}

	public String getClusterMonitoringHost() {
		return clusterMonitoringHost;
	}

	public void setClusterMonitoringHost(String clusterMonitoringHost) {
		this.clusterMonitoringHost = clusterMonitoringHost;
	}

	public int getClusterMonitoringPort() {
		return clusterMonitoringPort;
	}

	public void setClusterMonitoringPort(int clusterMonitoringPort) {
		this.clusterMonitoringPort = clusterMonitoringPort;
	}

	public boolean isMetricStoreActive() {
		return metricStoreActive;
	}

	public void setMetricStoreActive(boolean metricStoreActive) {
		this.metricStoreActive = metricStoreActive;
	}

	public String getMetricStoreHost() {
		return metricStoreHost;
	}

	public void setMetricStoreHost(String metricStoreHost) {
		this.metricStoreHost = metricStoreHost;
	}

	public int getMetricStorePort() {
		return metricStorePort;
	}

	public void setMetricStorePort(int metricStorePort) {
		this.metricStorePort = metricStorePort;
	}

	public boolean isLogSearchActive() {
		return logSearchActive;
	}

	public void setLogSearchActive(boolean logSearchActive) {
		this.logSearchActive = logSearchActive;
	}

	public String getLogSearchHost() {
		return logSearchHost;
	}

	public void setLogSearchHost(String logSearchHost) {
		this.logSearchHost = logSearchHost;
	}

	public int getLogSearchPort() {
		return logSearchPort;
	}

	public void setLogSearchPort(int logSearchPort) {
		this.logSearchPort = logSearchPort;
	}

	public boolean isEventExchangeActive() {
		return eventExchangeActive;
	}

	public void setEventExchangeActive(boolean eventExchangeActive) {
		this.eventExchangeActive = eventExchangeActive;
	}

	public String getEventExchangeHost() {
		return eventExchangeHost;
	}

	public void setEventExchangeHost(String eventExchangeHost) {
		this.eventExchangeHost = eventExchangeHost;
	}

	public int getEventExchangePort() {
		return eventExchangePort;
	}

	public void setEventExchangePort(int eventExchangePort) {
		this.eventExchangePort = eventExchangePort;
	}
	
	
}
