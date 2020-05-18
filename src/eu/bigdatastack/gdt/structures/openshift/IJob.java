package eu.bigdatastack.gdt.structures.openshift;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This is an implementation of a Job in Openshift. It is extending the Openshift rest client, since it
 * does not have one of these for some reason.
 * 
 * In practice we turn an IResource into one of these.
 * 
 * @author richardm
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
