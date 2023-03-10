package org.terrier.realization.operations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.Map;

import org.terrier.realization.openshift.OpenshiftOperationClient;
import org.terrier.realization.openshift.OpenshiftOperationFabric8ioClient;
import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.prometheus.PrometheusDataClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.BigDataStackObjectIO;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.threads.OperationSequenceThread;
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

public class WriteLogs extends BigDataStackOperation{

	private String instanceRef;
	private String outFile;
	private String appID;
	private String owner;
	private String namespace;
	
	public WriteLogs() {
		this.className = this.getClass().getName();
	}
	
	public WriteLogs(String appID, String owner, String namepace, String instanceRef) {
		super();
		this.instanceRef = instanceRef;
		this.appID = appID;
		this.owner = owner;
		this.namespace = namepace;
		
		this.className = this.getClass().getName();
	}
	public String getObjectID() {
		return instanceRef;
	}

	public String getAppID() {
		return appID;
	}
	public void setAppID(String appID) {
		this.appID = appID;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namepace) {
		this.namespace = namepace;
	}

	@Override
	public String defaultDescription() {
		return "Creates object with ref "+instanceRef+" on the Openshift Cluster in "+namespace+".";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {

		try {
			
			if (!parentSequenceRunner.getSequence().getParameters().containsKey(instanceRef)) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Print Operation Failed: '"+instanceRef+"'",
						"Attempted to find an instance with within-sequence reference '"+instanceRef+"', but the parent sequence did not have an appropriate instance reference (did you Instantiate first?)",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			}
			
			String sourceObjectID = parentSequenceRunner.getSequence().getParameters().get(instanceRef).split(":")[0];
			int instance = Integer.valueOf(parentSequenceRunner.getSequence().getParameters().get(instanceRef).split(":")[1]);
			
			// Stage 1: Retrieve instance object
			BigDataStackObjectIO objectInstanceClient = new BigDataStackObjectIO(database, false);
			BigDataStackObjectDefinition instanceObject = objectInstanceClient.getObject(sourceObjectID, getOwner(), instance);
			if (instanceObject==null) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Print Operation Failed: '"+sourceObjectID+"'",
						"Attempted to get an instance '"+sourceObjectID+"("+instance+")', but was unable to find an associated object definition from available instances.",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			}
			
			
			// Override namespace
			try {
				String yamlSource = instanceObject.getYamlSource();
				ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
				JsonNode yamlNode = mapper.readTree(yamlSource);
				
				String namespace = yamlNode.get("metadata").get("namespace").asText();
				instanceObject.setNamespace(namespace);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			Map<String,Map<String,String>> logs = ((OpenshiftOperationFabric8ioClient)openshiftOperationClient).getLogs(instanceObject);
			
			for (String paramKey : parentSequenceRunner.getSequence().getParameters().keySet()) {
				outFile = outFile.replaceAll("\\$"+paramKey+"\\$", parentSequenceRunner.getSequence().getParameters().get(paramKey));
			}
			
			File f = new File(outFile);
			
			
			BufferedWriter br = null;
			try {
				br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
				
				for (String podID : logs.keySet()) {
					br.write("\n");
					for (String containerID : logs.get(podID).keySet()) {
						br.write("### "+podID+" "+containerID+" ###\n");
						br.write(logs.get(podID).get(containerID));
						br.write("\n");
					}
				}
				br.write("\n");
				
				br.close();
				
			} catch (Exception e) {
				if (br!=null)
					try {
						br.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				e.printStackTrace();
			}
			
			
			
			
			
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		

		return true;
		
		
		
	}

	@Override
	public void initalizeParameters(JsonNode configJson) {
		instanceRef = configJson.get("instanceRef").asText();
		outFile = configJson.get("outFile").asText();
	}

	public String getInstanceRef() {
		return instanceRef;
	}

	public void setInstanceRef(String instanceRef) {
		this.instanceRef = instanceRef;
	}

	public String getOutFile() {
		return outFile;
	}

	public void setOutFile(String outFile) {
		this.outFile = outFile;
	}

}
