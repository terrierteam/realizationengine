package eu.bigdatastack.gdt.threads;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.bigdatastack.gdt.lxdb.BigDataStackApplicationIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackEventIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackOperationSequenceIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackPodStatusIO;
import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.openshift.OpenshiftObject;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackEvent;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectType;
import eu.bigdatastack.gdt.structures.data.BigDataStackPodStatus;

/**
 * This thread monitors the state of a project and reports status changes in pods.
 * In effect, this thread polls the Openshift API for a namespace and generates
 * BigDataStackEvent objects when a kubernetes/Openshift object is created or
 * changes state. These events are posted to a rabbitMQ topic, where they can be
 * used to trigger other operations. 
 * 
 * @author EbonBlade
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
									List<OpenshiftObject> pods = openshiftStatus.getPods(project.getName(), true, true, "operationsequence=True");
									for (OpenshiftObject pod : pods) {
										Map<String,String> labels = pod.getLabels();
										if (labels.get("runnerIndex").equalsIgnoreCase(String.valueOf(objectDef.getInstance())))
											updatePodStatus(project.getName(), app, objectDef, pod);
									}
									
								}
								
							}
						} catch (com.openshift.restclient.NotFoundException e) {
							// TODO Auto-generated catch block
							System.err.println(e.getMessage());
						}

					}

				}

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
					objectDef.getObjectID()
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
					objectDef.getObjectID()
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
						objectDef.getObjectID()
						);
				
				eventIO.addEvent(newEvent);
				rabbitMQClient.publishEvent(newEvent);
			}
		}
		

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
					objectDef.getObjectID()
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
					objectDef.getObjectID()
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
					objectDef.getObjectID()
					);

			podStatusIO.addPodStatus(newStatus);
			eventIO.addEvent(newEvent);
			rabbitMQClient.publishEvent(newEvent);

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
						objectDef.getObjectID()
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
