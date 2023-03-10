package org.terrier.realization.structures.data;

import java.util.Set;

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
 * This is a representation of a BigDataStack/Kubernetes object definition, representing either a DeploymentConfig, 
 * Job, Service, Route, Volume, VolumeClaim, etc
 * 
 * These represent the initial definition of the object and will not be altered. 
 *
 */
public class BigDataStackObjectDefinition {

	private String objectID;
	private String owner;
	private String namespace;
	private String appID;
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
		this.namespace = "";
		instance = 0;
		this.appID = "";
	}
	
	public BigDataStackObjectDefinition(String objectID, String owner, BigDataStackObjectType type,
			String yamlSource, Set<String> status, int instance) {
		super();
		this.objectID = objectID;
		this.owner = owner;
		this.type = type;
		this.yamlSource = yamlSource;
		this.status = status;
		this.namespace = "";
		this.instance = instance;
		this.appID = "";
	}
	
	public BigDataStackObjectDefinition(String objectID, String owner, BigDataStackObjectType type,
			String yamlSource, Set<String> status, String namespace, String appID) {
		super();
		this.objectID = objectID;
		this.owner = owner;
		this.type = type;
		this.yamlSource = yamlSource;
		this.status = status;
		this.namespace = namespace;
		this.appID = appID;
		instance = 0;
	}
	
	public BigDataStackObjectDefinition(String objectID, String owner, BigDataStackObjectType type,
			String yamlSource, Set<String> status, int instance, String namespace, String appID) {
		super();
		this.objectID = objectID;
		this.owner = owner;
		this.type = type;
		this.yamlSource = yamlSource;
		this.status = status;
		this.namespace = namespace;
		this.instance = instance;
		this.appID = appID;
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

	public BigDataStackObjectDefinition clone() {
		return new BigDataStackObjectDefinition(objectID, owner, type,
			yamlSource, status, instance, namespace, appID);
	}
	
	
	
	
}
