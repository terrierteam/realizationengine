package eu.bigdatastack.gdt.structures.data;

import java.util.List;

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
