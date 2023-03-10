package org.terrier.realization.operations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class GrepFileCheck extends BigDataStackOperation {

	private String appID;
	private String owner;
	private String namepace;
	
	private String criteria;
	private String targetFile;

	public GrepFileCheck() {
		this.className = this.getClass().getName();
	};
	
	public GrepFileCheck(String appID, String owner, String namepace, String criteria, String targetFile) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.criteria = criteria;
		this.targetFile = targetFile;
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
	
	@Override
	public String defaultDescription() {
		return "Performs a regular expression check against the contents of a file.";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		
		for (String paramKey : parentSequenceRunner.getSequence().getParameters().keySet()) {
			targetFile = targetFile.replaceAll("\\$"+paramKey+"\\$", parentSequenceRunner.getSequence().getParameters().get(paramKey));
		}
		
		File f = new File(targetFile);
		
		StringBuilder fileContents = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
			String line;
			while ((line = br.readLine())!=null) {
				fileContents.append(line);
				fileContents.append("\n");
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			
			try {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"GrepFileCheck Operation Failed when trying to open '"+targetFile+"'",
						"GrepFileCheck Operation Failed when trying to open '"+targetFile+"'",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			return false;
		}
		
		Pattern pattern = Pattern.compile(criteria, Pattern.CASE_INSENSITIVE);
	    Matcher matcher = pattern.matcher(fileContents.toString());
	    boolean matchFound = matcher.find();
		
		return matchFound;

	}

	@Override
	public String getObjectID() {
		return null;
	}
	
	public String getNamepace() {
		return namepace;
	}

	public void setNamepace(String namepace) {
		this.namepace = namepace;
	}

	public String getCriteria() {
		return criteria;
	}

	public void setCriteria(String criteria) {
		this.criteria = criteria;
	}

	public String getTargetFile() {
		return targetFile;
	}

	public void setTargetFile(String targetFile) {
		this.targetFile = targetFile;
	}

	@Override
	public void initalizeParameters(JsonNode configJson) {
		targetFile = configJson.get("targetFile").asText();
		criteria = configJson.get("criteria").asText();
		
	}

}
