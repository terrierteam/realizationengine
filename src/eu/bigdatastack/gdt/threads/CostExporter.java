package eu.bigdatastack.gdt.threads;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;


import eu.bigdatastack.gdt.lxdb.BigDataStackApplicationIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackEventIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackOperationSequenceIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackPodStatusIO;
import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.openshift.OpenshiftContainer;
import eu.bigdatastack.gdt.openshift.OpenshiftObject;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectType;
import eu.bigdatastack.gdt.structures.data.BigDataStackPodStatus;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;

public class CostExporter implements Runnable{

	static final Gauge costPerHour = Gauge.build()
		     .name("costPerHour")
		     .help("US Dollar cost per hour of operation")
		     .labelNames("owner", "namespace", "appID", "objectID", "instance" )
		     .register();

	
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

	public CostExporter(OpenshiftStatusClient openshiftStatus, RabbitMQClient rabbitMQClient, JDBCDB database, String owner, String namespace) {
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
		
		HTTPServer server = null;
		
		try {
			server = new HTTPServer(9678);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		while (!kill) {

			try {

				List<BigDataStackApplication> applications = applicationIO.getApplications(owner);

				for (BigDataStackApplication app : applications) {

					List<BigDataStackObjectDefinition> objectInstances = objectIO.getObjectList(owner, namespace, app.getAppID(), null);

					for (BigDataStackObjectDefinition objectDef : objectInstances) {
						
						if (objectDef==null) continue;
						if (objectDef.getType()==BigDataStackObjectType.DeploymentConfig || objectDef.getType()==BigDataStackObjectType.Job) {
							//System.out.println("|     - "+objectDef.getObjectID()+"("+objectDef.getInstance()+") of type "+objectDef.getType()+", states="+objectDef.getStatus());
							List<BigDataStackPodStatus> statuses = podStatusIO.getPodStatuses(app.getAppID(), app.getOwner(), objectDef.getObjectID(), app.getNamespace(), objectDef.getInstance());
							for (BigDataStackPodStatus status : statuses) {
								
								if (status.getStatus().equalsIgnoreCase("Running")) {
									
									OpenshiftObject pod = openshiftStatus.getPod(status.getNamespace(), status.getPodID());
									if (pod == null) continue;
									
									// Sum requests across containers
									int podTotalCPURequest = 0;
									int podTotalMemRequest = 0;
									for (OpenshiftContainer container : pod.ifPodGetContainers()) {
										podTotalCPURequest =+ cpuToMilicores(container.getRequestCPU());
										podTotalMemRequest =+ memToMegabytes(container.getRequestMemory());
									}
									
									System.err.println(status.getPodID()+" "+ podTotalCPURequest+" "+podTotalMemRequest);
									
									double cost = calculateCPUCost(podTotalCPURequest) + calculateMemCost(podTotalMemRequest);
									
									
									costPerHour.labels(app.getOwner(), app.getNamespace(), app.getAppID(), objectDef.getObjectID(), String.valueOf(objectDef.getInstance())).set(cost);
								} else {
									costPerHour.labels(app.getOwner(), app.getNamespace(), app.getAppID(), objectDef.getObjectID(), String.valueOf(objectDef.getInstance())).set(0.0);
								}
								
							}
						}
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
				failed = true;
				return;
			}

			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		// remember to close the thread pool used for communication with openshift
		openshiftStatus.close();
		server.stop();

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
	
	
	public double calculateCPUCost(int millicores) {
		// 4000m = 0.048
		return ((0.048/4000)/2)*millicores;
	}
	
	public double calculateMemCost(int mb) {
		// 16,384‬ = 0.048
		return ((0.048/16384)/2)*mb;
	}
	
	public int cpuToMilicores(String string) {
		int milicores = 0;
		if (string.endsWith("m")) milicores = Integer.parseInt(string.substring(0, string.length()-1));
		else milicores = Integer.parseInt(string)*1000;
		
		return milicores;
	}
	
	public int memToMegabytes(String string) {
		int mb = 0;
		if (string.endsWith("K")) mb = 1+(Integer.parseInt(string.substring(0, string.length()-1))/1024);
		if (string.endsWith("M")) mb = Integer.parseInt(string.substring(0, string.length()-1));
		if (string.endsWith("G")) mb = Integer.parseInt(string.substring(0, string.length()-1))*1024;
		
		if (string.endsWith("Ki")) mb = 1+(Integer.parseInt(string.substring(0, string.length()-2))/1024);
		if (string.endsWith("Mi")) mb = Integer.parseInt(string.substring(0, string.length()-2));
		if (string.endsWith("Gi")) mb = Integer.parseInt(string.substring(0, string.length()-2))*1024;
		
		
		return mb;
	}
	
	
	

}
