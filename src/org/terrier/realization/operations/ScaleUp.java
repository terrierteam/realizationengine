package org.terrier.realization.operations;


import org.terrier.realization.openshift.OpenshiftOperationClient;
import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.prometheus.PrometheusDataClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.BigDataStackObjectIO;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackObjectType;
import org.terrier.realization.threads.OperationSequenceThread;
import org.terrier.realization.util.EventUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

public class ScaleUp extends BigDataStackOperation {

	private String appID;
	private String owner;
	private String namepace;
	
	private String objectID;
	private int instance;
	private int replicasToIncreaseBy = 1;
	
	ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
	
	
	public ScaleUp() {
		this.className = this.getClass().getName();
	}
	
	public ScaleUp(String appID, String owner, String namepace, String objectID, int instance) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.objectID = objectID;
		this.instance = instance;

		this.className = this.getClass().getName();
	}
	
	

	public ScaleUp(String appID, String owner, String namepace, String objectID, int instance, int replicasToIncreaseBy) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.objectID = objectID;
		this.instance = instance;
		
		this.replicasToIncreaseBy = replicasToIncreaseBy;
		
		this.className = this.getClass().getName();
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
		return namepace;
	}

	public void setNamespace(String namepace) {
		this.namepace = namepace;
	}

	public String getObjectID() {
		return objectID;
	}

	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}
	
	public String getNamepace() {
		return namepace;
	}

	public void setNamepace(String namepace) {
		this.namepace = namepace;
	}

	public int getInstance() {
		return instance;
	}

	public void setInstance(int instance) {
		this.instance = instance;
	}

	public int getReplicasToIncreaseBy() {
		return replicasToIncreaseBy;
	}

	public void setReplicasToIncreaseBy(int replicasToIncreaseBy) {
		this.replicasToIncreaseBy = replicasToIncreaseBy;
	}

	@Override
	public String defaultDescription() {
		return "Increases the replication factor of "+objectID+"("+instance+") by "+replicasToIncreaseBy;
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		
		try {
			BigDataStackObjectIO objectInstanceClient = new BigDataStackObjectIO(database, false);
			
			BigDataStackObjectDefinition object = objectInstanceClient.getObject(objectID, owner, instance);
			if (object==null) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"ScaleUp Operation Failed on: '"+getObjectID()+"("+instance+")'",
						"Attempted scaleing up of pod count on '"+getObjectID()+"("+instance+")': but failed as the object was not found",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			}
			
			if (object.getType()==BigDataStackObjectType.Job || object.getType()==BigDataStackObjectType.DeploymentConfig) {
				
				ObjectNode objectYaml = (ObjectNode) yamlMapper.readTree(object.getYamlSource());
				if (object.getType()==BigDataStackObjectType.Job) {
					ObjectNode spec = (ObjectNode) objectYaml.get("spec");
					spec.put("parallelism", spec.get("parallelism").asInt()+replicasToIncreaseBy);
				}
				
				if (object.getType()==BigDataStackObjectType.DeploymentConfig) {
					ObjectNode spec = (ObjectNode) objectYaml.get("spec");
					spec.put("replicas", spec.get("replicas").asInt()+replicasToIncreaseBy);
				}
				
				object.setYamlSource(yamlMapper.writeValueAsString(objectYaml));
				
				objectInstanceClient.updateObject(object);
				
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Info,
						"ScaleUp Operation Updated Definition of: '"+getObjectID()+"("+instance+")'",
						"Increased pod count on '"+getObjectID()+"("+instance+")'.",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				
				boolean applySuccessful = openshiftOperationClient.applyOperation(object);
				
				if (applySuccessful) {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Info,
							"ScaleUp Operation Completed on: '"+getObjectID()+"("+instance+")'",
							"Scale-up operation applied to the cluster for '"+getObjectID()+"("+instance+")'.",
							parentSequenceRunner.getSequence().getSequenceID(),
							parentSequenceRunner.getSequence().getIndex()
							);
					return true;
				} else {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Error,
							"ScaleUp Operation failed to apply to the cluster object '"+getObjectID()+"("+instance+")'",
							"Failed to Increased pod count on '"+getObjectID()+"("+instance+")', because the cluster rejected it",
							parentSequenceRunner.getSequence().getSequenceID(),
							parentSequenceRunner.getSequence().getIndex()
							);
					return false;
				}
				
				
			
				
			} else {
				
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"ScaleUp Operation Failed on: '"+getObjectID()+"("+instance+")'",
						"Attempted scaleing up of pod count on '"+getObjectID()+"("+instance+")': but failed as the object was not a Job or DeploymentConfig",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			
			return false;
		}
		
	}
	
	@Override
	public void initalizeParameters(JsonNode configJson) {
		objectID = configJson.get("objectID").asText();
		instance = configJson.get("instance").asInt();
		if (configJson.has("increaseBy")) replicasToIncreaseBy = configJson.get("increaseBy").asInt();
		
	}

}
