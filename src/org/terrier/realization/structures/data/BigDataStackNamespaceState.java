package org.terrier.realization.structures.data;

 /*
 * Realization Engine 
 * Webpage: https://github.com/terrierteam/realizationengine
 * Contact: richard.mccreadie@glasgow.ac.uk
 * University of Glasgow - School of Computing Science
 * http://www.gla.ac.uk/
 * 
 * The contents of this file are subject to the Apache License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 *
 * The Original Code is Copyright (C) to the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richard.mccreadie@glasgow.ac.uk> (original author)
 */

public class BigDataStackNamespaceState {

	String namespace;
	String host;
	int port;
	
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
	
	public BigDataStackNamespaceState(String namespace, String host, int port) {
		this.namespace =namespace;
		this.host = host;
		this.port = port;
		clusterMonitoringActive = false;
		clusterMonitoringHost = "";
		clusterMonitoringPort = -1;
		metricStoreActive = false;
		metricStoreHost = "";
		metricStorePort = -1;
		logSearchActive = false;
		logSearchHost = "";
		logSearchPort = -1;
		eventExchangeActive = false;
		eventExchangeHost = "";
		eventExchangePort = -1;
	}
	
	

	public BigDataStackNamespaceState(String namespace, String host, int port, boolean clusterMonitoringActive, String clusterMonitoringHost,
			int clusterMonitoringPort, boolean metricStoreActive, String metricStoreHost, int metricStorePort,
			boolean logSearchActive, String logSearchHost, int logSearchPort, boolean eventExchangeActive,
			String eventExchangeHost, int eventExchangePort) {
		super();
		this.namespace = namespace;
		this.host = host;
		this.port = port;
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

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	
}
