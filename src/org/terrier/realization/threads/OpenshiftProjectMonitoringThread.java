package org.terrier.realization.threads;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.terrier.realization.openshift.OpenshiftObject;
import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.operations.BigDataStackOperationState;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.BigDataStackApplicationIO;
import org.terrier.realization.state.jdbc.BigDataStackEventIO;
import org.terrier.realization.state.jdbc.BigDataStackObjectIO;
import org.terrier.realization.state.jdbc.BigDataStackOperationSequenceIO;
import org.terrier.realization.state.jdbc.BigDataStackPodStatusIO;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.structures.data.BigDataStackApplication;
import org.terrier.realization.structures.data.BigDataStackEvent;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackObjectType;
import org.terrier.realization.structures.data.BigDataStackOperationSequence;
import org.terrier.realization.structures.data.BigDataStackPodStatus;
import org.terrier.realization.util.EventUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

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
 * This thread monitors the state of a project and reports status changes in pods.
 * In effect, this thread polls the Openshift API for a namespace and generates
 * BigDataStackEvent objects when a kubernetes/Openshift object is created or
 * changes state. These events are posted to a rabbitMQ topic, where they can be
 * used to trigger other operations. 
 *
 */
public class OpenshiftProjectMonitoringThread implements Runnable{

	OpenshiftStatusClient openshiftStatus;
	RabbitMQClient rabbitMQClient;
	JDBCDB database;
	String namespace;
	String owner;

	boolean kill = false;
	boolean failed = false;

	BigDataStackPodStatusIO podStatusIO;
	BigDataStackOperationSequenceIO operationSequenceIO;
	BigDataStackApplicationIO applicationIO;
	BigDataStackObjectIO objectIO;
	BigDataStackEventIO eventIO;
	EventUtil eventUtil;

	public OpenshiftProjectMonitoringThread(OpenshiftStatusClient openshiftStatus, RabbitMQClient rabbitMQClient, JDBCDB database, String owner, String namespace) {
		this.openshiftStatus = openshiftStatus;
		this.rabbitMQClient = rabbitMQClient;
		this.namespace = namespace;
		this.owner = owner;
		this.database = database;
	}

	@Override
	public void run() {

		try {
			// initalize database readers
			applicationIO = new BigDataStackApplicationIO(database);
			operationSequenceIO = new BigDataStackOperationSequenceIO(database,false); // monitor actual sequences, not templates
			podStatusIO = new BigDataStackPodStatusIO(database);
			eventIO = new BigDataStackEventIO(database);
			objectIO = new BigDataStackObjectIO(database, false); // monitor actual instances, not templates
			this.eventUtil = new EventUtil(eventIO, null);


		} catch (SQLException e) {
			e.printStackTrace();
			failed = true;
			return;
		}

		OpenshiftObject project = openshiftStatus.getProject(namespace); // here

		while (!kill) {

			try {

				List<BigDataStackApplication> applications = applicationIO.getApplications(owner);

				for (BigDataStackApplication app : applications) {

					List<BigDataStackObjectDefinition> objectInstances = objectIO.getObjectList(owner, namespace, app.getAppID(), null);

					for (BigDataStackObjectDefinition objectDef : objectInstances) {

						try {
							if (objectDef.getType() == BigDataStackObjectType.DeploymentConfig) processDeploymentConfig(project, app, objectDef);

							if (objectDef.getType() == BigDataStackObjectType.Job) processJob(project, app, objectDef);

							if (objectDef.getType() == BigDataStackObjectType.Pod) {
								if (objectDef.getObjectID().equalsIgnoreCase("operationsequence")) {
									
									if (objectDef.getStatus().contains("Failed")) {
										failStuckSequence(objectDef);
									}
									
									List<OpenshiftObject> pods = openshiftStatus.getPods(project.getName(), true, true, "operationsequence=True");
									for (OpenshiftObject pod : pods) {
										Map<String,String> labels = pod.getLabels();
										if (labels.get("runnerIndex").equalsIgnoreCase(String.valueOf(objectDef.getInstance())))
											updatePodStatus(project.getName(), app, objectDef, pod);
									}

								}
								
							}
						} catch (Exception e) {
							System.err.println(e.getMessage());
						}

					}

				}
				
				
				// now clean up pod statuses

			} catch (Exception e) {
				e.printStackTrace();
				failed = true;
				return;
			}

			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		// remember to close the thread pool used for communication with openshift
		openshiftStatus.close();

	}
	
	
	public void failStuckSequence(BigDataStackObjectDefinition objectDef) {
		try {
			
			ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
			JsonNode node = yamlMapper.readTree(objectDef.getYamlSource());
			JsonNode labels = node.get("metadata").get("labels");
			int sequenceInstance = Integer.parseInt(labels.get("sequenceInstance").asText());
			String sequenceID = labels.get("sequenceID").asText();
			
			BigDataStackOperationSequence sequence = operationSequenceIO.getOperationSequence(objectDef.getAppID(), sequenceID, sequenceInstance, owner);
			
			
			
			if (!sequence.hasFailed()) {
				sequence.getCurrentOperation().setState(BigDataStackOperationState.Failed);
				
				eventUtil.registerEvent(
						sequence.getAppID(),
						sequence.getOwner(),
						sequence.getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Operation Sequence Failed for: '"+sequence.getSequenceID()+"', detected a failure in the underlying runner",
						"The runner pod for sequence '"+sequence.getSequenceID()+"' with index '"+sequence.getIndex()+"' has failed, it may have been manually killed",
						sequence.getSequenceID(),
						sequence.getIndex()
						);
				
				operationSequenceIO.updateSequence(sequence);
			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void cleanUpPodsStatuses(BigDataStackApplication app, BigDataStackObjectDefinition object) {
		try {
			List<BigDataStackPodStatus> podStatuses = podStatusIO.getPodStatuses(null, app.getOwner(), object.getObjectID(), null, -1);
			for (BigDataStackPodStatus podStatus : podStatuses) {
				if (podStatus.getStatus().equalsIgnoreCase("Deleted")) continue;
				OpenshiftObject oobject = openshiftStatus.getPod(app.getNamespace(), podStatus.getPodID());
				if (oobject==null) {
					int previousEvents = eventIO.getEventCount(app.getAppID(), owner);

					BigDataStackEvent newEvent = new BigDataStackEvent(
							app.getAppID(),
							owner,
							previousEvents,
							namespace,
							BigDataStackEventType.Openshift,
							BigDataStackEventSeverity.Warning,
							"Pod '"+podStatus.getPodID()+"' was checked but was not found on the cluster",
							"Openshift project monitoring for '"+namespace+"' ran a scheduled check on '"+podStatus.getPodID()+"' connected to object '"+object.getObjectID()+"("+object.getInstance()+")', but did not find it, it may have been deleted, marking as such",
							object.getObjectID(),
							object.getInstance()
							);

					podStatus.setStatus("Deleted");

					podStatusIO.updatePodStatus(podStatus);
					eventIO.addEvent(newEvent);
					rabbitMQClient.publishEvent(newEvent);
				}
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	

	/**
	 * Call this to kill the thread
	 */
	public void kill() {
		kill = true;
	}

	/**
	 * If the thread has exited, you can use this to check whether it died
	 * due to an internal exception
	 * @return
	 */
	public boolean hasFailed() {
		return failed;
	}

	/**
	 * Checks and performs an update if needed for a specified object definition
	 * @param project
	 * @param app
	 * @param objectDef
	 * @throws Exception
	 */
	protected void processJob(OpenshiftObject project, BigDataStackApplication app, BigDataStackObjectDefinition objectDef) throws Exception {
		OpenshiftObject job = openshiftStatus.getJob(project.getName(), objectDef.getAppID()+"-"+objectDef.getObjectID()+"-"+objectDef.getInstance());
		if (job==null) {
			// This should have been running but was not. It may have been manually deleted. Report and perform Update
			if (!objectDef.getStatus().contains("Deleted") && !objectDef.getStatus().contains("Instantiated")) {
				int previousEvents = eventIO.getEventCount(app.getAppID(), owner);
				BigDataStackEvent newEvent = new BigDataStackEvent(
						app.getAppID(),
						owner,
						previousEvents,
						namespace,
						BigDataStackEventType.Openshift,
						BigDataStackEventSeverity.Warning,
						"Job '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")' was expected but not found",
						"Openshift project monitoring for '"+namespace+"' checked Job '"+objectDef.getObjectID()+"("+objectDef.getInstance()+") on the cluster but did not find it, it may have been manually deleted. Marking as such. ",
						objectDef.getObjectID(),
						objectDef.getInstance()
						);

				eventIO.addEvent(newEvent);
				rabbitMQClient.publishEvent(newEvent);

				Set<String> newStatuses = new HashSet<String>();
				newStatuses.add("Deleted");
				objectDef.setStatus(newStatuses);

				objectIO.updateObject(objectDef);

			}

			//System.err.println("Unable to update Job '"+objectDef.getAppID()+"-"+objectDef.getObjectID()+"-"+objectDef.getInstance()+"'");
			return;
		}

		System.err.println("Checking: '"+objectDef.getAppID()+"-"+objectDef.getObjectID()+"-"+objectDef.getInstance()+"'");

		// Stage 1: check whether the high-level object has changed state
		Set<String> jobStatuses = job.getStatuses();

		Set<String> newStatuses = new HashSet<>(jobStatuses);
		newStatuses.removeAll(objectDef.getStatus());

		for (String newStatus : newStatuses) {
			BigDataStackEventSeverity severity = BigDataStackEventSeverity.Info;
			if (newStatus.equalsIgnoreCase("Failed")) severity = BigDataStackEventSeverity.Alert;

			int previousEvents = eventIO.getEventCount(app.getAppID(), owner);

			BigDataStackEvent newEvent = new BigDataStackEvent(
					app.getAppID(),
					owner,
					previousEvents,
					namespace,
					BigDataStackEventType.Openshift,
					severity,
					"Job '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")' Status Added: "+objectDef.getStatus()+" -> "+newStatus,
					"Openshift project monitoring for '"+namespace+"' detected a status change in Job '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")', which changed status from '"+objectDef.getStatus()+"' to '"+newStatus+"'",
					objectDef.getObjectID(),
					objectDef.getInstance()
					);

			eventIO.addEvent(newEvent);
			rabbitMQClient.publishEvent(newEvent);
		}

		Set<String> removedStatuses = new HashSet<>(objectDef.getStatus());
		removedStatuses.removeAll(jobStatuses);

		for (String removedStatus : removedStatuses) {
			BigDataStackEventSeverity severity = BigDataStackEventSeverity.Info;

			int previousEvents = eventIO.getEventCount(app.getAppID(), owner);

			BigDataStackEvent newEvent = new BigDataStackEvent(
					app.getAppID(),
					owner,
					previousEvents,
					namespace,
					BigDataStackEventType.Openshift,
					severity,
					"Job '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")' Status Removed: "+removedStatus,
					"Openshift project monitoring for '"+namespace+"' detected a status change in Job '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")', which removed status '"+removedStatus+"'",
					objectDef.getObjectID(),
					objectDef.getInstance()
					);

			eventIO.addEvent(newEvent);
			rabbitMQClient.publishEvent(newEvent);
		}

		if (newStatuses.size()>0 || removedStatuses.size()==0) {
			objectDef.setStatus(jobStatuses);
			objectIO.updateObject(objectDef);
		}

		// Stage 2: check whether the underlying pods have changed state
		List<OpenshiftObject> pods = openshiftStatus.getPodsForJob(project.getName(), objectDef.getAppID()+"-"+objectDef.getObjectID()+"-"+objectDef.getInstance());
		System.err.println("Got "+pods.size()+" pods");
		for (OpenshiftObject pod : pods) {
			updatePodStatus(project.getName(), app, objectDef, pod);
		}

		for (String newStatus : newStatuses) {
			if (newStatus.equalsIgnoreCase("Complete")) {
				int previousEvents = eventIO.getEventCount(app.getAppID(), owner);

				BigDataStackEvent newEvent = new BigDataStackEvent(
						app.getAppID(),
						owner,
						previousEvents,
						namespace,
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Alert,
						"Job '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")' Completed",
						"Job '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")' reached Completed status",
						objectDef.getObjectID(),
						objectDef.getInstance()
						);

				eventIO.addEvent(newEvent);
				rabbitMQClient.publishEvent(newEvent);
			}
		}

		cleanUpPodsStatuses(app, objectDef);
	}

	/**
	 * Checks and performs an update if needed for a specified object definition
	 * @param project
	 * @param app
	 * @param objectDef
	 * @throws Exception
	 */
	protected void processDeploymentConfig(OpenshiftObject project, BigDataStackApplication app, BigDataStackObjectDefinition objectDef) throws Exception {
		OpenshiftObject deploymentConfig = openshiftStatus.getDeploymentConfig(project.getName(), objectDef.getAppID()+"-"+objectDef.getObjectID()+"-"+objectDef.getInstance());
		if (deploymentConfig==null) {
			// This should have been running but was not. It may have been manually deleted. Report and perform Update
			if (!objectDef.getStatus().contains("Deleted") && !objectDef.getStatus().contains("Instantiated")) {
				int previousEvents = eventIO.getEventCount(app.getAppID(), owner);
				BigDataStackEvent newEvent = new BigDataStackEvent(
						app.getAppID(),
						owner,
						previousEvents,
						namespace,
						BigDataStackEventType.Openshift,
						BigDataStackEventSeverity.Warning,
						"Deployment Config '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")' was expected but not found",
						"Openshift project monitoring for '"+namespace+"' checked Deployment Config '"+objectDef.getObjectID()+"("+objectDef.getInstance()+") on the cluster but did not find it, it may have been manually deleted. Marking as such. ",
						objectDef.getObjectID(),
						objectDef.getInstance()
						);

				eventIO.addEvent(newEvent);
				rabbitMQClient.publishEvent(newEvent);

				Set<String> newStatuses = new HashSet<String>();
				newStatuses.add("Deleted");
				objectDef.setStatus(newStatuses);

				objectIO.updateObject(objectDef);

			}

			//System.err.println("Unable to update DeploymentConfig '"+objectDef.getAppID()+"-"+objectDef.getObjectID()+"-"+objectDef.getInstance()+"'");
			return;
		}

		System.err.println("Checking: '"+objectDef.getAppID()+"-"+objectDef.getObjectID()+"-"+objectDef.getInstance()+"'");

		// Stage 1: check whether the high-level object has changed state		
		Set<String> deploymentStatuses = deploymentConfig.getStatuses();

		Set<String> newStatuses = new HashSet<>(deploymentStatuses);
		newStatuses.removeAll(objectDef.getStatus());

		for (String newStatus : newStatuses) {
			BigDataStackEventSeverity severity = BigDataStackEventSeverity.Info;
			if (newStatus.equalsIgnoreCase("Failed")) severity = BigDataStackEventSeverity.Alert;

			int previousEvents = eventIO.getEventCount(app.getAppID(), owner);

			BigDataStackEvent newEvent = new BigDataStackEvent(
					app.getAppID(),
					owner,
					previousEvents,
					namespace,
					BigDataStackEventType.Openshift,
					severity,
					"Deployment Config '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")' Status Added: "+objectDef.getStatus()+" -> "+newStatus,
					"Openshift project monitoring for '"+namespace+"' detected a status change in Deployment Config '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")', which changed status from '"+objectDef.getStatus()+"' to '"+newStatus+"'",
					objectDef.getObjectID(),
					objectDef.getInstance()
					);

			eventIO.addEvent(newEvent);
			rabbitMQClient.publishEvent(newEvent);
		}

		Set<String> removedStatuses = new HashSet<>(objectDef.getStatus());
		removedStatuses.removeAll(deploymentStatuses);

		for (String removedStatus : removedStatuses) {
			BigDataStackEventSeverity severity = BigDataStackEventSeverity.Info;

			int previousEvents = eventIO.getEventCount(app.getAppID(), owner);

			BigDataStackEvent newEvent = new BigDataStackEvent(
					app.getAppID(),
					owner,
					previousEvents,
					namespace,
					BigDataStackEventType.Openshift,
					severity,
					"Deployment Config '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")' Status Removed: "+objectDef.getStatus()+" -> "+removedStatus,
					"Openshift project monitoring for '"+namespace+"' detected a status change in Deployment Config '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")', which changed status from '"+objectDef.getStatus()+"' to '"+removedStatus+"'",
					objectDef.getObjectID(),
					objectDef.getInstance()
					);

			eventIO.addEvent(newEvent);
			rabbitMQClient.publishEvent(newEvent);
		}

		if (newStatuses.size()>0 || removedStatuses.size()==0) {
			objectDef.setStatus(deploymentStatuses);
			objectIO.updateObject(objectDef);
		}

		// Stage 2: check whether the underlying pods have changed state
		List<OpenshiftObject> pods = openshiftStatus.getPodsForDeploymentConfig(project.getName(), objectDef.getAppID()+"-"+objectDef.getObjectID()+"-"+objectDef.getInstance());
		System.err.println("Got "+pods.size()+" pods");
		for (OpenshiftObject pod : pods) {
			updatePodStatus(project.getName(), app, objectDef, pod);
		}

		cleanUpPodsStatuses(app, objectDef);

	}




	/**
	 * This method processes any changes detected to a particular pod, updating both the database as well as generating events
	 * @param project
	 * @param app
	 * @param objectDef
	 * @param pod
	 * @throws SQLException
	 */
	protected void updatePodStatus(String project, BigDataStackApplication app, BigDataStackObjectDefinition objectDef, OpenshiftObject pod) throws SQLException {

		String podID = pod.getName();
		String status = pod.getStatuses().iterator().next();
		String podIP = pod.ifPodGetIP();
		String hostIP = pod.ifPodGetHost();

		// check if we know about this pod already
		BigDataStackPodStatus savedStatus = podStatusIO.getPodStatus(podID);

		// if operation sequence pod, reflect status back to that object
		if (objectDef.getObjectID().equalsIgnoreCase("operationsequence") && (objectDef.getStatus()==null || objectDef.getStatus().size()==0) && savedStatus!=null) {
			Set<String> objStatus = new HashSet<String>();
			objStatus.add(savedStatus.getStatus());
			objectDef.setStatus(objStatus);
			objectIO.updateObject(objectDef);
		}
		
		// if not, then create a new PodStatus and report the creation event
		if (savedStatus==null) {
			BigDataStackPodStatus newStatus = new BigDataStackPodStatus(
					app.getAppID(),
					owner,
					namespace,
					objectDef.getObjectID(),
					objectDef.getInstance(),
					podID,
					status,
					podIP,
					hostIP
					);

			int previousEvents = eventIO.getEventCount(app.getAppID(), owner);

			BigDataStackEvent newEvent = new BigDataStackEvent(
					app.getAppID(),
					owner,
					previousEvents,
					namespace,
					BigDataStackEventType.Openshift,
					BigDataStackEventSeverity.Info,
					"Pod Created: '"+podID+"'",
					"Openshift project monitoring for '"+namespace+"' detected a new pod connected to object '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")', which has status status '"+status+"'",
					objectDef.getObjectID(),
					objectDef.getInstance()
					);

			podStatusIO.addPodStatus(newStatus);
			eventIO.addEvent(newEvent);
			rabbitMQClient.publishEvent(newEvent);
			
			// if operation sequence pod, reflect status back to that object
			if (objectDef.getObjectID().equalsIgnoreCase("operationsequence")) {
				Set<String> objStatus = new HashSet<String>();
				objStatus.add(newStatus.getStatus());
				objectDef.setStatus(objStatus);
				objectIO.updateObject(objectDef);
			}
			

			// if we know about this pod already, check to see if anything has changed 
		} else {

			// if pod status has changed, update the pod status and report the event
			if (!savedStatus.getStatus().equalsIgnoreCase(status)) {

				int previousEvents = eventIO.getEventCount(app.getAppID(), owner);

				BigDataStackEvent newEvent = new BigDataStackEvent(
						app.getAppID(),
						owner,
						previousEvents,
						namespace,
						BigDataStackEventType.Openshift,
						BigDataStackEventSeverity.Info,
						"Pod '"+podID+"' Status Change: '"+savedStatus.getStatus()+"' -> '"+status+"'",
						"Openshift project monitoring for '"+namespace+"' detected a change in pod '"+podID+"' connected to object '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")', its status changed from '"+savedStatus.getStatus()+"' -> '"+status+"'",
						objectDef.getObjectID()
						);

				savedStatus.setStatus(status);
				savedStatus.setHostIP(hostIP);
				savedStatus.setPodIP(podIP);

				podStatusIO.updatePodStatus(savedStatus);
				eventIO.addEvent(newEvent);
				rabbitMQClient.publishEvent(newEvent);
				
				// if operation sequence pod, reflect status back to that object
				if (objectDef.getObjectID().equalsIgnoreCase("operationsequence")) {
					Set<String> objStatus = new HashSet<String>();
					objStatus.add(savedStatus.getStatus());
					objectDef.setStatus(objStatus);
					objectIO.updateObject(objectDef);
				}

				// if pod IP addresses has changed, update the pod status and report the event
			} else if (!savedStatus.getHostIP().equalsIgnoreCase(hostIP) || !savedStatus.getPodIP().equalsIgnoreCase(podIP)) {

				int previousEvents = eventIO.getEventCount(app.getAppID(), owner);

				BigDataStackEvent newEvent = new BigDataStackEvent(
						app.getAppID(),
						owner,
						previousEvents,
						namespace,
						BigDataStackEventType.Openshift,
						BigDataStackEventSeverity.Info,
						"Pod '"+podID+"' Hosting Change: Host ['"+savedStatus.getHostIP()+"' -> '"+hostIP+"'], Pod IP ['"+savedStatus.getPodIP()+"' -> '"+podIP+"']",
						"Openshift project monitoring for '"+namespace+"' detected a change in pod '"+podID+"' connected to object '"+objectDef.getObjectID()+"("+objectDef.getInstance()+")', ['"+savedStatus.getHostIP()+"' -> '"+hostIP+"'], Pod IP ['"+savedStatus.getPodIP()+"' -> '"+podIP+"']",
						objectDef.getObjectID(),
						objectDef.getInstance()
						);

				savedStatus.setStatus(status);
				savedStatus.setHostIP(hostIP);
				savedStatus.setPodIP(podIP);

				podStatusIO.updatePodStatus(savedStatus);
				eventIO.addEvent(newEvent);
				rabbitMQClient.publishEvent(newEvent);
			}
			// otherwise nothing happens


		}
	}

}
