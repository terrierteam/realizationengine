package org.terrier.realization.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.terrier.realization.application.GDTManager;
import org.terrier.realization.openshift.OpenshiftOperationFabric8ioClient;
import org.terrier.realization.structures.data.BigDataStackAppState;
import org.terrier.realization.structures.data.BigDataStackApplication;
import org.terrier.realization.structures.data.BigDataStackEvent;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.structures.data.BigDataStackMetric;
import org.terrier.realization.structures.data.BigDataStackMetricValue;
import org.terrier.realization.structures.data.BigDataStackNamespaceState;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackObjectType;
import org.terrier.realization.structures.data.BigDataStackOperationSequence;
import org.terrier.realization.structures.data.BigDataStackOperationSequenceValidation;
import org.terrier.realization.structures.data.BigDataStackPodStatus;
import org.terrier.realization.structures.data.BigDataStackResourceTemplateAdv;
import org.terrier.realization.structures.data.BigDataStackSLO;
import org.terrier.realization.structures.data.BigDataStackSearchResponse;
import org.terrier.realization.structures.reports.*;
import org.terrier.realization.util.EventUtil;
import org.terrier.realization.util.GDTFileUtil;
import org.terrier.realization.util.ManagerQuerying;
import org.terrier.realization.util.OperationSequenceValidation;
import org.terrier.realization.util.ResourceTemplateRecommendationUtil;
import org.terrier.realization.util.SearchUtil;

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

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
/**
 * This is the API hosting class which exposes a subset of the GDT manager methods for use by other applications. 
 * @author EbonBlade
 *
 */
public class ManagerResource {

	GDTManager manager;
	ManagerQuerying querying;
	
	ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
	ObjectMapper jsonMapper = new ObjectMapper();
	
	public ManagerResource(GDTManager manager) {
		this.manager = manager;
		querying = new ManagerQuerying(manager);
	}
	
	
	//----------------------------------------------------------
	// YAML Registration
	//----------------------------------------------------------
	
	@GET
	@Path("/registerfile/playbook/{owner}/{namespace}/{file}/{teamid}/{project}")
	public List<String> registerPlaybookFile(@PathParam("owner") String owner, @PathParam("namespace") String namespace, @PathParam("file") String file, @PathParam("teamid") String teamid, @PathParam("project") String project){
		
		String override = "TEAMID:"+teamid+","+"PROJECT:"+project;
		
		try {
			String playbook = GDTFileUtil.file2String(new File(file), "UTF-8");
			
			for (String param : override.split(",")) {
				String paramKey = param.split(":")[0];
				String value = param.split(":")[1];
				playbook = playbook.replaceAll("\\$"+paramKey+"\\$", value);
			}
			
			return manager.loadPlaybook(playbook, owner, namespace);
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<String>(0);
		}
	}
	
	@POST
	@Path("/registeryaml/playbook/{owner}/{namespace}")
	public List<String> registerPlaybook(@PathParam("owner") String owner, @PathParam("namespace") String namespace, String yaml) {
		return manager.loadPlaybook(yaml, owner, namespace);
	}
	
	@POST
	@Path("/registeryaml/playbook")
	public List<String> registerPlaybook(String yaml) {
		return manager.loadPlaybook(yaml, null, null);
	}
	
	@POST
	@Path("/registeryaml/application")
	public BigDataStackApplication registerApplication(String yaml) {
		return manager.registerApplication(yaml);	
	}
	
	@POST
	@Path("/registeryaml/object")
	public BigDataStackObjectDefinition registerObject(String yaml) {
		return manager.registerObject(yaml);
	}
	
	@POST
	@Path("/registeryaml/slo")
	public BigDataStackSLO registerSLO(String yaml) {
		return manager.registerSLO(yaml);
	}
	
	@POST
	@Path("/registeryaml/metric")
	public BigDataStackMetric registerMetric(String yaml) {
		return manager.registerMetric(yaml);
	}
	
	@POST
	@Path("/registeryaml/namespace")
	public BigDataStackNamespaceState registerNamespace(String yaml) {
		return manager.registerNamespace(yaml);
	}
	
	@POST
	@Path("/registeryaml/operationSequence")
	public BigDataStackOperationSequence registerOperationSequence(String yaml) {
		return manager.registerOperationSequence(yaml);
	}
	
	
	//----------------------------------------------------------
	// JSON Registration
	//----------------------------------------------------------
	
	@POST
	@Path("/registerjson/playbook/{owner}/{namespace}")
	public List<String> registerPlaybookJson(@PathParam("owner") String owner, @PathParam("namespace") String namespace, String json) {
		try {
			JsonNode node = jsonMapper.readTree(json);
			return manager.loadPlaybook(yamlMapper.writeValueAsString(node), owner, namespace);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@POST
	@Path("/registerjson/playbook")
	public List<String> registerPlaybookJson(String json) {
		try {
			JsonNode node = jsonMapper.readTree(json);
			return manager.loadPlaybook(yamlMapper.writeValueAsString(node), null, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@POST
	@Path("/registerjson/application")
	public BigDataStackApplication registerApplicationJson(String json) {
		try {
			JsonNode node = jsonMapper.readTree(json);
			return manager.registerApplication(yamlMapper.writeValueAsString(node));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@POST
	@Path("/registerjson/object")
	public BigDataStackObjectDefinition registerObjectJson(String json) {
		
		json = json.trim().replaceFirst("\ufeff", "");
		
		try {
			JsonNode node = jsonMapper.readTree(json);
			return manager.registerObject(yamlMapper.writeValueAsString(node));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@POST
	@Path("/registerjson/slo")
	public BigDataStackSLO registerSLOJson(String json) {
		try {
			JsonNode node = jsonMapper.readTree(json);
			return manager.registerSLO(yamlMapper.writeValueAsString(node));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@POST
	@Path("/registerjson/metric")
	public BigDataStackMetric registerMetricJson(String json) {
		try {
			JsonNode node = jsonMapper.readTree(json);
			return manager.registerMetric(yamlMapper.writeValueAsString(node));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@POST
	@Path("/registerjson/namespace")
	public BigDataStackNamespaceState registerNamespaceJson(String json) {
		try {
			JsonNode node = jsonMapper.readTree(json);
			return manager.registerNamespace(yamlMapper.writeValueAsString(node));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@POST
	@Path("/registerjson/operationSequence")
	public BigDataStackOperationSequence registerOperationSequenceJson(String json) {
		try {
			JsonNode node = jsonMapper.readTree(json);
			return manager.registerOperationSequence(yamlMapper.writeValueAsString(node));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@POST
	@Path("/updatejson/operationSequence")
	public BigDataStackOperationSequence updateOperationSequenceJson(String json) {
		try {
			BigDataStackOperationSequence sequence = jsonMapper.readValue(json, BigDataStackOperationSequence.class);
			if (!manager.sequenceTemplateClient.addSequence(sequence)) {
				if (!manager.sequenceTemplateClient.updateSequence(sequence)) {
					return null;
				}
			}
			return sequence;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	//----------------------------------------------------------
	// List and Get
	//----------------------------------------------------------
	
	@GET
	public boolean online(){
		return true;
	}
	
	@GET
	@Path("/list/{owner}")
	public List<BigDataStackApplication> listApplicationsB(@PathParam("owner") String owner){
		return querying.listApplications(owner);
	}
	
	@GET
	@Path("/list/{owner}/apps")
	public List<BigDataStackApplication> listApplications(@PathParam("owner") String owner){
		return querying.listApplications(owner);
	}
	
	@GET
	@Path("/list/{owner}/{appID}/states")
	public List<BigDataStackAppState> listApplicationsStates(@PathParam("owner") String owner, @PathParam("appID") String appID){
		return manager.getApplicationStates(owner, null, appID);
	}
	
	@GET
	@Path("/list/{owner}/{appID}/states/all")
	public List<BigDataStackAppState> listApplicationsStatesAll(@PathParam("owner") String owner, @PathParam("appID") String appID){
		return querying.listApplicationPossibleStates(owner, appID);
	}
	
	@GET
	@Path("/list/{owner}/{appID}/objectTemplates")
	public List<BigDataStackObjectDefinition> listObjectTemplates(@PathParam("owner") String owner, @PathParam("appID") String appID, @QueryParam("type") String type){
		return querying.listObjectTemplates(owner, appID, BigDataStackObjectType.valueOf(type));
	}
	
	@GET
	@Path("/list/{owner}/objectTemplates")
	public List<BigDataStackObjectDefinition> listObjectTemplates(@PathParam("owner") String owner, @QueryParam("type") String type){
		return querying.listObjectTemplates(owner, null, BigDataStackObjectType.valueOf(type));
	}
	
	@GET
	@Path("/list/{owner}/{appID}/objects")
	public List<BigDataStackObjectDefinition> listObjectInstances(@PathParam("owner") String owner, @PathParam("appID") String appID, @QueryParam("type") String type){
		return querying.listObjectInstances(owner, appID, BigDataStackObjectType.valueOf(type));
	}
	
	@GET
	@Path("/list/{owner}/objects")
	public List<BigDataStackObjectDefinition> listObjectInstances(@PathParam("owner") String owner, @QueryParam("type") String type){
		return querying.listObjectInstances(owner, null, BigDataStackObjectType.valueOf(type));
	}
	
	@GET
	@Path("/list/{owner}/{appID}/objects/{objectID}")
	public List<BigDataStackObjectDefinition> listObjectInstancesID(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("objectID") String objectID){
		return querying.listObjectInstances(owner, appID, objectID);
	}
	
	
	@GET
	@Path("/get/{owner}/{appID}/objects/{objectID}/instance/{instance}")
	public BigDataStackObjectDefinition getObjectInstance(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("objectID") String objectID, @PathParam("instance") String instance){
		return querying.getObjectInstance(owner, appID, objectID, Integer.parseInt(instance));
	}
	
	@GET
	@Path("/get/{owner}/{appID}/objects/{objectID}/template")
	public BigDataStackObjectDefinition getObjectTemplate(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("objectID") String objectID){
		return querying.getObjectTemplate(owner, appID, objectID);
	}
	
	@GET
	@Path("/list/{owner}/{appID}/sequenceTemplates")
	public List<BigDataStackOperationSequence> listOperationSequenceTemplates(@PathParam("owner") String owner, @PathParam("appID") String appID){
		return querying.listOperationSequenceTemplates(owner, appID);
	}
	
	@GET
	@Path("/list/{owner}/{appID}/sequences")
	public List<BigDataStackOperationSequence> listOperationSequenceInstances(@PathParam("owner")String owner, @PathParam("appID") String appID){
		return querying.listOperationSequenceInstances(owner, appID);
	}
	
	@GET
	@Path("/list/{owner}/{appID}/sequences/state")
	public List<BigDataStackOperationSequence> listOperationSequenceInstancesByState(@PathParam("owner")String owner, @PathParam("appID") String appID, @QueryParam("sequenceState") String sequenceState){
		return querying.listOperationSequenceInstancesByState(owner, appID, sequenceState);
	}
	
	@GET
	@Path("/list/{owner}/{appID}/sequence/{sequenceID}")
	public List<BigDataStackOperationSequence> listOperationSequenceInstances(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("sequenceID") String sequenceID){
		return querying.listOperationSequenceInstances(owner, appID, sequenceID);
	}
	
	@GET
	@Path("/get/{owner}/{appID}/sequence/{sequenceID}/instance/{instance}")
	public BigDataStackOperationSequence getOperationSequenceInstance(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("sequenceID") String sequenceID, @PathParam("instance") String instance){
		return querying.getOperationSequenceInstance(owner, appID, sequenceID, Integer.parseInt(instance));
	}
	
	@GET
	@Path("/get/{owner}/{appID}/sequence/{sequenceID}/template")
	public BigDataStackOperationSequence getOperationSequenceTemplate(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("sequenceID") String sequenceID){
		return querying.getOperationSequenceTemplate(owner, appID, sequenceID);
	}
	
	@GET
	@Path("/get/{owner}/{appID}/sequence/{sequenceID}/validate")
	public BigDataStackOperationSequenceValidation getOperationSequenceValidation(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("sequenceID") String sequenceID){
		BigDataStackOperationSequence sequence =  querying.getOperationSequenceTemplate(owner, appID, sequenceID);
		if (sequence==null) return null;
		else
			try {
				BigDataStackOperationSequenceValidation validation = OperationSequenceValidation.validate(manager, sequence);
				System.err.println(jsonMapper.writeValueAsString(validation));
				return validation;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
	} 
	
	@GET
	@Path("/list/{owner}/{appID}/objects/{objectID}/pods")
	public List<BigDataStackPodStatus> listPodStatuses(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("objectID") String objectID){
		return querying.listPodStatuses(owner, objectID);
	}
	
	@GET
	@Path("/list/{owner}/{appID}/objects/{objectID}/slos/{metricName}")
	public List<BigDataStackSLO> listSLOs(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("objectID") String objectID, @PathParam("metricName") String metricName){
		return querying.listSLOInstances(owner, appID, objectID, metricName);
	}
	
	@GET
	@Path("/list/{owner}/{appID}/objects/{objectID}/instance/{instance}/slos/{metricName}")
	public List<BigDataStackSLO> listSLOs(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("objectID") String objectID, @PathParam("metricName") String metricName, @PathParam("instance") String instance){
		return querying.listSLOInstances(owner, appID, objectID, Integer.parseInt(instance), metricName);
	}
	
	@GET
	@Path("/list/{owner}/metrics")
	public List<BigDataStackMetric> listMetrics(@PathParam("owner") String owner){
		return querying.listMetrics(owner);
	}
	
	@GET
	@Path("/get/{owner}/metrics/{metricName}")
	public BigDataStackMetric getMetric(@PathParam("owner") String owner, @PathParam("metricName") String metricName){
		return querying.getMetric(owner, metricName);
	}
	
	@GET
	@Path("/list/{owner}/{appID}/metrics/{metricName}")
	public List<BigDataStackMetricValue> listMetricValuesB(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("metricName") String metricName){
		return querying.listMetricValues(owner, appID, metricName, null);
	}
	
	@GET
	@Path("/list/{owner}/{appID}/metrics/{metricName}/{objectID}")
	public List<BigDataStackMetricValue> listMetricValues(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("metricName") String metricName, @PathParam("objectID") String objectID){
		return querying.listMetricValues(owner, appID, metricName, objectID);
	}
	
	@GET
	@Path("/list/{owner}/{appID}/events")
	public List<BigDataStackEvent> listEvents(@PathParam("owner") String owner, @PathParam("appID") String appID){
		return querying.listEvents(owner, appID);
	}
	
	@GET
	@Path("/list/{owner}/events")
	public List<BigDataStackEvent> listEvents(@PathParam("owner") String owner){
		return querying.listEvents(owner);
	}
	
	@GET
	@Path("/list/{owner}/events/all")
	public List<BigDataStackEvent> listEventsAll(@PathParam("owner") String owner){
		return querying.listEvents(owner);
	}
	
	@GET
	@Path("/list/{owner}/events/error")
	public List<BigDataStackEvent> listEventsError(@PathParam("owner") String owner){
		return querying.listEvents(owner, BigDataStackEventSeverity.Error);
	}
	
	@GET
	@Path("/list/{owner}/events/alert")
	public List<BigDataStackEvent> listEventsAlert(@PathParam("owner") String owner){
		return querying.listEvents(owner, BigDataStackEventSeverity.Alert);
	}
	
	@GET
	@Path("/list/{owner}/events/info")
	public List<BigDataStackEvent> listEventsInfo(@PathParam("owner") String owner){
		return querying.listEvents(owner, BigDataStackEventSeverity.Info);
	}
	
	@GET
	@Path("/list/{owner}/events/warning")
	public List<BigDataStackEvent> listEventsWarning(@PathParam("owner") String owner){
		return querying.listEvents(owner, BigDataStackEventSeverity.Warning);
	}
	
	@GET
	@Path("/list/{owner}/events/all/{type}")
	public List<BigDataStackEvent> listEventsAll(@PathParam("owner") String owner, @PathParam("type") String type){
		return querying.listEvents(owner, BigDataStackEventType.valueOf(type));
	}
	
	@GET
	@Path("/list/{owner}/events/error/{type}")
	public List<BigDataStackEvent> listEventsError(@PathParam("owner") String owner, @PathParam("type") String type){
		return querying.listEvents(owner, BigDataStackEventSeverity.Error, BigDataStackEventType.valueOf(type));
	}
	
	@GET
	@Path("/list/{owner}/events/alert/{type}")
	public List<BigDataStackEvent> listEventsAlert(@PathParam("owner") String owner, @PathParam("type") String type){
		return querying.listEvents(owner, BigDataStackEventSeverity.Alert, BigDataStackEventType.valueOf(type));
	}
	
	@GET
	@Path("/list/{owner}/events/info/{type}")
	public List<BigDataStackEvent> listEventsInfo(@PathParam("owner") String owner, @PathParam("type") String type){
		return querying.listEvents(owner, BigDataStackEventSeverity.Info, BigDataStackEventType.valueOf(type));
	}
	
	@GET
	@Path("/list/{owner}/events/warning/{type}")
	public List<BigDataStackEvent> listEventsWarning(@PathParam("owner") String owner, @PathParam("type") String type){
		return querying.listEvents(owner, BigDataStackEventSeverity.Warning, BigDataStackEventType.valueOf(type));
	}
	
	
	@GET
	@Path("/list/{owner}/kevents")
	public List<BigDataStackEvent> listEventsK(@PathParam("owner") String owner, @QueryParam("k") String k){
		List<BigDataStackEvent> events = querying.listEvents(owner);
		int kValue = Integer.valueOf(k);
		if (events.size()<kValue) return events;
		else return events.subList(0, kValue);
	}
	
	@GET
	@Path("/list/{owner}/kevents/all")
	public List<BigDataStackEvent> listEventsAllK(@PathParam("owner") String owner, @QueryParam("k") String k){
		List<BigDataStackEvent> events =  querying.listEvents(owner);
		int kValue = Integer.valueOf(k);
		if (events.size()<kValue) return events;
		else return events.subList(0, kValue);
	}
	
	@GET
	@Path("/list/{owner}/kevents/error")
	public List<BigDataStackEvent> listEventsErrorK(@PathParam("owner") String owner, @QueryParam("k") String k){
		List<BigDataStackEvent> events =  querying.listEvents(owner, BigDataStackEventSeverity.Error);
		int kValue = Integer.valueOf(k);
		if (events.size()<kValue) return events;
		else return events.subList(0, kValue);
	}
	
	@GET
	@Path("/list/{owner}/kevents/alert")
	public List<BigDataStackEvent> listEventsAlertK(@PathParam("owner") String owner, @QueryParam("k") String k){
		List<BigDataStackEvent> events =  querying.listEvents(owner, BigDataStackEventSeverity.Alert);
		int kValue = Integer.valueOf(k);
		if (events.size()<kValue) return events;
		else return events.subList(0, kValue);
	}
	
	@GET
	@Path("/list/{owner}/kevents/info")
	public List<BigDataStackEvent> listEventsInfoK(@PathParam("owner") String owner, @QueryParam("k") String k){
		List<BigDataStackEvent> events =  querying.listEvents(owner, BigDataStackEventSeverity.Info);
		int kValue = Integer.valueOf(k);
		if (events.size()<kValue) return events;
		else return events.subList(0, kValue);
	}
	
	@GET
	@Path("/list/{owner}/kevents/warning")
	public List<BigDataStackEvent> listEventsWarningK(@PathParam("owner") String owner, @QueryParam("k") String k){
		List<BigDataStackEvent> events =  querying.listEvents(owner, BigDataStackEventSeverity.Warning);
		int kValue = Integer.valueOf(k);
		if (events.size()<kValue) return events;
		else return events.subList(0, kValue);
	}
	
	@GET
	@Path("/list/{owner}/kevents/all/{type}")
	public List<BigDataStackEvent> listEventsAllK(@PathParam("owner") String owner, @PathParam("type") String type, @QueryParam("k") String k){
		List<BigDataStackEvent> events =  querying.listEvents(owner, BigDataStackEventType.valueOf(type));
		int kValue = Integer.valueOf(k);
		if (events.size()<kValue) return events;
		else return events.subList(0, kValue);
	}
	
	@GET
	@Path("/list/{owner}/kevents/error/{type}")
	public List<BigDataStackEvent> listEventsErrorK(@PathParam("owner") String owner, @PathParam("type") String type, @QueryParam("k") String k){
		List<BigDataStackEvent> events =  querying.listEvents(owner, BigDataStackEventSeverity.Error, BigDataStackEventType.valueOf(type));
		int kValue = Integer.valueOf(k);
		if (events.size()<kValue) return events;
		else return events.subList(0, kValue);
	}
	
	@GET
	@Path("/list/{owner}/kevents/alert/{type}")
	public List<BigDataStackEvent> listEventsAlertK(@PathParam("owner") String owner, @PathParam("type") String type, @QueryParam("k") String k){
		List<BigDataStackEvent> events =  querying.listEvents(owner, BigDataStackEventSeverity.Alert, BigDataStackEventType.valueOf(type));
		int kValue = Integer.valueOf(k);
		if (events.size()<kValue) return events;
		else return events.subList(0, kValue);
	}
	
	@GET
	@Path("/list/{owner}/kevents/info/{type}")
	public List<BigDataStackEvent> listEventsInfoK(@PathParam("owner") String owner, @PathParam("type") String type, @QueryParam("k") String k){
		List<BigDataStackEvent> events =  querying.listEvents(owner, BigDataStackEventSeverity.Info, BigDataStackEventType.valueOf(type));
		int kValue = Integer.valueOf(k);
		if (events.size()<kValue) return events;
		else return events.subList(0, kValue);
	}
	
	@GET
	@Path("/list/{owner}/kevents/warning/{type}")
	public List<BigDataStackEvent> listEventsWarningK(@PathParam("owner") String owner, @PathParam("type") String type, @QueryParam("k") String k){
		List<BigDataStackEvent> events =  querying.listEvents(owner, BigDataStackEventSeverity.Warning, BigDataStackEventType.valueOf(type));
		int kValue = Integer.valueOf(k);
		if (events.size()<kValue) return events;
		else return events.subList(0, kValue);
	}
	
	@GET
	@Path("/list/{owner}/{appID}/events/{objectID}")
	public List<BigDataStackEvent> listEvents(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("objectID") String objectID, @QueryParam("instance") String instance){
		if (instance==null) return querying.listEvents(owner, appID, objectID, -1);
		else return querying.listEvents(owner, appID, objectID, Integer.parseInt(instance));
	}
	
	@GET
	@Path("/delete/{owner}/{namespace}/{appID}")
	public boolean deleteApp(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("namespace") String namespace){
		manager.cleanupSequenceRunners(owner, namespace, appID);
		return manager.deleteApp(owner, namespace, appID);
	}
	
	@GET
	@Path("/delete/{owner}/{namespace}/{appID}/osrs")
	public boolean deleteOSRs(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("namespace") String namespace){
		return manager.cleanupSequenceRunners(owner, namespace, appID);
	}
	
	
	@GET
	@Path("/query/{owner}/{appID}/{namespace}/metrics/{metricName}/{objectID}")
	public BigDataStackMetricValue queryMetricValues(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("namespace") String namespace, @PathParam("metricName") String metricName, @PathParam("objectID") String objectID, @QueryParam("time") String time){
		
		if (time==null) return manager.prometheusDataClient.basicQuery(owner, namespace, appID, objectID, null, metricName);
		else return manager.prometheusDataClient.basicQuery(owner, namespace, appID, objectID, null, metricName, time);
	}
	
	
	@GET
	@Path("/query/{owner}/{appID}/logs/{objectID}")
	public Map<String,BigDataStackSearchResponse> queryLogs(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("objectID") String objectID, @QueryParam("query") String query){
		
		Map<String,BigDataStackSearchResponse> responses = new HashMap<String,BigDataStackSearchResponse>();
		List<BigDataStackObjectDefinition> objects = querying.listObjectInstances(owner, appID, objectID);
		
		for (BigDataStackObjectDefinition object : objects) {
			try {
				if (object==null) continue;
				
				BigDataStackSearchResponse response = SearchUtil.basicSearch(query, (OpenshiftOperationFabric8ioClient)manager.openshiftOperationClient, object);
				if (response!=null) responses.put(object.getObjectID()+"-"+object.getInstance(), response);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


		return responses;
	}
	
	
	@GET
	@Path("/query/{owner}/{appID}/{namespace}/metrics/{metricName}/{objectID}/{instanceID}")
	public BigDataStackMetricValue queryMetricValues(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("namespace") String namespace, @PathParam("metricName") String metricName, @PathParam("objectID") String objectID, @PathParam("instanceID") String instanceID, @QueryParam("time") String time){
		
		if (time==null) return manager.prometheusDataClient.basicQuery(owner, namespace, appID, objectID, instanceID, metricName);
		else return manager.prometheusDataClient.basicQuery(owner, namespace, appID, objectID, instanceID, metricName, time);
	}
	
	
	@POST
	@Path("/event/{owner}/{appID}/{objectID}")
	public boolean registerNewEvent(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("objectID") String objectID, BigDataStackEvent event) {
		try {
			return manager.eventUtil.registerEvent(event.getAppID(), event.getOwner(), event.getNamepace(), event.getType(), event.getSeverity(), event.getTitle(), event.getDescription(), event.getObjectID(), event.getInstance());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	//----------------------------------------------------------
	// Operations
	//----------------------------------------------------------

	@GET
	@Path("/exe/monitor/{owner}/{namespace}/start")
	public boolean startMonitoringNamespace(@PathParam("owner") String owner, @PathParam("namespace") String namespace) {
		return manager.startMonitoringNamespace(namespace, owner);
	}
	
	@GET
	@Path("/exe/monitor/{owner}/{namespace}/stop")
	public boolean stopMonitoringNamespace(@PathParam("owner") String owner, @PathParam("namespace") String namespace) {
		return manager.stopMonitoringNamespace(namespace, owner);
	}
	
	@GET
	@Path("/exe/{owner}/{appID}/{sequenceID}/start")
	public BigDataStackOperationSequence executeSequenceFromTemplate(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("sequenceID") String sequenceID) {
		try {
			BigDataStackOperationSequence sequenceTemplate = manager.sequenceTemplateClient.getOperationSequence(appID, sequenceID, 0, owner);
			return manager.executeSequenceFromTemplate(sequenceTemplate);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@POST
	@Path("/exe/{owner}/{appID}/{sequenceID}/start")
	public BigDataStackOperationSequence executeSequenceFromTemplateParam(@PathParam("owner") String owner, @PathParam("appID") String appID, @PathParam("sequenceID") String sequenceID, Map<String,String> params) {
		try {
			BigDataStackOperationSequence sequenceTemplate = manager.sequenceTemplateClient.getOperationSequence(appID, sequenceID, 0, owner);
			return manager.executeSequenceFromTemplate(sequenceTemplate, params);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	//----------------------------------------------------------
	// Reports
	//----------------------------------------------------------
	
	@GET
	@Path("/report/{owner}/{namespace}/realizationStatus")
	public RealizationStatus realizationStatus(@PathParam("owner") String owner, @PathParam("namespace") String namespace) {
		return manager.generateRealizationStatus(owner, namespace);
	}
	
	@GET
	@Path("/report/{owner}/{namespace}/eventTimeSeries")
	public EventTimeSeries eventTimeSeries(@PathParam("owner") String owner, @PathParam("namespace") String namespace) {
		return manager.generateEventTimeSeries(owner, namespace);
	}
	
	@GET
	@Path("/report/{owner}/{namespace}/costPerHour")
	public PerHourTimeSeries costPerHour(@PathParam("owner") String owner, @PathParam("namespace") String namespace) {
		return manager.generatePerHourTimeSeries(owner, namespace, "costPerHour");
	}
	
	@GET
	@Path("/report/{owner}/{namespace}/executingStatus")
	public ExecutingStatus executingStatus(@PathParam("owner") String owner, @PathParam("namespace") String namespace) {
		return manager.generateExecutingStatus(owner, namespace);
	}
	
	@GET
	@Path("/report/{owner}/{namespace}/routeList")
	public RouteList routeList(@PathParam("owner") String owner, @PathParam("namespace") String namespace) {
		return manager.generateRouteList(owner, namespace);
	}
	
	@GET
	@Path("/drc/default")
	public List<BigDataStackResourceTemplateAdv> getDefaultRemplates() {
		return ResourceTemplateRecommendationUtil.getDefaultRecommendations();
	}
	
	
	
}
