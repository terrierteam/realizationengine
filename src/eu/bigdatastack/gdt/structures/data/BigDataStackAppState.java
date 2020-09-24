package eu.bigdatastack.gdt.structures.data;

import java.util.ArrayList;
import java.util.List;

public class BigDataStackAppState {

	String appID;
	String owner;
	String namespace;
	
	String appStateID;
	String name;
	List<String> notInStates;
	List<String> sequences;
	List<BigDataStackAppStateCondition> conditions;
	
	public BigDataStackAppState() {
		notInStates = new ArrayList<String>(0);
		sequences = new ArrayList<String>(0);
		conditions = new ArrayList<BigDataStackAppStateCondition>(0);
	}
	
	

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
