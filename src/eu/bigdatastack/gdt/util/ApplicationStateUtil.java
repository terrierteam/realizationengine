package eu.bigdatastack.gdt.util;

import java.util.ArrayList;
import java.util.List;

import eu.bigdatastack.gdt.lxdb.BigDataStackAppStateIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackOperationSequenceIO;
import eu.bigdatastack.gdt.structures.data.BigDataStackAppState;
import eu.bigdatastack.gdt.structures.data.BigDataStackAppStateCondition;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;

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
				
				if (!inNegativeState && positiveMatchStates.size()==1) {
					activeStates.add(possibleState);
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
