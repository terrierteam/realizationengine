package org.terrier.realization.structures.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.terrier.realization.operations.BigDataStackOperation;
import org.terrier.realization.operations.BigDataStackOperationState;

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
 * This represents a sequence of operations that can be executed on the openshift cluster.
 * THis is used for queueing up the steps needed to deploy the user application.
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
	private BigDataStackOperationSequenceMode mode;
	
	private String onFailDo = null;
	private String onSuccessDo = null;

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

	public String getOnFailDo() {
		return onFailDo;
	}

	public void setOnFailDo(String onFailDo) {
		this.onFailDo = onFailDo;
	}

	public String getOnSuccessDo() {
		return onSuccessDo;
	}

	public void setOnSuccessDo(String onSuccessDo) {
		this.onSuccessDo = onSuccessDo;
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
	
	public int getCurrentOperationNo() {
		
		boolean hasStarted = false;
		
		int i =1;
		for (BigDataStackOperation operation : operations) {
			
			if (operation.getState() != BigDataStackOperationState.NotStarted) hasStarted = true;
			if (operation.getState() == BigDataStackOperationState.InProgress) return i;
			
			i++;
		}
		
		if (!hasStarted) return 1;
		
		return operations.size();
		
		
	}
	
	/**
	 * Checks if all operations are marked as complete
	 * @return
	 */
	public boolean isComplete() {

		for (BigDataStackOperation operation : operations) {
			if (operation.getState() != BigDataStackOperationState.Completed) return false;
		}
		
		return true;
		
	}
	
	/**
	 * Checks if all operations are marked as complete
	 * @return
	 */
	public boolean isInProgress() {

		for (BigDataStackOperation operation : operations) {
			if (operation.getState() == BigDataStackOperationState.InProgress) return true;
		}
		
		return false;
		
	}
	
	/**
	 * Checks if all operations are marked as not started
	 * @return
	 */
	public boolean isPending() {

		for (BigDataStackOperation operation : operations) {
			if (operation.getState() != BigDataStackOperationState.NotStarted) return false;
		}
		
		return true;
		
	}
	
	/**
	 * Checks if all operations are marked as not started
	 * @return
	 */
	public boolean hasFailed() {

		for (BigDataStackOperation operation : operations) {
			if (operation.getState() == BigDataStackOperationState.Failed) return true;
		}
		
		return false;
		
	}
	
	public BigDataStackOperationSequence clone() {
		BigDataStackOperationSequence seq =  new BigDataStackOperationSequence(appID, owner, namepace, index, sequenceID, name,
				description, parameters, operations, mode);
		
		seq.setOnFailDo(onFailDo);
		seq.setOnSuccessDo(onSuccessDo);
		
		
		return seq;
	}
	
	
	
}
