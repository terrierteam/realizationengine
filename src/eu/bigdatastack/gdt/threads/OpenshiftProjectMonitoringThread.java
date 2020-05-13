package eu.bigdatastack.gdt.threads;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;

import eu.bigdatastack.gdt.lxdb.BigDataStackApplicationIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackEventIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackOperationSequenceIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackPodStatusIO;
import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackEvent;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectType;
import eu.bigdatastack.gdt.structures.data.BigDataStackPodStatus;
import eu.bigdatastack.gdt.structures.openshift.IJob;
import eu.bigdatastack.gdt.util.OpenshiftUtil;

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
	LXDB database;
	String namespace;
	String owner;

	boolean kill = false;
	boolean failed = false;

	BigDataStackPodStatusIO podStatusIO;
	BigDataStackOperationSequenceIO operationSequenceIO;
	BigDataStackApplicationIO applicationIO;
	BigDataStackObjectIO objectIO;
	BigDataStackEventIO eventIO;

	public OpenshiftProjectMonitoringThread(OpenshiftStatusClient openshiftStatus, RabbitMQClient rabbitMQClient, LXDB database, String owner, String namespace) {
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

		IProject project = openshiftStatus.getProject(namespace); // here

		while (!kill) {

			try {

				List<BigDataStackApplication> applications = applicationIO.getApplications(owner);

				for (BigDataStackApplication app : applications) {

					List<BigDataStackObjectDefinition> objectInstances = objectIO.getObjectList(owner, namespace, app.getAppID());

					for (BigDataStackObjectDefinition objectDef : objectInstances) {
						
						if (objectDef.getType() == BigDataStackObjectType.DeploymentConfig) processDeploymentConfig(project, app, objectDef);
						
						if (objectDef.getType() == BigDataStackObjectType.Job) processJob(project, app, objectDef);

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
	protected void processJob(IProject project, BigDataStackApplication app, BigDataStackObjectDefinition objectDef) throws Exception {
		IJob job = openshiftStatus.getJob(project, objectDef.getObjectID());

		// Stage 1: check whether the high-level object has changed state
		Set<String> jobStatuses = job.getJobStatuses();

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
					"Job '"+objectDef.getObjectID()+"' Status Added: "+objectDef.getStatus()+" -> "+newStatus,
					"Openshift project monitoring for '"+namespace+"' detected a status change in Job '"+objectDef.getObjectID()+"', which changed status from '"+objectDef.getStatus()+"' to '"+newStatus+"'",
					objectDef.getObjectID()
					);

			eventIO.addEvent(newEvent);
			rabbitMQClient.publishEvent(newEvent);
		}

		Set<String> removedStatuses = new HashSet<>(objectDef.getStatus());
		removedStatuses.removeAll(jobStatuses);

		for (String newStatus : newStatuses) {
			BigDataStackEventSeverity severity = BigDataStackEventSeverity.Info;

			int previousEvents = eventIO.getEventCount(app.getAppID(), owner);

			BigDataStackEvent newEvent = new BigDataStackEvent(
					app.getAppID(),
					owner,
					previousEvents,
					namespace,
					BigDataStackEventType.Openshift,
					severity,
					"Job '"+objectDef.getObjectID()+"' Status Removed: "+objectDef.getStatus()+" -> "+newStatus,
					"Openshift project monitoring for '"+namespace+"' detected a status change in Job '"+objectDef.getObjectID()+"', which changed status from '"+objectDef.getStatus()+"' to '"+newStatus+"'",
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
		List<IPod> pods = openshiftStatus.getPods(project, objectDef.getObjectID());
		for (IPod pod : pods) {
			updatePodStatus(project, app, objectDef, pod);
		}

	}

	/**
	 * Checks and performs an update if needed for a specified object definition
	 * @param project
	 * @param app
	 * @param objectDef
	 * @throws Exception
	 */
	protected void processDeploymentConfig(IProject project, BigDataStackApplication app, BigDataStackObjectDefinition objectDef) throws Exception {
		IDeploymentConfig deploymentConfig = openshiftStatus.getDeploymentConfig(project, objectDef.getObjectID());

		// Stage 1: check whether the high-level object has changed state		
		Set<String> deploymentStatuses = OpenshiftUtil.getDeploymentConfigStatuses(deploymentConfig);

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
					"Deployment Config '"+objectDef.getObjectID()+"' Status Added: "+objectDef.getStatus()+" -> "+newStatus,
					"Openshift project monitoring for '"+namespace+"' detected a status change in Deployment Config '"+objectDef.getObjectID()+"', which changed status from '"+objectDef.getStatus()+"' to '"+newStatus+"'",
					objectDef.getObjectID()
					);

			eventIO.addEvent(newEvent);
			rabbitMQClient.publishEvent(newEvent);
		}

		Set<String> removedStatuses = new HashSet<>(objectDef.getStatus());
		removedStatuses.removeAll(deploymentStatuses);

		for (String newStatus : newStatuses) {
			BigDataStackEventSeverity severity = BigDataStackEventSeverity.Info;

			int previousEvents = eventIO.getEventCount(app.getAppID(), owner);

			BigDataStackEvent newEvent = new BigDataStackEvent(
					app.getAppID(),
					owner,
					previousEvents,
					namespace,
					BigDataStackEventType.Openshift,
					severity,
					"Deployment Config '"+objectDef.getObjectID()+"' Status Removed: "+objectDef.getStatus()+" -> "+newStatus,
					"Openshift project monitoring for '"+namespace+"' detected a status change in Deployment Config '"+objectDef.getObjectID()+"', which changed status from '"+objectDef.getStatus()+"' to '"+newStatus+"'",
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
		List<IPod> pods = openshiftStatus.getPods(project, objectDef.getObjectID());
		for (IPod pod : pods) {
			updatePodStatus(project, app, objectDef, pod);
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
	protected void updatePodStatus(IProject project, BigDataStackApplication app, BigDataStackObjectDefinition objectDef, IPod pod) throws SQLException {

		String podID = pod.getName();
		String status = pod.getStatus();
		String podIP = pod.getIP();
		String hostIP = pod.getHost();

		// check if we know about this pod already
		BigDataStackPodStatus savedStatus = podStatusIO.getPodStatus(podID);

		// if not, then create a new PodStatus and report the creation event
		if (savedStatus==null) {
			BigDataStackPodStatus newStatus = new BigDataStackPodStatus(
					app.getAppID(),
					owner,
					namespace,
					objectDef.getObjectID(),
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
					"Openshift project monitoring for '"+namespace+"' detected a new pod connected to object '"+objectDef.getObjectID()+"', which has status status '"+status+"'",
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
						"Openshift project monitoring for '"+namespace+"' detected a change in pod '"+podID+"' connected to object '"+objectDef.getObjectID()+"', its status changed from '"+savedStatus.getStatus()+"' -> '"+status+"'",
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
						"Openshift project monitoring for '"+namespace+"' detected a change in pod '"+podID+"' connected to object '"+objectDef.getObjectID()+"', its status changed from '"+savedStatus.getStatus()+"' -> '"+status+"'",
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
