package org.terrier.realization.util;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.terrier.realization.application.GDTManager;
import org.terrier.realization.operations.BigDataStackOperation;
import org.terrier.realization.operations.Deploy;
import org.terrier.realization.operations.InstanceRefFromObjectLookup;
import org.terrier.realization.operations.Instantiate;
import org.terrier.realization.operations.ParameterFromObjectLookup;
import org.terrier.realization.operations.RecommendResources;
import org.terrier.realization.operations.SetParameter;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackObjectType;
import org.terrier.realization.structures.data.BigDataStackOperationSequence;
import org.terrier.realization.structures.data.BigDataStackOperationSequenceValidation;
import org.terrier.realization.structures.data.BigDataStackResourceTemplate;

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
public class OperationSequenceValidation {

	public static BigDataStackOperationSequenceValidation validate(GDTManager manager, BigDataStackOperationSequence sequence) throws SQLException {
		
		
		sequence.getParameters().put("appID", "Inferred");
		sequence.getParameters().put("namespace", "Inferred");
		sequence.getParameters().put("owner", "Inferred");
		sequence.getParameters().put("sequenceID", "Inferred");
		sequence.getParameters().put("dbusername", "Inferred");
		sequence.getParameters().put("dbpassword", "Inferred");
		sequence.getParameters().put("dbtype", "Inferred");
		sequence.getParameters().put("dbhost", "Inferred");
		sequence.getParameters().put("dbport", "Inferred");
		sequence.getParameters().put("dbname", "Inferred");
		sequence.getParameters().put("occlient", "Inferred");
		sequence.getParameters().put("ochost", "Inferred");
		sequence.getParameters().put("ocport", "Inferred");
		sequence.getParameters().put("ocusername", "Inferred");
		sequence.getParameters().put("ocpassword", "Inferred");
		sequence.getParameters().put("ochostextension", "Inferred");
		sequence.getParameters().put("ocimagerepositoryhost", "Inferred");
		sequence.getParameters().put("rmqhost", "Inferred");
		sequence.getParameters().put("rmqport", "Inferred");
		sequence.getParameters().put("rmqusername", "Inferred");
		sequence.getParameters().put("rmqpassword", "Inferred");
		sequence.getParameters().put("objectID", "Inferred");
		sequence.getParameters().put("instance", "Inferred");
		
		List<String> missingObjects = new ArrayList<String>();
		
		// Parameters
		Map<String,String> parametersSetWithDefaults = new HashMap<String,String>();
		Map<String,BigDataStackOperation> parametersSetAtRuntime = new HashMap<String,BigDataStackOperation>();
		List<String> parametersNotSet = new ArrayList<String>();
		
		// Check objects and get their parameters
		Map<String,String> objectIDsAndRefs = getObjectIDsAndRefsForSequence(sequence);
		Map<String,BigDataStackObjectDefinition> objectID2Object = new HashMap<String,BigDataStackObjectDefinition>(); 
		List<String> allParameters = new ArrayList<String>(); // list of all placeholders
		Map<String,List<String>> objects2parameters = new HashMap<String,List<String>>(); // mapping between an objectid and its placeholders
		for (String objectID : objectIDsAndRefs.keySet()) {
			BigDataStackObjectDefinition object = manager.getObjectTemplateClient().getObject(objectID, sequence.getOwner());
			if (object==null) {
				missingObjects.add(objectID);
				objectIDsAndRefs.remove(objectID);
			}
			else {
				objectID2Object.put(objectID, object);
				List<String> placeholders = getConfigurableParametersForObjectID(object);
				objects2parameters.put(objectID, placeholders);
				for (String placeholder : placeholders) if (!allParameters.contains(placeholder)) allParameters.add(placeholder);
			}
		}
		
		// Get a list of parameters that are set at run-time
		parametersSetAtRuntime = getParametersSetAtRunTime(sequence);
		for (String parameter : parametersSetAtRuntime.keySet()) if (allParameters.contains(parameter)) allParameters.remove(parameter);
		
		// Get a list of parameters set using defaults
		for (String parameter : allParameters) if (sequence.getParameters().containsKey(parameter)) parametersSetWithDefaults.put(parameter, sequence.getParameters().get(parameter));
		for (String parameter : parametersSetWithDefaults.keySet()) if (allParameters.contains(parameter)) allParameters.remove(parameter);
		
		// Add all remaining parameters, which are not set
		for (String parameter : allParameters) parametersNotSet.add(parameter);
		
		
		
		// Resources
		Map<String, List<BigDataStackResourceTemplate>> objectsWithValidTemplates = new HashMap<String, List<BigDataStackResourceTemplate>>();
		Map<String, List<BigDataStackResourceTemplate>> objectsWithIncompleteTemplates = new HashMap<String, List<BigDataStackResourceTemplate>>();
		List<String> objectsWithTemplatesSetAtRuntime = new ArrayList<String>();
		
		// check which objects have templates
		for (BigDataStackObjectDefinition object : objectID2Object.values()) {
			List<BigDataStackResourceTemplate> resourceTemplates = extractTemplatesFromObject(object);
			if (resourceTemplates==null) continue;
			boolean isValid = true;
			for (BigDataStackResourceTemplate template : resourceTemplates) {
				if (!template.getRequests().containsKey("cpu")) isValid=false;
				if (!template.getRequests().containsKey("memory")) isValid=false;
				if (!template.getLimits().containsKey("cpu")) isValid=false;
				if (!template.getLimits().containsKey("memory")) isValid=false;
			}
			
			if (isValid) objectsWithValidTemplates.put(object.getObjectID(), resourceTemplates);
			else objectsWithIncompleteTemplates.put(object.getObjectID(), resourceTemplates);
			
		}
		
		// are any templates expected to be set at run-time?
		List<String> objectIDsWithRuntimeResources = getObjectIDsThatAreSlatedForResourceRecommendation(sequence);
		for (BigDataStackObjectDefinition object : objectID2Object.values()) {
			if (objectIDsWithRuntimeResources.contains(object.getObjectID())) objectsWithTemplatesSetAtRuntime.add(object.getObjectID());
		}
		
		return new BigDataStackOperationSequenceValidation(missingObjects, parametersSetWithDefaults, parametersSetAtRuntime, parametersNotSet, objects2parameters, objectsWithValidTemplates, objectsWithIncompleteTemplates, objectsWithTemplatesSetAtRuntime);
	}
	
	public static List<String> getObjectIDsThatAreSlatedForResourceRecommendation(BigDataStackOperationSequence sequence) {
		
		List<String> objectIDs = new ArrayList<String>();
		
		List<BigDataStackOperation> operations = sequence.getOperations();
		for (BigDataStackOperation operation : operations) {
			if (operation instanceof RecommendResources) objectIDs.add(operation.getObjectID());
		}
		return objectIDs;
		
	}
	
	
	public static List<BigDataStackResourceTemplate> extractTemplatesFromObject(BigDataStackObjectDefinition object) {
		List<BigDataStackResourceTemplate> resourceTemplate = new ArrayList<BigDataStackResourceTemplate>();
		
		try {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			
			JsonNode root = mapper.readTree(object.getYamlSource());
			
			Iterator<JsonNode> containers = null;
			
			// Deployment Config
			if (object.getType()==BigDataStackObjectType.DeploymentConfig || object.getType()==BigDataStackObjectType.Job) {
				containers = root.get("spec").get("template").get("spec").get("containers").elements();
			}
			
			if (object.getType()==BigDataStackObjectType.Pod) {
				containers = root.get("spec").get("containers").elements();
			}
			
			if (containers==null) return null;
			
			while (containers.hasNext()) {
				JsonNode container = containers.next();
				if (container.has("resources")) {
					JsonNode resources = container.get("resources");
					BigDataStackResourceTemplate newTemplate = new BigDataStackResourceTemplate();
					Map<String,String> requests = new HashMap<String,String>();
					if (resources.has("requests")) {
						JsonNode requestJSON = resources.get("requests");
						Iterator<Entry<String,JsonNode>> entryI = requestJSON.fields();
						while (entryI.hasNext()) {
							Entry<String,JsonNode> entry = entryI.next();
							requests.put(entry.getKey(), entry.getValue().asText());
						}
					}
					newTemplate.setRequests(requests);
					
					Map<String,String> limits = new HashMap<String,String>();
					if (resources.has("limits")) {
						JsonNode requestJSON = resources.get("limits");
						Iterator<Entry<String,JsonNode>> entryI = requestJSON.fields();
						while (entryI.hasNext()) {
							Entry<String,JsonNode> entry = entryI.next();
							limits.put(entry.getKey(), entry.getValue().asText());
						}
					}
					newTemplate.setLimits(limits);
					
					resourceTemplate.add(newTemplate);
				} else resourceTemplate.add(null);
			}
			

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return resourceTemplate;
	}
	
	/**
	 * Gets a list of the target objectIDs created by this operation sequence
	 * @param sequence
	 * @return
	 */
	public static Map<String,String> getObjectIDsAndRefsForSequence(BigDataStackOperationSequence sequence) {
		Map<String,String> objectIDs = new HashMap<String,String>();
		List<BigDataStackOperation> operations = sequence.getOperations();
		for (BigDataStackOperation operation : operations) {
			
			if (operation instanceof Instantiate) {
				String objectID = operation.getObjectID();
				if (!objectIDs.containsKey(objectID)) objectIDs.put(objectID, ((Instantiate)operation).getSeqInstanceRef());
			}
			
			if (operation instanceof Deploy) {
				String objectID = operation.getObjectID();
				if (!objectIDs.containsKey(objectID)) objectIDs.put(objectID, ((Deploy)operation).getInstanceRef());
			}
			
		}
		return objectIDs;
	}
	
	
	public static List<String> getConfigurableParametersForObjectID(BigDataStackObjectDefinition object) {
		List<String> placeholders = new ArrayList<String>();
		
		String yaml = object.getYamlSource();
        yaml = yaml.replaceAll("\\n", "\n");

		boolean inplaceholder = false;
		StringBuilder placeholder = new StringBuilder();
		for (int i = 0; i<yaml.length(); i++) {
			char c = yaml.charAt(i);
			
			if (c=='$') {
				if (!inplaceholder) {
					inplaceholder=true;
					placeholder = new StringBuilder();
				}
				else {
					inplaceholder=false;
					if (placeholder.length()>0 && !placeholders.contains(placeholder.toString())) placeholders.add(placeholder.toString()); 
				}
			} else {
				if (c=='\n') {
					inplaceholder = false;
				} else if (inplaceholder) placeholder.append(c);
			}
		}
		
		
		return placeholders;
	}
	
	
	public static Map<String,BigDataStackOperation> getParametersSetAtRunTime(BigDataStackOperationSequence sequence) {
		HashMap<String,BigDataStackOperation> parametersSetAtRunTime = new HashMap<String,BigDataStackOperation>();
		
		List<BigDataStackOperation> operations = sequence.getOperations();
		for (BigDataStackOperation operation : operations) {
			
			if (operation instanceof Instantiate) parametersSetAtRunTime.put(((Instantiate) operation).getSeqInstanceRef(), operation);
			if (operation instanceof Deploy) parametersSetAtRunTime.put(((Deploy) operation).getInstanceRef(), operation);
			if (operation instanceof InstanceRefFromObjectLookup) parametersSetAtRunTime.put(((InstanceRefFromObjectLookup) operation).getParameter(), operation);
			if (operation instanceof ParameterFromObjectLookup) parametersSetAtRunTime.put(((ParameterFromObjectLookup) operation).getParameter(), operation);
			if (operation instanceof SetParameter) parametersSetAtRunTime.put(((SetParameter) operation).getKey(), operation);
			
		}
		
		if (parametersSetAtRunTime.containsKey(null)) parametersSetAtRunTime.remove(null);
		
		return parametersSetAtRunTime;
	}
	
	
}
