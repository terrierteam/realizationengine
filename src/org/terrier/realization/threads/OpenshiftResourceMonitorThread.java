package org.terrier.realization.threads;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.terrier.realization.prometheus.PrometheusDataClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.BigDataStackApplicationIO;
import org.terrier.realization.state.jdbc.BigDataStackMetricValueIO;
import org.terrier.realization.state.jdbc.BigDataStackObjectIO;
import org.terrier.realization.state.jdbc.BigDataStackPodStatusIO;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.structures.data.BigDataStackApplication;
import org.terrier.realization.structures.data.BigDataStackMetricValue;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackPodStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
 * This thread periodically pools the central Openshift Prometheus instance to get the current CPU and
 * memory availability for pods managed by the realization engine. It will then store the that information
 * in the State Database and also write the information to a file.
 * @author EbonBlade
 *
 */
public class OpenshiftResourceMonitorThread implements Runnable{

	RabbitMQClient rabbitMQClient;
	JDBCDB database;
	String namespace;
	String owner;

	boolean kill = false;
	boolean failed = false;

	BigDataStackPodStatusIO podStatusIO;
	BigDataStackApplicationIO applicationIO;
	BigDataStackObjectIO objectIO;
	BigDataStackMetricValueIO metricValueIO;

	String centralPrometheusHost;
	String writeDIR;
	
	String[] metrics = {"pod_name:container_cpu_usage:sum", "pod_name:container_memory_usage_bytes:sum"};
	
	ObjectMapper mapper = new ObjectMapper();
	
	public OpenshiftResourceMonitorThread(RabbitMQClient rabbitMQClient, JDBCDB database, String owner, String namespace, String centralPrometheusHost, String writeDIR) {
		this.rabbitMQClient = rabbitMQClient;
		this.namespace = namespace;
		this.owner = owner;
		this.database = database;
		this.centralPrometheusHost = centralPrometheusHost;
		this.writeDIR = writeDIR;
	}

	@Override
	public void run() {
		
		try {
			// initalize database readers
			applicationIO = new BigDataStackApplicationIO(database);
			podStatusIO = new BigDataStackPodStatusIO(database);
			objectIO = new BigDataStackObjectIO(database, false); // monitor actual instances, not templates
			metricValueIO = new BigDataStackMetricValueIO(database);
		} catch (SQLException e) {
			e.printStackTrace();
			failed = true;
			return;
		}
		
		Map<String,BufferedWriter> writers = new HashMap<String,BufferedWriter>();

		File kfile = new File("kill");
		
		
		while (!kill && !kfile.exists()) {
			
			Set<String> writeKeysThisIteration = new HashSet<String>();
			
			try {

				List<BigDataStackApplication> applications = applicationIO.getApplications(owner);

				for (BigDataStackApplication app : applications) {

					List<BigDataStackObjectDefinition> objectInstances = objectIO.getObjectList(owner, namespace, app.getAppID(), null);

					for (BigDataStackObjectDefinition objectDef : objectInstances) {

						List<BigDataStackPodStatus> pods = podStatusIO.getPodStatuses(app.getAppID(), owner, objectDef.getObjectID(), namespace, -1);
						boolean oneOrMorePodsActive = false;
						for (BigDataStackPodStatus pod : pods) if (pod.getStatus().equalsIgnoreCase("Running")) {
							oneOrMorePodsActive=true;
							break;
						}
							
						if (!oneOrMorePodsActive) continue;
							
						for (String metricName : metrics) {

							List<BigDataStackMetricValue> matchedMetricValues = metricValueIO.getMetricValues(app.getAppID(), owner, namespace, objectDef.getObjectID(), metricName);
							
							BigDataStackMetricValue metricValue = null;
							if (matchedMetricValues.size()==0) {
								// No existing metric value reference found, create one
								metricValue = new BigDataStackMetricValue(owner, namespace, app.getAppID(), objectDef.getObjectID(), metricName, new ArrayList<String>(1), new ArrayList<Long>(1), new ArrayList<Map<String,String>>());
							} else {
								metricValue = matchedMetricValues.get(0); // this can only ever match one item
							}

							metricValue.getValue().clear();
							metricValue.getLastUpdated().clear();
							metricValue.getLabels().clear();
							
							for(BigDataStackPodStatus pod : pods) {
								if (pod.getStatus().equalsIgnoreCase("Running")) {
									Map<String,String> matchCriteria = new HashMap<String,String>();
									matchCriteria.put("namespace", namespace);
									matchCriteria.put("pod_name", pod.getPodID());
									
									BigDataStackMetricValue newMetricValue = PrometheusDataClient.basicQuery(centralPrometheusHost, metricName, matchCriteria, null, owner, namespace, app.getAppID(), objectDef.getObjectID());
	
									for (String value : newMetricValue.getValue()) metricValue.getValue().add(value);
									for (Long timestamp : newMetricValue.getLastUpdated()) metricValue.getLastUpdated().add(timestamp);
									for (Map<String,String> labels : newMetricValue.getLabels()) metricValue.getLabels().add(labels);

								}
								
								
							}
							
							if (writeDIR!=null) {
								String writeKey = owner+"-"+namespace+"-"+app.getAppID()+"-"+objectDef.getObjectID()+"-"+metricName;
								writeKeysThisIteration.add(writeKey);
								
								if (!writers.containsKey(writeKey)) writers.put(writeKey, new BufferedWriter(new OutputStreamWriter((new FileOutputStream(writeDIR+"/"+writeKey+".json", true)))));
								writers.get(writeKey).append(mapper.writeValueAsString(metricValue));
								writers.get(writeKey).newLine();
							}
							
							
							if (!metricValueIO.addMetricValue(metricValue)) metricValueIO.updateMetricValue(metricValue);

						}
						
						

					}

				}
				
				Set<String> keysToClose = new HashSet<String>();
				for (String writeKey : writers.keySet()) {
					if (!writeKeysThisIteration.contains(writeKey)) keysToClose.add(writeKey);
				}
				
				for (String writeKey : keysToClose) {
					writers.remove(writeKey).close();
				}

			} catch (Exception e) {
				e.printStackTrace();
				failed = true;
				
				try {
					Set<String> keysToClose = new HashSet<String>();
					for (String writeKey : writers.keySet()) keysToClose.add(writeKey);
					for (String writeKey : keysToClose) writers.remove(writeKey).close();
				} catch (IOException e1) {}
				
				return;
			}
			
			
			

			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		
		try {
			Set<String> keysToClose = new HashSet<String>();
			for (String writeKey : writers.keySet()) keysToClose.add(writeKey);
			for (String writeKey : keysToClose) writers.remove(writeKey).close();
		} catch (IOException e1) {}
	}
	
}
