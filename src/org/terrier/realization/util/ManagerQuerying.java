package org.terrier.realization.util;

import java.util.ArrayList;
import java.util.List;

import org.terrier.realization.application.GDTManager;
import org.terrier.realization.structures.data.BigDataStackAppState;
import org.terrier.realization.structures.data.BigDataStackApplication;
import org.terrier.realization.structures.data.BigDataStackEvent;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.structures.data.BigDataStackMetric;
import org.terrier.realization.structures.data.BigDataStackMetricValue;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackObjectType;
import org.terrier.realization.structures.data.BigDataStackOperationSequence;
import org.terrier.realization.structures.data.BigDataStackPodStatus;
import org.terrier.realization.structures.data.BigDataStackSLO;


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
 * This class provides utility methods for querying the state of the application space
 * via the GDTManager. This functionality could have been directly part of GDTManager, but
 * the class was getting too long.
 * @author EbonBlade
 *
 */
public class ManagerQuerying {

	GDTManager manager;
	
	public ManagerQuerying(GDTManager manager) {
		this.manager = manager;
	}
	
	/**
	 * Gets all applications for the specified user
	 * @param owner
	 * @return
	 */
	public List<BigDataStackApplication> listApplications(String owner){
		try {
			return manager.appClient.getApplications(owner);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets all object templates for a specified owner and application of a specified type
	 * @param owner
	 * @return
	 */
	public List<BigDataStackObjectDefinition> listObjectTemplates(String owner, String appID, BigDataStackObjectType type){
		try {
			return manager.objectTemplateClient.getObjectList(owner, null, appID, type);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets all object instances for a specified owner and application of a specified type
	 * @param owner
	 * @return
	 */
	public List<BigDataStackObjectDefinition> listObjectInstances(String owner, String appID, BigDataStackObjectType type){
		try {
			return manager.objectInstanceClient.getObjectList(owner, null, appID, type);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets all states that are registered for an application
	 * @param owner
	 * @return
	 */
	public List<BigDataStackAppState> listApplicationPossibleStates(String owner, String appID){
		try {
			return manager.appStateClient.getAppStates(owner, appID, null, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Gets all object instances for a specified owner, application and objectID
	 * @param owner
	 * @return
	 */
	public List<BigDataStackObjectDefinition> listObjectInstances(String owner, String appID, String objectID){
		try {
			return manager.objectInstanceClient.getObjects(objectID, owner, null, appID);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets all slo instances for a specified owner, application, objectID and metric
	 * @param owner
	 * @return
	 */
	public List<BigDataStackSLO> listSLOInstances(String owner, String appID, String triggerID, String metricName){
		try {
			return manager.sloClient.getSLOs(owner, appID, triggerID);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets all slo instances for a specified owner, application, objectID, instance and metric
	 * @param owner
	 * @return
	 */
	public List<BigDataStackSLO> listSLOInstances(String owner, String appID, String triggerID, int objectInstance, String metricName){
		try {
			return manager.sloClient.getSLOs(owner, appID, triggerID);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets a specified object instance
	 * @param owner
	 * @return
	 */
	public BigDataStackObjectDefinition getObjectInstance(String owner, String appID, String objectID, int instance){
		try {
			return manager.objectInstanceClient.getObject(objectID, owner, instance);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets a specified object template
	 * @param owner
	 * @return
	 */
	public BigDataStackObjectDefinition getObjectTemplate(String owner, String appID, String objectID){
		try {
			return manager.objectTemplateClient.getObject(objectID, owner);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets all operation sequence instances for a specified owner and application
	 * @param owner
	 * @return
	 */
	public List<BigDataStackOperationSequence> listOperationSequenceTemplates(String owner, String appID){
		try {
			return manager.sequenceTemplateClient.getOperationSequences(owner, appID, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets all operation sequence instances for a specified owner and application
	 * @param owner
	 * @return
	 */
	public List<BigDataStackOperationSequence> listOperationSequenceInstances(String owner, String appID){
		try {
			return manager.sequenceInstanceClient.getOperationSequences(owner, appID, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets all instances of a specified operation sequence
	 * @param owner
	 * @return
	 */
	public List<BigDataStackOperationSequence> listOperationSequenceInstances(String owner, String appID, String sequenceID){
		try {
			return manager.sequenceInstanceClient.getOperationSequences(owner, appID, sequenceID);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets all instances of a specified operation sequence
	 * @param owner
	 * @return
	 */
	public List<BigDataStackOperationSequence> listOperationSequenceInstancesByState(String owner, String appID, String sequenceState){
		try {
			List<BigDataStackOperationSequence> allAppSequences = manager.sequenceInstanceClient.getOperationSequences(owner, appID, null);
			List<BigDataStackOperationSequence> matchedSequences  = new ArrayList<BigDataStackOperationSequence>();
			
			
			
			for (BigDataStackOperationSequence sequence : allAppSequences) {
				
				System.err.println(appID+" "+sequence.getSequenceID()+" "+sequenceState+":"+sequence.isPending()+"/"+sequence.isInProgress()+"/"+sequence.hasFailed()+"/"+sequence.isComplete());
				
				if (sequenceState.equalsIgnoreCase("Running") && sequence.isInProgress()) matchedSequences.add(sequence);
				else if (sequenceState.equalsIgnoreCase("Complete") && sequence.isComplete()) matchedSequences.add(sequence);
				else if (sequenceState.equalsIgnoreCase("Pending") && sequence.isPending()) matchedSequences.add(sequence);
				else if (sequenceState.equalsIgnoreCase("Failed") && sequence.hasFailed()) matchedSequences.add(sequence);
			}
			
			return matchedSequences;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets a specified operation sequence instance
	 * @param owner
	 * @return
	 */
	public BigDataStackOperationSequence getOperationSequenceInstance(String owner, String appID, String sequenceID, int instance){
		try {
			return manager.sequenceInstanceClient.getOperationSequence(appID, sequenceID, instance, owner);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets a specified operation sequence template
	 * @param owner
	 * @return
	 */
	public BigDataStackOperationSequence getOperationSequenceTemplate(String owner, String appID, String sequenceID){
		try {
			return manager.sequenceTemplateClient.getSequence(appID, sequenceID);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets the status for all pods associated to an object instance
	 * @param owner
	 * @return
	 */
	public List<BigDataStackPodStatus> listPodStatuses(String owner, String objectID){
		try {
			return manager.podStatusClient.getPodStatuses(null, owner, objectID, null, -1);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets the list of metrics registered by an owner (raw metric definitions are not tied to an app)
	 * @param owner
	 * @return
	 */
	public List<BigDataStackMetric> listMetrics(String owner){
		try {
			return manager.metricClient.listMetrics(owner, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets a named metric registered by an owner (raw metric definitions are not tied to an app)
	 * @param owner
	 * @return
	 */
	public BigDataStackMetric getMetric(String owner, String metricName){
		try {
			return manager.metricClient.getMetric(owner, metricName);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Lists all metric values stored in the DB for an owner, app, object and metric
	 * @param owner
	 * @return
	 */
	public List<BigDataStackMetricValue> listMetricValues(String owner, String appID, String metricName, String objectID){
		try {
			return manager.metricValueClient.getMetricValues(appID, owner, null, objectID, metricName);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Lists all events for an app
	 * @param owner
	 * @return
	 */
	public List<BigDataStackEvent> listEvents(String owner, String appID){
		try {
			return manager.eventClient.getEvents(appID, owner);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Lists all events for an owner
	 * @param owner
	 * @return
	 */
	public List<BigDataStackEvent> listEvents(String owner){
		try {
			return manager.eventClient.getEvents(null, owner);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Lists all events for an owner
	 * @param owner
	 * @return
	 */
	public List<BigDataStackEvent> listEvents(String owner, BigDataStackEventType type){
		try {
			return manager.eventClient.getEvents(null, owner, type);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Lists all events for an owner
	 * @param owner
	 * @return
	 */
	public List<BigDataStackEvent> listEvents(String owner, BigDataStackEventSeverity severity){
		try {
			return manager.eventClient.getEvents(null, owner, severity);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Lists all events for an owner
	 * @param owner
	 * @return
	 */
	public List<BigDataStackEvent> listEvents(String owner, BigDataStackEventSeverity severity, BigDataStackEventType type){
		try {
			return manager.eventClient.getEvents(null, owner, type, severity );
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Lists all events for an app and object
	 * @param owner
	 * @return
	 */
	public List<BigDataStackEvent> listEvents(String owner, String appID, String objectID, int instance){
		List<BigDataStackEvent> events = new ArrayList<BigDataStackEvent>();
		try {
			for (BigDataStackEvent event : manager.eventClient.getEvents(appID, owner)) {
				if (event.getObjectID().equalsIgnoreCase(objectID) && event.getInstance()==instance) events.add(event);
			}
			return events;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	
}
