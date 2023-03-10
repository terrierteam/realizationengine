package org.terrier.realization.operations;

import java.util.HashSet;
import java.util.Set;

import org.terrier.realization.openshift.OpenshiftOperationClient;
import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.prometheus.PrometheusDataClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.BigDataStackObjectIO;
import org.terrier.realization.state.jdbc.BigDataStackOperationSequenceIO;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.threads.OperationSequenceThread;
import org.terrier.realization.util.EventUtil;

import com.fasterxml.jackson.databind.JsonNode;

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

public class Instantiate extends BigDataStackOperation{

	private String appID;
	private String owner;
	private String namespace;

	private String seqInstanceRef;

	public Instantiate() {
		this.className = this.getClass().getName();
	}

	public Instantiate(String appID, String owner, String namespace, String objectID, String defineInstanceRef) {
		super();
		this.objectID = objectID;
		this.appID = appID;
		this.owner = owner;
		this.namespace = namespace;
		this.seqInstanceRef = defineInstanceRef;

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
		return namespace;
	}
	public void setNamepace(String namespace) {
		this.namespace = namespace;
	}

	public String getSeqInstanceRef() {
		return seqInstanceRef;
	}

	public void setSeqInstanceRef(String seqInstanceRef) {
		this.seqInstanceRef = seqInstanceRef;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@Override
	public String defaultDescription() {
		return "Creates an instance of "+objectID+", it can be referred to within the sequence as '"+seqInstanceRef+"'";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {

		try {

			// Stage 1: Retrieve instance object
			BigDataStackObjectIO objectTemplateClient = new BigDataStackObjectIO(database, true);
			BigDataStackObjectDefinition templateObject = objectTemplateClient.getObject(getObjectID(), getOwner(), 0);
			if (templateObject==null) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Object Instantiate Operation Failed: '"+getObjectID()+"'",
						"Attempted to create a new instance of object '"+getObjectID()+"', but was unable to find an associated object definition from available instances.",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			}

			// Stage 2: Create a new object from the template
			BigDataStackObjectDefinition instanceObject = templateObject.clone();
			instanceObject.setNamespace(namespace);
			instanceObject.setAppID(appID);
			Set<String> statuses = new HashSet<String>();
			statuses.add("Instantiated");
			instanceObject.setStatus(statuses);

			// Stage 3: Register object instance
			BigDataStackObjectIO objectInstanceClient = new BigDataStackObjectIO(database, false);

			int registerFailures =0;
			boolean hasRegistered = false;

			while (!hasRegistered) {
				int highestInstanceID = objectInstanceClient.getObjectCount(objectID, owner);
				int newInstanceID = highestInstanceID++;
				instanceObject.setInstance(newInstanceID);

				// Overwrite any default parameters listed in the yaml
				String updatedYaml = replaceDefaultParameters(instanceObject.getYamlSource(), newInstanceID);
				instanceObject.setYamlSource(updatedYaml);


				if (!objectInstanceClient.addObject(instanceObject)) {
					registerFailures++;
					if (registerFailures>=5) break;
				}

				hasRegistered = true;

			}

			if (!hasRegistered) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Object Instantiate Operation Failed: '"+getObjectID()+"'",
						"Attempted to create a new instance of object '"+getObjectID()+"', but failed when attempting to register the new instance with the database.",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			} else {
				parentSequenceRunner.getSequence().getParameters().put(seqInstanceRef, getObjectID()+":"+String.valueOf(instanceObject.getInstance()));
				// we have just changed the information stored in the sequence instance, so sync that with the db
				BigDataStackOperationSequenceIO sequenceInstanceClient = new BigDataStackOperationSequenceIO(database,false);
				sequenceInstanceClient.updateSequence(parentSequenceRunner.getSequence());
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Info,
						"Object Instantiate Operation Completed: '"+getObjectID()+"("+instanceObject.getInstance()+")'",
						"Created a new instance of object '"+getObjectID()+"("+instanceObject.getInstance()+")'",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				
			}


		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;

	}
	
	
	public String replaceDefaultParameters(String yaml, int instance) {
		yaml = yaml.replaceAll("\\$appID\\$", appID);
		yaml = yaml.replaceAll("\\$owner\\$", owner);
		yaml = yaml.replaceAll("\\$namespace\\$", namespace);
		yaml = yaml.replaceAll("\\$objectID\\$", objectID);
		yaml = yaml.replaceAll("\\$instance\\$", String.valueOf(instance));
		
		yaml = yaml.replaceAll("\\$appid\\$", appID);
		yaml = yaml.replaceAll("\\$objectid\\$", objectID);
		
		yaml = yaml.replaceAll("\\$APPID\\$", appID);
		yaml = yaml.replaceAll("\\$OWNER\\$", owner);
		yaml = yaml.replaceAll("\\$NAMESPACE\\$", namespace);
		yaml = yaml.replaceAll("\\$OBJECTID\\$", objectID);
		yaml = yaml.replaceAll("\\$INSTANCE\\$", String.valueOf(instance));
		
		return yaml;
	}
	
	@Override
	public void initalizeParameters(JsonNode configJson) {
		objectID = configJson.get("objectID").asText();
		seqInstanceRef = configJson.get("defineInstanceRef").asText();
	}
}
