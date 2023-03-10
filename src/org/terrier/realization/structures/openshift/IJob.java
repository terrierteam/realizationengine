package org.terrier.realization.structures.openshift;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
 * This is an implementation of a Job in Openshift. It is extending the Openshift rest client, since it
 * does not have one of these for some reason.
 * 
 * In practice we turn an IResource into one of these.
 *
 */
public class IJob {

	IMetaData metadata;
	ISpec spec;
	ObjectNode status;

	
	public IJob() {}


	public IMetaData getMetadata() {
		return metadata;
	}


	public void setMetadata(IMetaData metadata) {
		this.metadata = metadata;
	}


	public ISpec getSpec() {
		return spec;
	}


	public void setSpec(ISpec spec) {
		this.spec = spec;
	}


	public ObjectNode getStatus() {
		return status;
	}


	public void setStatus(ObjectNode status) {
		this.status = status;
	}

	
	public Set<String> getJobStatuses() {
		
		Set<String> statuses = new HashSet<String>();
		if (status!=null && status.has("conditions")) {
			Iterator<JsonNode> conditionIterator = status.get("conditions").iterator();
			while (conditionIterator.hasNext()) {
				JsonNode condition = conditionIterator.next();
				statuses.add(condition.get("type").asText());
			}
		}
		
		if (statuses.size()==0) {
			if (status.has("active")) {
				int numActive = status.get("active").asInt();
				if (numActive>0) statuses.add("In Progress");
			} else statuses.add("Unknown");
		}
		
		return statuses;
	}

	
	
	
	
}
