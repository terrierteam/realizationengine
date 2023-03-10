package org.terrier.realization.threads;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.terrier.realization.openshift.OpenshiftContainer;
import org.terrier.realization.openshift.OpenshiftObject;
import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.BigDataStackApplicationIO;
import org.terrier.realization.state.jdbc.BigDataStackEventIO;
import org.terrier.realization.state.jdbc.BigDataStackMetricValueIO;
import org.terrier.realization.state.jdbc.BigDataStackObjectIO;
import org.terrier.realization.state.jdbc.BigDataStackOperationSequenceIO;
import org.terrier.realization.state.jdbc.BigDataStackPodStatusIO;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.structures.data.BigDataStackApplication;
import org.terrier.realization.structures.data.BigDataStackMetricValue;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackObjectType;
import org.terrier.realization.structures.data.BigDataStackPodStatus;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;

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
public class CostExporter implements Runnable{

	static final Gauge costPerHour = Gauge.build()
		     .name("costPerHourInstance")
		     .help("US Dollar cost per hour of operation")
		     .labelNames("owner", "namespace", "appID", "objectID", "instance" )
		     .register();
	
	static final Gauge costPerHourObject = Gauge.build()
		     .name("costPerHour")
		     .help("US Dollar cost per hour of operation")
		     .labelNames("owner", "namespace", "appID", "objectID" )
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

					Set<String> objectIDS = new HashSet<String>();
					for (BigDataStackObjectDefinition objectDef : objectInstances) {
						objectIDS.add(objectDef.getObjectID());
					}
					
					for (String objectIDKey : objectIDS) {
						
						double totalObjectCost = 0.0;
						int active = 0;
						
						for (BigDataStackObjectDefinition objectDef : objectInstances) {
							
							if (!objectIDKey.equalsIgnoreCase(objectDef.getObjectID())) continue;
							
							
							
							
							//System.err.println("  Object: "+objectDef.getObjectID()+":"+objectDef.getInstance());
							
							if (objectDef==null) continue;
							if (objectDef.getType()==BigDataStackObjectType.DeploymentConfig || objectDef.getType()==BigDataStackObjectType.Job) {
								
								active++;
								
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
								
								double cost = calculateCPUCost(podTotalCPURequest) + calculateMemCost(podTotalMemRequest);
								
								if (changePerformed) {
									System.err.println("Update: "+objectDef.getObjectID()+"("+objectDef.getInstance()+") "+cost);
									costPerHour.labels(app.getOwner(), app.getNamespace(), app.getAppID(), objectDef.getObjectID(), String.valueOf(objectDef.getInstance())).set(cost);
								}
								
								totalObjectCost = totalObjectCost+cost;
							}
							
							
							
						}
						
						if (active>0) {
							costPerHourObject.labels(app.getOwner(), app.getNamespace(), app.getAppID(), objectIDKey).set(totalObjectCost);
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
