package org.terrier.realization.operations;

import java.util.ArrayList;
import java.util.List;

import org.terrier.realization.openshift.OpenshiftObject;
import org.terrier.realization.openshift.OpenshiftOperationClient;
import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.prometheus.PrometheusDataClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.threads.OperationSequenceThread;
import org.terrier.realization.util.EventUtil;

import com.fasterxml.jackson.databind.JsonNode;
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

public class ParameterFromObjectLookup extends BigDataStackOperation{

	private String appID;
	private String owner;
	private String namepace;
	
	private String parameter;
	private String criteria;
	private boolean allowMultipleMatches = false;
	private boolean selectFirst = false;
	private boolean ifPodGetIP = false;
	
	public ParameterFromObjectLookup() {
		this.className = this.getClass().getName();
	}
	
	public ParameterFromObjectLookup(String appID, String owner, String namepace, String parameter, String criteria) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.criteria = criteria;
		this.parameter =parameter;
		
		this.className = this.getClass().getName();
	}
	
	public ParameterFromObjectLookup(String appID, String owner, String namepace, String parameter, String criteria, boolean allowMultiplerMatches, boolean selectFirst) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.criteria = criteria;
		this.parameter =parameter;
		this.allowMultipleMatches= allowMultiplerMatches;
		this.selectFirst = selectFirst;
		
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

	public String getNamepace() {
		return namepace;
	}

	public void setNamepace(String namepace) {
		this.namepace = namepace;
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	public String getCriteria() {
		return criteria;
	}

	public void setCriteria(String criteria) {
		this.criteria = criteria;
	}

	public boolean isAllowMultipleMatches() {
		return allowMultipleMatches;
	}

	public void setAllowMultipleMatches(boolean allowMultipleMatches) {
		this.allowMultipleMatches = allowMultipleMatches;
	}

	public boolean isSelectFirst() {
		return selectFirst;
	}

	public void setSelectFirst(boolean selectFirst) {
		this.selectFirst = selectFirst;
	}

	@Override
	public String defaultDescription() {
		return "Records the parameter "+parameter+" to an objectID determined by a criteria search on openshift";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		
		try {
			String[] criteriaParts = criteria.split(":");
			String namespace = criteriaParts[0];
			String kind = criteriaParts[1];
			String nameregex = criteriaParts[2];
			
			List<OpenshiftObject> resources = openshiftStatusClient.getResources(namespace, kind);
			List<OpenshiftObject> matchedResources = new ArrayList<OpenshiftObject>();
			for (OpenshiftObject resource : resources) {
				if (resource.getName().matches(nameregex)) {
					matchedResources.add(resource);
				}
			}
			
			if (matchedResources.size()==0) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Parameter From Object Lookup Operation Failed using criteria '"+criteria+"'",
						"Attempted to match an object in namespace '"+namespace+"' of kind '"+kind+"' with name '"+nameregex+"' but no matches were found.",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			} if (matchedResources.size()>1) {
				if (allowMultipleMatches) {
					List<String> matches = new ArrayList<String>(matchedResources.size());
					for (OpenshiftObject resource : matchedResources) {
						if (ifPodGetIP) {
							String ip = resource.ifPodGetIP();
							if (ip!=null) matches.add(ip);
							else matches.add(resource.getName());
						}
						else matches.add(resource.getName());
					}
					ObjectMapper mapper = new ObjectMapper();
					parentSequenceRunner.getSequence().getParameters().put(parameter, mapper.writeValueAsString(matches));
					return true;
				} else if (selectFirst) {
					if (ifPodGetIP) {
						String ip = matchedResources.get(0).ifPodGetIP();
						if (ip!=null) parentSequenceRunner.getSequence().getParameters().put(parameter, ip);
						else parentSequenceRunner.getSequence().getParameters().put(parameter, matchedResources.get(0).getName());
					} else parentSequenceRunner.getSequence().getParameters().put(parameter, matchedResources.get(0).getName());
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Info,
							"Parameter From Object Lookup Operation Completed: '"+parameter+"' -> '"+matchedResources.get(0).getName()+"'",
							"Matched an object in namespace'"+namespace+"' of kind '"+kind+"' with name '"+nameregex+"' and used it to set parameter '"+parameter+"'",
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
							"Parameter From Object Lookup Operation Failed using criteria '"+criteria+"'",
							"Attempted to match an object in namespace '"+namespace+"' of kind '"+kind+"' with name '"+nameregex+"' but found multiple matches and multipleMatches was not allowed.",
							parentSequenceRunner.getSequence().getSequenceID(),
							parentSequenceRunner.getSequence().getIndex()
							);
					return false;
				}
			} else {
				if (ifPodGetIP) {
					String ip = matchedResources.get(0).ifPodGetIP();
					if (ip!=null) parentSequenceRunner.getSequence().getParameters().put(parameter, ip);
					else parentSequenceRunner.getSequence().getParameters().put(parameter, matchedResources.get(0).getName());
				} else parentSequenceRunner.getSequence().getParameters().put(parameter, matchedResources.get(0).getName());
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Info,
						"Parameter From Object Lookup Operation Completed: '"+parameter+"' -> '"+matchedResources.get(0).getName()+"'",
						"Matched an object in namespace'"+namespace+"' of kind '"+kind+"' with name '"+nameregex+"' and used it to set parameter '"+parameter+"'",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return true;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
		
	}
	
	@Override
	public void initalizeParameters(JsonNode configJson) {
		parameter = configJson.get("parameter").asText();
		criteria = configJson.get("criteria").asText();
		if (configJson.has("multipleMatches")) {
			String multipleMatches = configJson.get("multipleMatches").asText();
			if (multipleMatches.equalsIgnoreCase("SelectFirst")) selectFirst = true;
			if (multipleMatches.equalsIgnoreCase("Allow")) allowMultipleMatches = true;
		}
		if (configJson.has("ifPodGetIP")) {
			ifPodGetIP = Boolean.parseBoolean(configJson.get("ifPodGetIP").asText());
		}
	}

	@Override
	public String getObjectID() {
		return "ParentSequence";
	}

	public boolean isIfPodGetIP() {
		return ifPodGetIP;
	}

	public void setIfPodGetIP(boolean ifPodGetIP) {
		this.ifPodGetIP = ifPodGetIP;
	}
}
