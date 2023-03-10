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

/**
 * This represents the current state of Pod running on the cluster.
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
