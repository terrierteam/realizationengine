package org.terrier.realization.util;

import java.util.ArrayList;
import java.util.List;

import org.terrier.realization.state.jdbc.BigDataStackAppStateIO;
import org.terrier.realization.state.jdbc.BigDataStackObjectIO;
import org.terrier.realization.state.jdbc.BigDataStackOperationSequenceIO;
import org.terrier.realization.structures.data.BigDataStackAppState;
import org.terrier.realization.structures.data.BigDataStackAppStateCondition;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackOperationSequence;

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
 * Provides functions to check the state of an application against the possible
 * states registered with it. This is used to determine what sequences are currently
 * valid to run in the current state.
 * @author EbonBlade
 *
 */
public class ApplicationStateUtil {

	
	/**
	 * Gets the states that a specified app can be in and then validates each against the current state of the app
	 * on the cluster, returning all currently matching states.
	 * @param manager
	 * @param owner
	 * @param namespace
	 * @param appID
	 * @return
	 */
	public static List<BigDataStackAppState> getActiveStates(BigDataStackObjectIO objectInstanceClient, BigDataStackOperationSequenceIO sequenceInstanceClient, BigDataStackAppStateIO appStateClient, String owner, String namespace, String appID) {
		
		List<BigDataStackAppState> activeStates = new ArrayList<BigDataStackAppState>();
		
		
		try {
			List<BigDataStackAppState> positiveMatchStates = new ArrayList<BigDataStackAppState>();
			
			
			// Step 1: Get Possible States
			List<BigDataStackAppState> allStates = appStateClient.getAppStates(owner, appID, namespace, null);
			
		
			// Step 2: Validate Possible States
			for (BigDataStackAppState possibleState : allStates) {
				boolean conditionsMet  = true;
				
				List<BigDataStackAppStateCondition> conditions = possibleState.getConditions();
				
				for (BigDataStackAppStateCondition condition : conditions) {
					List<String> objectIDsToCheck = condition.getObjectIDs();
					
					if (objectIDsToCheck!=null ) {
						for (String objectID : objectIDsToCheck) {
							List<BigDataStackObjectDefinition> objectInstances = objectInstanceClient.getObjects(objectID, owner, namespace, appID);
							
							if (!validateObjectSet(condition, objectInstances)) {
								conditionsMet = false;
								break;
							}
						}
					}
					
					String sequenceID = condition.getSequenceID();
					
					if (sequenceID!=null) {
						List<BigDataStackOperationSequence> sequences = sequenceInstanceClient.getOperationSequences(appID, sequenceID);
						
						if (!validateSequenceSet(condition, sequences)) {
							conditionsMet = false;
							break;
						}
							
					}
					
				}
				
				if (conditionsMet) {
					positiveMatchStates.add(possibleState);
				}
				
			}
			
			
			// Step 3: Check Negative States
			
			for (BigDataStackAppState possibleState : positiveMatchStates) {
				List<String> notInStates = possibleState.getNotInStates();
				
				boolean inNegativeState = false;
				for (BigDataStackAppState checkState : positiveMatchStates) {
					if (notInStates.contains(checkState.getAppStateID())) inNegativeState=true;
				}
				
				if (!inNegativeState) {
					activeStates.add(possibleState);
				}
			}
			
			if (activeStates.size()==0) {
				for (BigDataStackAppState possibleState : allStates) {
					List<String> notInStates = possibleState.getNotInStates();
					if (notInStates.contains("anyOther")) activeStates.add(possibleState);
				}
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		return activeStates;
	}
	
	
	public static boolean validateObjectSet(BigDataStackAppStateCondition condition, List<BigDataStackObjectDefinition> objectInstances) {
		
		// Perform state check
		int numInTargetState = 0;
		if (condition.getState()!=null) {
			for (BigDataStackObjectDefinition objectInstance : objectInstances) {
				if (objectInstance.getStatus().contains(condition.getState())) numInTargetState++;
			}
		} else {
			numInTargetState = objectInstances.size();
		}
		
		
		// Perform instance count check
		if (condition.getInstances()!=null) {
			
			String comparatorSymbol = condition.getInstances().substring(0, 2);
			double value = Double.parseDouble(condition.getInstances().substring(2, condition.getInstances().length()));
			
			if (comparatorSymbol.equalsIgnoreCase("==")) return numInTargetState==value;
			else if (comparatorSymbol.equalsIgnoreCase(">=")) return numInTargetState>value;
			else if (comparatorSymbol.equalsIgnoreCase("<=")) return numInTargetState<value;
			else {
				System.err.println("ERR: Unknown comparitor symbol");
				return false;
			}
		}
		
		
		
		return true;
	}
	
	public static boolean validateSequenceSet(BigDataStackAppStateCondition condition, List<BigDataStackOperationSequence> sequences) {
		
		// Perform state check
		int numInTargetState = 0;
		if (condition.getState()!=null) {
			for (BigDataStackOperationSequence sequence : sequences) {
				if (condition.getState()=="Instantiated" && !sequence.isInProgress() && !sequence.isInProgress()) numInTargetState++;
				if (condition.getState()=="InProgress" && sequence.isInProgress()) numInTargetState++;
				if (condition.getState()=="Complete" && sequence.isComplete()) numInTargetState++;
			}
		} else {
			numInTargetState = sequences.size();
		}
		
		int numNotInListedState = 0;
		if (condition.getNotInState()!=null) {
			for (BigDataStackOperationSequence sequence : sequences) {
				boolean conditionFailed = false;
				for (String state : condition.getNotInState()) {
					
					
					if (state=="Instantiated" && !sequence.isInProgress() && !sequence.isInProgress()) conditionFailed=true; 
					if (state=="InProgress" && sequence.isInProgress()) conditionFailed=true; 
					if (state=="Complete" && sequence.isComplete()) conditionFailed=true;
					
				}
				if (conditionFailed) {
					numNotInListedState++;
					break;
				}
			}
		}
		
		if (numNotInListedState>=0) return false;
		
		if (condition.getInstances()!=null) {
			
			String comparatorSymbol = condition.getInstances().substring(0, 1);
			double value = Double.parseDouble(condition.getInstances().substring(1, condition.getInstances().length()));
			
			if (comparatorSymbol=="=" || comparatorSymbol=="==") return numInTargetState==value;
			if (comparatorSymbol==">") return numInTargetState>value;
			if (comparatorSymbol=="<") return numInTargetState<value;
			if (comparatorSymbol==">=") return numInTargetState>=value;
			if (comparatorSymbol=="<=") return numInTargetState<=value;
		}
		
		
		return true;
	}
	
	
}
