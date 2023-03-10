package org.terrier.realization.structures.data;

import java.util.ArrayList;
import java.util.List;

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
 * Represents a BigDataStack deployable application
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
	
	public BigDataStackApplication(String appID, String name, String description, String owner, String namespace) {
		super();
		this.appID = appID;
		this.name = name;
		this.description = description;
		this.owner = owner;
		this.namespace = namespace;
		this.types = new ArrayList<BigDataStackApplicationType>();
		types.add(BigDataStackApplicationType.inferMissingValues);
		types.add(BigDataStackApplicationType.overrideValues);
		types.add(BigDataStackApplicationType.setObjectMetadata);
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
