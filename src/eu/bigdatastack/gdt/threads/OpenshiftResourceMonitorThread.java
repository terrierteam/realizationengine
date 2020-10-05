package eu.bigdatastack.gdt.threads;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bigdatastack.gdt.lxdb.BigDataStackApplicationIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackMetricValueIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackPodStatusIO;
import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetricValue;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackPodStatus;

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
								
								if (!writers.containsKey(writeKey)) writers.put(writeKey, new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(writeDIR+"/"+writeKey+".json.gz", true)))));
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
	
	
	public static void main(String[] args) {
		
		Map<String,String> matchCriteria = new HashMap<String,String>();
		matchCriteria.put("namespace", "richardmproject");
		matchCriteria.put("pod_name", "dc-gdt-lx-store-2-k2966");
		
		BigDataStackMetricValue newMetricValue = PrometheusDataClient.basicQuery("http://prometheus-open-openshift-monitoring.ida.dcs.gla.ac.uk", "pod_name:container_memory_usage_bytes:sum", matchCriteria, null, "richardm", "richardmproject", "default", "none");
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			System.out.println(mapper.writeValueAsString(newMetricValue));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
