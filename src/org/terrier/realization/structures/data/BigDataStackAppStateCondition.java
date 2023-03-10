package org.terrier.realization.structures.data;

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

public class BigDataStackAppStateCondition {

	List<String> objectIDs;
	String instances;
	String state;
	
	String sequenceID;
	List<String> notInState;
	
	public BigDataStackAppStateCondition() {}
	
	public BigDataStackAppStateCondition(List<String> objectIDs, String instances, String state, String sequenceID,
			List<String> notInState) {
		super();
		this.objectIDs = objectIDs;
		this.instances = instances;
		this.state = state;
		this.sequenceID = sequenceID;
		this.notInState = notInState;
	}

	public List<String> getObjectIDs() {
		return objectIDs;
	}

	public void setObjectIDs(List<String> objectIDs) {
		this.objectIDs = objectIDs;
	}

	public String getInstances() {
		return instances;
	}

	public void setInstances(String instances) {
		this.instances = instances;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getSequenceID() {
		return sequenceID;
	}

	public void setSequenceID(String sequenceID) {
		this.sequenceID = sequenceID;
	}

	public List<String> getNotInState() {
		return notInState;
	}

	public void setNotInState(List<String> notInState) {
		this.notInState = notInState;
	}
	
	
}
