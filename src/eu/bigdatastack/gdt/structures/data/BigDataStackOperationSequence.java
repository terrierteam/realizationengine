package eu.bigdatastack.gdt.structures.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.bigdatastack.gdt.operations.BigDataStackOperation;
import eu.bigdatastack.gdt.operations.BigDataStackOperationState;

/**
 * This represents a sequence of operations that can be executed on the openshift cluster.
 * THis is used for queueing up the steps needed to deploy the user application.
 * @author EbonBlade
 *
 */
public class BigDataStackOperationSequence {

	private String appID;
	private String owner;
	private String namepace;
	private int index;
	
	private String sequenceID;
	private String name;
	private String description;
	
	private Map<String,String> parameters;
	private List<BigDataStackOperation> operations;
	BigDataStackOperationSequenceMode mode;

	public BigDataStackOperationSequence() {}
	
	public BigDataStackOperationSequence(String appID, String owner, String namepace, int index, String sequenceID, String name,
			String description, List<BigDataStackOperation> operations, BigDataStackOperationSequenceMode mode) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.index = index;
		this.sequenceID = sequenceID;
		this.name = name;
		this.description = description;
		this.parameters = new HashMap<String,String>();
		this.operations = operations;
		this.mode = mode;
	}

	public BigDataStackOperationSequence(String appID, String owner, String namepace, int index, String sequenceID, String name,
			String description, Map<String, String> parameters, List<BigDataStackOperation> operations, BigDataStackOperationSequenceMode mode) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.index = index;
		this.sequenceID = sequenceID;
		this.name = name;
		this.description = description;
		this.parameters = parameters;
		this.operations = operations;
		this.mode = mode;
	}
	
	public BigDataStackOperationSequence(String appID, String owner, String namepace, String sequenceID, String name,
			String description, List<BigDataStackOperation> operations, BigDataStackOperationSequenceMode mode) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.index = 0;
		this.sequenceID = sequenceID;
		this.name = name;
		this.description = description;
		this.parameters = new HashMap<String,String>();
		this.operations = operations;
		this.mode = mode;
	}

	public BigDataStackOperationSequence(String appID, String owner, String namepace, String sequenceID, String name,
			String description, Map<String, String> parameters, List<BigDataStackOperation> operations,BigDataStackOperationSequenceMode mode) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.index = 0;
		this.sequenceID = sequenceID;
		this.name = name;
		this.description = description;
		this.parameters = parameters;
		this.operations = operations;
		this.mode = mode;
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
		return namepace;
	}

	public void setNamespace(String namepace) {
		this.namepace = namepace;
	}

	public List<BigDataStackOperation> getOperations() {
		return operations;
	}

	public void setOperations(List<BigDataStackOperation> operations) {
		this.operations = operations;
	}
	
	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public String getSequenceID() {
		return sequenceID;
	}

	public void setSequenceID(String sequenceID) {
		this.sequenceID = sequenceID;
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

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public BigDataStackOperationSequenceMode getMode() {
		return mode;
	}

	public void setMode(BigDataStackOperationSequenceMode mode) {
		this.mode = mode;
	}

	public String getNamepace() {
		return namepace;
	}

	public void setNamepace(String namepace) {
		this.namepace = namepace;
	}

	/**
	 * Get the currently active operation. Returns null if the sequence has not started, or the last
	 * operation if all have been completed.
	 * @return
	 */
	public BigDataStackOperation getCurrentOperation() {
		
		boolean hasStarted = false;
		
		for (BigDataStackOperation operation : operations) {
			
			if (operation.getState() != BigDataStackOperationState.NotStarted) hasStarted = true;
			if (operation.getState() == BigDataStackOperationState.InProgress) return operation;
		}
		
		if (!hasStarted) return null;
		
		return operations.get(operations.size()-1);
		
		
	}
	
	public BigDataStackOperationSequence clone() {
		return new BigDataStackOperationSequence(appID, owner, namepace, index, sequenceID, name,
				description, parameters, operations, mode);
	}
	
	
	
}
