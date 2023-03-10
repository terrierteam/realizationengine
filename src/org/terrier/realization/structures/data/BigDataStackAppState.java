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
public class BigDataStackAppState {

	String appID;
	String owner;
	String namespace;
	
	String appStateID;
	String name;
	List<String> notInStates;
	List<String> sequences;
	List<BigDataStackAppStateCondition> conditions;
	
	public BigDataStackAppState() {}

	public BigDataStackAppState(String appID, String owner, String namespace, String appStateID, String name,
			List<String> notInStates, List<String> sequences, List<BigDataStackAppStateCondition> conditions) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namespace = namespace;
		this.appStateID = appStateID;
		this.name = name;
		this.notInStates = notInStates;
		this.sequences = sequences;
		this.conditions = conditions;
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



	public String getAppStateID() {
		return appStateID;
	}

	public void setAppStateID(String appStateID) {
		this.appStateID = appStateID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getNotInStates() {
		return notInStates;
	}

	public void setNotInStates(List<String> notInStates) {
		this.notInStates = notInStates;
	}

	public List<String> getSequences() {
		return sequences;
	}

	public void setSequences(List<String> sequences) {
		this.sequences = sequences;
	}

	public List<BigDataStackAppStateCondition> getConditions() {
		return conditions;
	}

	public void setConditions(List<BigDataStackAppStateCondition> conditions) {
		this.conditions = conditions;
	}
	
	

}
