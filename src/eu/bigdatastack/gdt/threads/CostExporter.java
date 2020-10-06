package eu.bigdatastack.gdt.threads;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import eu.bigdatastack.gdt.lxdb.BigDataStackApplicationIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackEventIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackMetricValueIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackOperationSequenceIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackPodStatusIO;
import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.openshift.OpenshiftContainer;
import eu.bigdatastack.gdt.openshift.OpenshiftObject;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetricValue;
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
	BigDataStackMetricValueIO metricValueIO;
	
	String[] metrics = {"pod_name:container_cpu_usage:sum", "pod_name:container_memory_usage_bytes:sum"}; // these are the default resource usage metrics in the state db

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
			metricValueIO = new BigDataStackMetricValueIO(database);


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

					//System.err.println("App: "+app.getName());
					
					List<BigDataStackObjectDefinition> objectInstances = objectIO.getObjectList(owner, namespace, app.getAppID(), null);

					for (BigDataStackObjectDefinition objectDef : objectInstances) {
						
						//System.err.println("  Object: "+objectDef.getObjectID()+":"+objectDef.getInstance());
						
						if (objectDef==null) continue;
						if (objectDef.getType()==BigDataStackObjectType.DeploymentConfig || objectDef.getType()==BigDataStackObjectType.Job) {
							//System.out.println("    "+objectDef.getObjectID()+"("+objectDef.getInstance()+") of type "+objectDef.getType()+", states="+objectDef.getStatus());
							List<BigDataStackPodStatus> statuses = podStatusIO.getPodStatuses(app.getAppID(), app.getOwner(), objectDef.getObjectID(), app.getNamespace(), objectDef.getInstance());
							
							int podTotalCPURequest = 0;
							int podTotalMemRequest = 0;
							boolean changePerformed = false;
							
							List<BigDataStackMetricValue> cpuMetricValue = metricValueIO.getMetricValues(app.getAppID(), owner, namespace, objectDef.getObjectID(), metrics[0]);
							List<BigDataStackMetricValue> memMetricValue = metricValueIO.getMetricValues(app.getAppID(), owner, namespace, objectDef.getObjectID(), metrics[1]);
							
							for (BigDataStackPodStatus status : statuses) {
								
								if (status.getStatus().equalsIgnoreCase("Running") || status.getStatus().equalsIgnoreCase("Progressing")) {
									
									// First try and see if we have live data on resource usage in the state database
									
									boolean sucessfullyGotCPU = false;
									boolean sucessfullyGotMem = false;
									if (cpuMetricValue.size()>0) {
										for (BigDataStackMetricValue cpuMetric : cpuMetricValue) {
											// one metric can contain multiple instances, find a match to this pod we are currently on
											for (int i =0; i<cpuMetric.getValue().size(); i++) {
												Map<String,String> labels = cpuMetric.getLabels().get(i);
												String podID = labels.get("pod_name");
												if (podID!=null) {
													if (status.getPodID().equalsIgnoreCase(podID)) {
														
														// check the data is recent
														if ((System.currentTimeMillis()-cpuMetric.getLastUpdated().get(i))<120000) {
															sucessfullyGotCPU = true;
															int cpuMilicores = (int)(Double.parseDouble(cpuMetric.getValue().get(i))*1000);
															podTotalCPURequest = podTotalCPURequest + cpuMilicores;
														}
														
													}
												}
											}
										}
									}
									
									if (memMetricValue.size()>0) {
										for (BigDataStackMetricValue memMetric : memMetricValue) {
											// one metric can contain multiple instances, find a match to this pod we are currently on
											for (int i =0; i<memMetric.getValue().size(); i++) {
												Map<String,String> labels = memMetric.getLabels().get(i);
												String podID = labels.get("pod_name");
												if (podID!=null) {
													if (status.getPodID().equalsIgnoreCase(podID)) {
														
														// check the data is recent
														if ((System.currentTimeMillis()-memMetric.getLastUpdated().get(i))<120000) {
															sucessfullyGotMem = true;
															long memMegaBytes = (int)(Long.parseLong(memMetric.getValue().get(i)));
															memMegaBytes = memMegaBytes/1024; //KB
															memMegaBytes = memMegaBytes/1024; //MB
															podTotalMemRequest = podTotalMemRequest + (int)memMegaBytes;
														}
														
													}
												}
											}
										}
									}
									
									if (!sucessfullyGotCPU || !sucessfullyGotMem) {
										// we need to fall back to the resource request information
										
										OpenshiftObject pod = openshiftStatus.getPod(status.getNamespace(), status.getPodID());
										if (pod == null) {
											System.err.println("    Failed to get pod: "+status.getPodID()+" in "+status.getNamespace());
											continue;
										}
										
										// Sum requests across containers
										
										
										for (OpenshiftContainer container : pod.ifPodGetContainers()) {
											//System.err.println(status.getPodID()+" "+ container.getRequestCPU()+" "+container.getRequestMemory());
											if (!sucessfullyGotCPU) podTotalCPURequest = podTotalCPURequest + cpuToMilicores(container.getRequestCPU());
											if (!sucessfullyGotMem) podTotalMemRequest = podTotalMemRequest + memToMegabytes(container.getRequestMemory());
										}
										
										//System.err.println(status.getPodID()+" "+ podTotalCPURequest+" "+podTotalMemRequest);
									}
									
									
									
									
									
									changePerformed = true;
									
								} else {
									changePerformed = true;
								}
								
							}
							if (changePerformed) {
								
								double cost = calculateCPUCost(podTotalCPURequest) + calculateMemCost(podTotalMemRequest);
								System.err.println("Update: "+objectDef.getObjectID()+"("+objectDef.getInstance()+") "+cost);
								costPerHour.labels(app.getOwner(), app.getNamespace(), app.getAppID(), objectDef.getObjectID(), String.valueOf(objectDef.getInstance())).set(cost);
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
		return (((0.048*mb)/16384)/2);
	}
	
	public int cpuToMilicores(String string) {
		int milicores = 0;
		if (string.endsWith("m")) milicores = Integer.parseInt(string.substring(0, string.length()-1));
		else milicores = Integer.parseInt(string)*1000;
		
		return milicores;
	}
	
	public int memToMegabytes(String string) {
		int mb = 0;
		
		//System.err.println(string);
		
		if (string.endsWith("K")) mb = 1+(Integer.parseInt(string.substring(0, string.length()-1))/1024);
		if (string.endsWith("M")) mb = Integer.parseInt(string.substring(0, string.length()-1));
		if (string.endsWith("G")) mb = Integer.parseInt(string.substring(0, string.length()-1))*1024;
		
		if (string.endsWith("Ki")) mb = 1+(Integer.parseInt(string.substring(0, string.length()-2))/1024);
		if (string.endsWith("Mi")) mb = Integer.parseInt(string.substring(0, string.length()-2));
		if (string.endsWith("Gi")) mb = Integer.parseInt(string.substring(0, string.length()-2))*1024;
		
		
		
		return mb;
	}
	
	
	

}
