package org.terrier.realization.application;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.terrier.realization.openshift.OpenshiftOperationClient;
import org.terrier.realization.openshift.OpenshiftOperationFabric8ioClient;
import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.openshift.OpenshiftStatusFabric8ioClient;
import org.terrier.realization.operations.Apply;
import org.terrier.realization.operations.BigDataStackOperation;
import org.terrier.realization.operations.Instantiate;
import org.terrier.realization.prometheus.PrometheusDataClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.BigDataStackAppStateIO;
import org.terrier.realization.state.jdbc.BigDataStackApplicationIO;
import org.terrier.realization.state.jdbc.BigDataStackCredentialsIO;
import org.terrier.realization.state.jdbc.BigDataStackEventIO;
import org.terrier.realization.state.jdbc.BigDataStackMetricIO;
import org.terrier.realization.state.jdbc.BigDataStackMetricValueIO;
import org.terrier.realization.state.jdbc.BigDataStackNamespaceStateIO;
import org.terrier.realization.state.jdbc.BigDataStackObjectIO;
import org.terrier.realization.state.jdbc.BigDataStackOperationSequenceIO;
import org.terrier.realization.state.jdbc.BigDataStackPodStatusIO;
import org.terrier.realization.state.jdbc.BigDataStackSLOIO;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.state.jdbc.LXDB;
import org.terrier.realization.state.jdbc.MySQLDB;
import org.terrier.realization.structures.config.DatabaseConf;
import org.terrier.realization.structures.config.GDTConfig;
import org.terrier.realization.structures.config.OpenshiftConfig;
import org.terrier.realization.structures.config.RabbitMQConf;
import org.terrier.realization.structures.data.BigDataStackAppState;
import org.terrier.realization.structures.data.BigDataStackApplication;
import org.terrier.realization.structures.data.BigDataStackApplicationType;
import org.terrier.realization.structures.data.BigDataStackCredentials;
import org.terrier.realization.structures.data.BigDataStackCredentialsType;
import org.terrier.realization.structures.data.BigDataStackEvent;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.structures.data.BigDataStackMetric;
import org.terrier.realization.structures.data.BigDataStackNamespaceState;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackObjectType;
import org.terrier.realization.structures.data.BigDataStackOperationSequence;
import org.terrier.realization.structures.data.BigDataStackOperationSequenceMode;
import org.terrier.realization.structures.data.BigDataStackPodStatus;
import org.terrier.realization.structures.data.BigDataStackSLO;
import org.terrier.realization.structures.reports.EventTimeSeries;
import org.terrier.realization.structures.reports.ExecutingStatus;
import org.terrier.realization.structures.reports.PerHourTimeSeries;
import org.terrier.realization.structures.reports.RealizationReport;
import org.terrier.realization.structures.reports.RealizationStatus;
import org.terrier.realization.structures.reports.RouteList;
import org.terrier.realization.threads.OperationSequenceThread;
import org.terrier.realization.util.ApplicationStateUtil;
import org.terrier.realization.util.EventUtil;
import org.terrier.realization.util.GDTFileUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

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
 * This is the main manager class for the Realization Engine
 *
 */
public class GDTManager implements Manager {

	GDTConfig gdtConfig; 

	ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
	ObjectMapper jsonMapper = new ObjectMapper();

	JDBCDB database;
	public OpenshiftOperationClient openshiftOperationClient;
	public OpenshiftStatusClient openshiftStatusClient;
	public RabbitMQClient mailboxClient;
	public PrometheusDataClient prometheusDataClient;

	public EventUtil eventUtil;

	public BigDataStackApplicationIO appClient;
	public BigDataStackEventIO eventClient;
	public BigDataStackMetricIO metricClient;
	public BigDataStackObjectIO objectInstanceClient;
	public BigDataStackObjectIO objectTemplateClient;
	public BigDataStackOperationSequenceIO sequenceInstanceClient;
	public BigDataStackOperationSequenceIO sequenceTemplateClient;
	public BigDataStackPodStatusIO podStatusClient;
	public BigDataStackNamespaceStateIO namespaceStateClient;
	public BigDataStackCredentialsIO credentialsClient;
	public BigDataStackMetricValueIO metricValueClient;
	public BigDataStackSLOIO sloClient;
	public BigDataStackAppStateIO appStateClient;

	BigDataStackCredentials databaseCredential;
	BigDataStackCredentials openshiftCredential;
	BigDataStackCredentials rabbitMQCredential;

	public GDTManager(File gdtJsonConfigFile) throws Exception {
		gdtConfig = new GDTConfig(gdtJsonConfigFile);
		initClients();
	}

	public GDTManager(GDTConfig gdtConfig) throws Exception {
		this.gdtConfig = gdtConfig;
		initClients();
	}

	public void initClients() throws SQLException {
		// initalize the database client
		DatabaseConf dbconf = gdtConfig.getDatabase();
		if (dbconf.getType().equalsIgnoreCase("mysql")) {
			if (dbconf.getPassword()!=null && dbconf.getPassword().length()>0) database = new MySQLDB(dbconf.getHost(), dbconf.getPort(), dbconf.getName(), dbconf.getUsername(), dbconf.getPassword());
			else database = new MySQLDB(dbconf.getHost(), dbconf.getPort(), dbconf.getName(), dbconf.getUsername());
		} else if (dbconf.getType().equalsIgnoreCase("lxdb")) {
			if (dbconf.getPassword()!=null && dbconf.getPassword().length()>0) database = new LXDB(dbconf.getHost(), dbconf.getPort(), dbconf.getName(), dbconf.getUsername(), dbconf.getPassword());
			else database = new LXDB(dbconf.getHost(), dbconf.getPort(), dbconf.getName(), dbconf.getUsername());
		} else {
			System.err.println("Database type "+dbconf.getType()+" not recognised");
			System.exit(1);
		}


		appClient = new BigDataStackApplicationIO(database);
		eventClient = new BigDataStackEventIO(database);
		metricClient = new BigDataStackMetricIO(database);
		objectInstanceClient = new BigDataStackObjectIO(database, false);
		objectTemplateClient = new BigDataStackObjectIO(database, true);
		sequenceInstanceClient = new BigDataStackOperationSequenceIO(database, false);
		sequenceTemplateClient = new BigDataStackOperationSequenceIO(database, true);
		podStatusClient = new BigDataStackPodStatusIO(database);
		namespaceStateClient = new BigDataStackNamespaceStateIO(database);
		credentialsClient = new BigDataStackCredentialsIO(database);
		metricValueClient = new BigDataStackMetricValueIO(database);
		sloClient = new BigDataStackSLOIO(database);
		appStateClient = new BigDataStackAppStateIO(database);


		// Add database credentials
		databaseCredential = new BigDataStackCredentials("GDT", gdtConfig.getDatabase().getUsername(), gdtConfig.getDatabase().getPassword(), BigDataStackCredentialsType.database);
		//if (!credentialsClient.addCredential(databaseCredential)) credentialsClient.updatePassweord("GDT", BigDataStackCredentialsType.database, gdtConfig.getDatabase().getUsername(), gdtConfig.getDatabase().getPassword());

		// Initalize Openshift Clients
		OpenshiftConfig openshiftConf = gdtConfig.getOpenshift();
		openshiftCredential = new BigDataStackCredentials("GDT", gdtConfig.getOpenshift().getUsername(), gdtConfig.getOpenshift().getPassword(), BigDataStackCredentialsType.openshift);
		//if (!credentialsClient.addCredential(openshiftCredential)) credentialsClient.updatePassweord("GDT", BigDataStackCredentialsType.openshift, gdtConfig.getOpenshift().getUsername(), gdtConfig.getOpenshift().getPassword());
		openshiftOperationClient = null;
		
		if (openshiftConf.getClient().equalsIgnoreCase("fabric8io")) {
			openshiftOperationClient = new OpenshiftOperationFabric8ioClient(openshiftConf.getHost(), openshiftConf.getPort(), openshiftConf.getUsername(), openshiftConf.getPassword(), openshiftConf.getNamespace());
			openshiftOperationClient.connectToOpenshift();
			openshiftStatusClient = new OpenshiftStatusFabric8ioClient(((OpenshiftOperationFabric8ioClient)openshiftOperationClient).getOsClient());
			((OpenshiftStatusFabric8ioClient)openshiftStatusClient).setHost(openshiftConf.getHost());
			((OpenshiftStatusFabric8ioClient)openshiftStatusClient).setPort(openshiftConf.getPort());
			((OpenshiftStatusFabric8ioClient)openshiftStatusClient).setUsername(openshiftConf.getUsername());
			((OpenshiftStatusFabric8ioClient)openshiftStatusClient).setPassword(openshiftConf.getPassword());
		}
		if (openshiftOperationClient==null) {
			System.err.println("Openshift client '"+openshiftConf.getClient()+"' is not supported for operations");
			return;
		}


		//if (occlient.equalsIgnoreCase("openshift3")) openshiftStatus = new OpenshiftStatusClientv3(openshiftConf.getHost(), openshiftConf.getPort(), openshiftConf.getUsername(), openshiftConf.getPassword());


		// Initalize RabbitMQ Client
		RabbitMQConf rabbitMQConf = gdtConfig.getRabbitmq();
		mailboxClient = new RabbitMQClient(rabbitMQConf.getHost(), rabbitMQConf.getPort(), rabbitMQConf.getUsername(), rabbitMQConf.getPassword());
		rabbitMQCredential = new BigDataStackCredentials("GDT", gdtConfig.getRabbitmq().getUsername(), gdtConfig.getRabbitmq().getPassword(), BigDataStackCredentialsType.rabbitmq);
		//if (!credentialsClient.addCredential(rabbitMQCredential)) credentialsClient.updatePassweord("GDT", BigDataStackCredentialsType.rabbitmq, gdtConfig.getRabbitmq().getUsername(), gdtConfig.getRabbitmq().getPassword());


		// Initalize Prometheus Data Client
		prometheusDataClient = new PrometheusDataClient(openshiftConf.getHostExtension());


		// Initalize Utilities
		eventUtil = new EventUtil(eventClient, mailboxClient);


	}

	/**
	 * Registers a new BigDataStack Application with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackApplication registerApplication(String yaml) {
		try {
			BigDataStackApplication app = yamlMapper.readValue(yaml, BigDataStackApplication.class);
			return registerApplication(app, null, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new BigDataStack Application with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackApplication registerApplication(String yaml, String namespace) {
		try {
			BigDataStackApplication app = yamlMapper.readValue(yaml, BigDataStackApplication.class);
			return registerApplication(app, namespace, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new BigDataStack Application with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackApplication registerApplication(String yaml, String namespace, String owner) {
		try {
			BigDataStackApplication app = yamlMapper.readValue(yaml, BigDataStackApplication.class);
			return registerApplication(app, namespace, owner);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * Registers a new BigDataStack SLO with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackSLO registerSLO(String yaml) {
		try {
			BigDataStackSLO app = yamlMapper.readValue(yaml, BigDataStackSLO.class);
			return registerSLO(app, null, null, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new BigDataStack SLO with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackSLO registerSLO(String yaml, String namespace, String owner, String appID) {
		try {
			
			JsonNode node = yamlMapper.readTree(yaml);
			
			if (!node.has("safetyChecks") || !node.get("safetyChecks").isArray()) return null;
			Iterator<JsonNode> operationI = node.get("safetyChecks").iterator();
			List<BigDataStackOperation> operations = new ArrayList<BigDataStackOperation>(5);
			while (operationI.hasNext()) {
				JsonNode operationJson = operationI.next();
				String className = operationJson.get("className").asText();
				if (className.startsWith("eu.bigdatastack.gdt.operations.")) className = className.replace("eu.bigdatastack.gdt.operations.", "");
				

				@SuppressWarnings("deprecation")
				BigDataStackOperation operation = (BigDataStackOperation) Class.forName("eu.bigdatastack.gdt.operations."+className).newInstance();
				operation.setAppID(appID);
				operation.setNamespace(namespace);
				operation.setOwner(owner);
				operation.initalizeFromJson(operationJson);
				
				// check to see if we have a pre-stored configuration and use it if found
				if (operationJson.has("configJson") && !operationJson.get("configJson").isNull()) {
					operation.initalizeFromJson(operationJson.get("configJson"));
				}
				operations.add(operation);
			}

			BigDataStackSLO app = new BigDataStackSLO(appID, 
					owner, 
					namespace, 
					node.get("triggerID").asText(),
					node.get("metricName").asText(),
					node.get("triggerMessage").asText(),
					node.get("type").asText(),
					node.get("value").asDouble(),
					BigDataStackEventSeverity.valueOf(node.get("breachSeverity").asText()),
					node.get("action").asText(),
					operations,
					node.get("coolDownMins").asInt()
			);
			
			return registerSLO(app, namespace, owner, appID);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new BigDataStack Application from a BigDataStackApplication object
	 * @param yaml
	 * @return
	 */
	protected BigDataStackSLO registerSLO(BigDataStackSLO slo, String namespace, String owner, String appID) {
		if (namespace!=null) slo.setNamespace(namespace);
		if (owner!=null) slo.setOwner(owner);
		if (appID!=null) slo.setAppID(appID);

		try {

			

			if (!sloClient.addSLO(slo)) {
				if (!sloClient.updateSLO(slo)) {
			
						eventUtil.registerEvent(
								slo.getAppID(),
								slo.getOwner(),
								slo.getNamespace(),
								BigDataStackEventType.ObjectRegistry,
								BigDataStackEventSeverity.Error,
								"New SLO Failed to Register: '"+slo.getAppID()+"|"+slo.getTriggerID()+"' targeting "+slo.getMetricName(),
								"Tried to create a new service level objective for '"+slo.getAppID()+"|"+slo.getTriggerID()+"' targeting "+slo.getMetricName()+" but was rejected, likely because it already exists.",
								slo.getTriggerID(),
								-1
								);
						return null;
					}
				}


				eventUtil.registerEvent(
						slo.getAppID(),
						slo.getOwner(),
						slo.getNamespace(),
						BigDataStackEventType.ObjectRegistry,
						BigDataStackEventSeverity.Info,
						"New SLO Registered: '"+slo.getAppID()+"|"+slo.getTriggerID()+"' targeting "+slo.getMetricName(),
						"Created a new service level objective for '"+slo.getAppID()+"|"+slo.getTriggerID()+"' targeting "+slo.getMetricName(),
						slo.getTriggerID(),
						-1
						);
				return slo;	

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new BigDataStack SLO with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackAppState registerApplicationState(String yaml, BigDataStackApplication app) {
		try {
			BigDataStackAppState state = GDTFileUtil.readApplicationStateFromString(yaml, app);
			return registerApplicationState(state, null, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}



	/**
	 * Registers a new BigDataStack Application from a BigDataStackApplication object
	 * @param yaml
	 * @return
	 */
	protected BigDataStackAppState registerApplicationState(BigDataStackAppState appState, String namespace, String owner) {
		if (namespace!=null) appState.setNamespace(namespace);
		if (owner!=null) appState.setOwner(owner);

		try {
			if (!appStateClient.addAppState(appState)) {
				if (!appStateClient.updateAppState(appState)) {
					eventUtil.registerEvent(
							appState.getAppID(),
							appState.getOwner(),
							appState.getNamespace(),
							BigDataStackEventType.ObjectRegistry,
							BigDataStackEventSeverity.Error,
							"New App State Failed to Register: '"+appState.getAppID()+"|"+appState.getAppStateID()+"'",
							"Tried to create a new possible application state '"+appState.getAppID()+"|"+appState.getAppStateID()+"', but was rejected, likely because it already exists.",
							appState.getAppStateID(),
							-1
							);
					return null;
				}
			}

			eventUtil.registerEvent(
					appState.getAppID(),
					appState.getOwner(),
					appState.getNamespace(),
					BigDataStackEventType.ObjectRegistry,
					BigDataStackEventSeverity.Info,
					"New App State Registered: '"+appState.getAppID()+"|"+appState.getAppStateID()+"'",
					"Created a new possible application state '"+appState.getAppID()+"|"+appState.getAppStateID()+"'",
					appState.getAppStateID(),
					-1
					);
			return appState;	

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * Registers a new BigDataStack Application from a BigDataStackApplication object
	 * @param yaml
	 * @return
	 */
	protected BigDataStackApplication registerApplication(BigDataStackApplication app, String namespace, String owner) {
		if (namespace!=null) app.setNamespace(namespace);
		if (owner!=null) app.setOwner(owner);

		try {
			if (!appClient.addApplication(app)) {
				if (!appClient.updateApp(app)) {
					eventUtil.registerEvent(
							app.getAppID(),
							app.getOwner(),
							app.getNamespace(),
							BigDataStackEventType.ObjectRegistry,
							BigDataStackEventSeverity.Error,
							"New Application Failed to Register: '"+app.getAppID()+"'",
							"Tried to create a new application '"+app.getAppID()+"', but failed, likely due to an existing app with the same ID already existing",
							app.getAppID(),
							-1
							);
					return null;
				}
			}

			eventUtil.registerEvent(
					app.getAppID(),
					app.getOwner(),
					app.getNamespace(),
					BigDataStackEventType.ObjectRegistry,
					BigDataStackEventSeverity.Info,
					"New Application Registered: '"+app.getAppID()+"'",
					"A new application was created '"+app.getAppID()+"'",
					app.getAppID(),
					-1
					);
			return app;	

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new BigDataStack Application with the database from a yaml format File
	 * @param yamlFile
	 * @return
	 */
	public BigDataStackApplication registerApplication(File yamlFile) {
		try {
			String yaml = GDTFileUtil.file2String(yamlFile, "UTF-8");
			return registerApplication(yaml);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new BigDataStack Application with the database from a yaml format File
	 * @param yamlFile
	 * @return
	 */
	public BigDataStackApplication registerApplication(File yamlFile, String namespace, String owner) {
		try {
			String yaml = GDTFileUtil.file2String(yamlFile, "UTF-8");
			return registerApplication(yaml, namespace, owner);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new BigDataStack Object Definition with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackObjectDefinition registerObject(String yaml) {
		return registerObject(yaml, null);
	}

	/**
	 * Registers a new BigDataStack Object Definition with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackObjectDefinition registerObject(String yaml, BigDataStackApplication app) {
		try {
			BigDataStackObjectDefinition object = GDTFileUtil.readObjectFromString(yaml, app);
			if (!objectTemplateClient.addObject(object)) {
				if (!objectTemplateClient.updateObject(object)) {
					eventUtil.registerEvent(
							"GDT",
							object.getOwner(),
							"None",
							BigDataStackEventType.ObjectRegistry,
							BigDataStackEventSeverity.Info,
							"New Object Definition Template Failed to Register: '"+object.getObjectID()+"'",
							"Tried to create a new object template '"+object.getObjectID()+"' but failed, likely due to a template with the same ID already existing",
							object.getObjectID(),
							object.getInstance()
							);
					return null;
				}

			}

			eventUtil.registerEvent(
					"GDT",
					object.getOwner(),
					"None",
					BigDataStackEventType.ObjectRegistry,
					BigDataStackEventSeverity.Info,
					"New Object Definition Template Registered: '"+object.getObjectID()+"'",
					"A new object template was created '"+object.getObjectID()+"'",
					object.getObjectID(),
					object.getInstance()
					);
			return object;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new BigDataStack Object Definition with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackObjectDefinition registerObject(String yaml, String namespace, String owner) {
		try {
			BigDataStackObjectDefinition object = GDTFileUtil.readObjectFromString(yaml, null);
			object.setNamespace(namespace);
			object.setOwner(owner);
			if (!objectTemplateClient.addObject(object)) {
				if (!objectTemplateClient.updateObject(object)) {
					eventUtil.registerEvent(
							object.getAppID(),
							object.getOwner(),
							object.getNamespace(),
							BigDataStackEventType.ObjectRegistry,
							BigDataStackEventSeverity.Info,
							"New Object Definition Template Failed to Register: '"+object.getObjectID()+"'",
							"Tried to create a new object template '"+object.getObjectID()+"' but failed, likely due to a template with the same ID already existing",
							object.getObjectID(),
							object.getInstance()
							);
					return null;
				}

			}

			eventUtil.registerEvent(
					object.getAppID(),
					object.getOwner(),
					object.getNamespace(),
					BigDataStackEventType.ObjectRegistry,
					BigDataStackEventSeverity.Info,
					"New Object Definition Template Registered: '"+object.getObjectID()+"'",
					"A new object template was created '"+object.getObjectID()+"'",
					object.getObjectID(),
					object.getInstance()
					);
			return object;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * Registers a new BigDataStack Object Definition with the database from a yaml format File
	 * @param yamlFile
	 * @return
	 */
	public BigDataStackObjectDefinition registerObject(File yamlFile) {
		try {
			String yaml = GDTFileUtil.file2String(yamlFile, "UTF-8");
			return registerObject(yaml);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new BigDataStack Object Definition with the database from a yaml format File
	 * @param yamlFile
	 * @return
	 */
	public BigDataStackObjectDefinition registerObject(File yamlFile, String namespace) {
		try {
			String yaml = GDTFileUtil.file2String(yamlFile, "UTF-8");
			return registerObject(yaml, namespace, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new BigDataStack Object Definition with the database from a yaml format File
	 * @param yamlFile
	 * @return
	 */
	public BigDataStackObjectDefinition registerObject(File yamlFile, String namespace, String owner) {
		try {
			String yaml = GDTFileUtil.file2String(yamlFile, "UTF-8");
			return registerObject(yaml, namespace, owner);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * Registers a new BigDataStack Metric with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackMetric registerMetric(String yaml, String owner) {
		try {
			BigDataStackMetric object = yamlMapper.readValue(yaml, BigDataStackMetric.class);
			object.setOwner(owner);
			if (!metricClient.addMetric(object)) {
				if (!metricClient.updateMetric(object)) {
					eventUtil.registerEvent(
							"Metric",
							object.getOwner(),
							"None",
							BigDataStackEventType.ObjectRegistry,
							BigDataStackEventSeverity.Info,
							"New Metric Failed to Register: '"+object.getName()+"'",
							"Tried to create a newmetric '"+object.getName()+"' but failed, likely due to a metric with the same ID already existing",
							object.getName(),
							-1
							);
					return null;
				}
			}

			eventUtil.registerEvent(
					"Metric",
					object.getOwner(),
					"None",
					BigDataStackEventType.ObjectRegistry,
					BigDataStackEventSeverity.Info,
					"New Metric Registered: '"+object.getName()+"'",
					"A new metric was created '"+object.getName()+"'",
					object.getName(),
					-1
					);
			return object;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new BigDataStack Metric with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackMetric registerMetric(String yaml) {
		try {
			BigDataStackMetric object = yamlMapper.readValue(yaml, BigDataStackMetric.class);
			if (!metricClient.addMetric(object)) {
				if (!metricClient.updateMetric(object)) {
					eventUtil.registerEvent(
							"Metric",
							object.getOwner(),
							"None",
							BigDataStackEventType.ObjectRegistry,
							BigDataStackEventSeverity.Info,
							"New Metric Failed to Register: '"+object.getName()+"'",
							"Tried to create a newmetric '"+object.getName()+"' but failed, likely due to a metric with the same ID already existing",
							object.getName(),
							-1
							);
					return null;
				}
			}

			eventUtil.registerEvent(
					"Metric",
					object.getOwner(),
					"None",
					BigDataStackEventType.ObjectRegistry,
					BigDataStackEventSeverity.Info,
					"New Metric Registered: '"+object.getName()+"'",
					"A new metric was created '"+object.getName()+"'",
					object.getName(),
					-1
					);
			return object;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * Registers a new BigDataStack Metric with the database from a yaml format File
	 * @param yamlFile
	 * @return
	 */
	public BigDataStackMetric registerMetric(File yamlFile) {
		try {
			String yaml = GDTFileUtil.file2String(yamlFile, "UTF-8");
			return registerMetric(yaml);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Creates a simple operation sequence template comprised of applying a single object
	 * provided. 
	 * @param object
	 * @return
	 */
	public boolean createOperationSequence(BigDataStackApplication app, BigDataStackObjectDefinition object, String sequenceID) {
		try {
			Instantiate instantiateOperation = new Instantiate(app.getAppID(), app.getOwner(), app.getNamespace(), object.getObjectID(), "defaultInstanceRef");
			Apply applyOperation = new Apply(app.getAppID(), app.getOwner(), app.getNamespace(), "defaultInstanceRef");
			List<BigDataStackOperation> operations = new ArrayList<BigDataStackOperation>(1);
			operations.add(instantiateOperation);
			operations.add(applyOperation);

			BigDataStackOperationSequence sequence = new BigDataStackOperationSequence(
					app.getAppID(),
					object.getOwner(), 
					app.getNamespace(),
					0, 
					sequenceID, 
					"Generic auto-created sequence for '"+object.getObjectID()+"'",
					"This is an automatically generated sequence in Run mode comprising of Instantiate then Apply for '"+object.getObjectID()+"'", 
					operations, 
					BigDataStackOperationSequenceMode.Run);

			if (!sequenceTemplateClient.addSequence(sequence)) {
				eventUtil.registerEvent(
						app.getAppID(),
						object.getOwner(),
						app.getNamespace(),
						BigDataStackEventType.ObjectRegistry,
						BigDataStackEventSeverity.Error,
						"Automatic Operation Sequence Template Creation Failed: '"+object.getObjectID()+"'",
						"Attempted to construct an Operation Sequence Template automatically containing '"+object.getObjectID()+"', but the registry rejected it, there may already be a sequence with that ID.",
						object.getObjectID(),
						object.getInstance()
						);
				return false;
			} else {
				eventUtil.registerEvent(
						app.getAppID(),
						object.getOwner(),
						app.getNamespace(),
						BigDataStackEventType.ObjectRegistry,
						BigDataStackEventSeverity.Info,
						"Automatic Operation Sequence Template Creation Complete: '"+object.getObjectID()+"'",
						"Constructed an Operation Sequence Template automatically containing '"+object.getObjectID()+"', this sequence can now be instantiated.",
						object.getObjectID(),
						object.getInstance()
						);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Triggers the execution of an operation sequence from an existing sequence template. This will
	 * generate a sequence instance, and as needed, object instances. 
	 * 
	 * This is the synchronous version of the call (will not exit until the sequence ends or fails).
	 * @param sequenceTemplate
	 * @return
	 */
	public boolean executeSequenceFromTemplateSync(BigDataStackOperationSequence sequenceTemplate, Map<String,String> parameters) {

		OperationSequenceThread thread = new OperationSequenceThread(
				database,
				openshiftOperationClient,
				openshiftStatusClient,
				mailboxClient,
				prometheusDataClient,
				sequenceTemplate,
				parameters,
				eventUtil);

		thread.run();
		return !thread.hasFailed();
	}

	/**
	 * Triggers the execution of an operation sequence from an existing sequence template. This will
	 * generate a sequence instance, and as needed, object instances. 
	 * 
	 * This is the synchronous version of the call (will not exit until the sequence ends or fails).
	 * @param sequenceTemplate
	 * @return
	 */
	public boolean executeSequenceFromTemplateSync(BigDataStackOperationSequence sequenceTemplate) {

		return executeSequenceFromTemplateSync(sequenceTemplate, new HashMap<String,String>());
	}

	/**
	 * Registers a new namespace with the database (this does not trigger the start of monitoring)
	 * @param namespace
	 * @return
	 */
	public BigDataStackNamespaceState registerNamespace(String yaml) {
		try {
			BigDataStackNamespaceState namespace = yamlMapper.readValue(yaml, BigDataStackNamespaceState.class);
			if (namespace.getClusterMonitoringHost()==null) namespace.setClusterMonitoringHost("");
			if (namespace.getEventExchangeHost()==null) namespace.setEventExchangeHost("");
			if (namespace.getLogSearchHost()==null) namespace.setLogSearchHost("");
			if (namespace.getMetricStoreHost()==null) namespace.setMetricStoreHost("");
			if (!namespaceStateClient.addNamespace(namespace)) {
				eventUtil.registerEvent(
						"GDT",
						"None",
						namespace.getNamespace(),
						BigDataStackEventType.ObjectRegistry,
						BigDataStackEventSeverity.Error,
						"Failed adding namespace : '"+namespace.getNamespace()+"'",
						"Attempted to add a new namepace '"+namespace.getNamespace()+"', but the registry rejected it, there may already be a namespace with that name.",
						namespace.getNamespace(),
						-1
						);
				return null;
			} else {
				eventUtil.registerEvent(
						"GDT",
						"None",
						namespace.getNamespace(),
						BigDataStackEventType.ObjectRegistry,
						BigDataStackEventSeverity.Info,
						"Added new namespace : '"+namespace.getNamespace()+"'",
						"Successfully added a new namepace '"+namespace.getNamespace()+"'",
						namespace.getNamespace(),
						-1
						);
				return namespace;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new namespace with the database (this does not trigger the start of monitoring)
	 * @param yamlFile
	 * @return
	 */
	public BigDataStackNamespaceState registerNamespace(File yamlFile) {
		try {
			String yaml = GDTFileUtil.file2String(yamlFile, "UTF-8");
			return registerNamespace(yaml);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new operation sequence with the database
	 * @param namespace
	 * @return
	 */
	public BigDataStackOperationSequence registerOperationSequence(String yaml) {
		return registerOperationSequence(yaml, null, null);
	}


	/**
	 * Registers a new operation sequence with the database
	 * @param namespace
	 * @return
	 */
	public BigDataStackOperationSequence registerOperationSequence(String yaml, String namespace, String owner) {
		try {
			BigDataStackOperationSequence sequence = GDTFileUtil.readSequenceFromString(yaml, namespace, owner);
			if (namespace!=null) sequence.setNamepace(namespace);
			if (owner!=null) sequence.setOwner(owner);

			if (!sequenceTemplateClient.addSequence(sequence)) {
				if (!sequenceTemplateClient.updateSequence(sequence)) {
					eventUtil.registerEvent(
							sequence.getAppID(),
							sequence.getOwner(),
							sequence.getNamespace(),
							BigDataStackEventType.ObjectRegistry,
							BigDataStackEventSeverity.Error,
							"Failed adding operation sequence template : '"+sequence.getSequenceID()+"'",
							"Attempted to add a new operation sequence template '"+sequence.getSequenceID()+"', but the registry rejected it, there may already be a template with that ID.",
							sequence.getSequenceID(),
							sequence.getIndex()
							);
					return null;
				}

			}

			eventUtil.registerEvent(
					sequence.getAppID(),
					sequence.getOwner(),
					sequence.getNamespace(),
					BigDataStackEventType.ObjectRegistry,
					BigDataStackEventSeverity.Info,
					"Registered operation sequence template : '"+sequence.getSequenceID()+"'",
					"Added a new operation sequence template '"+sequence.getSequenceID()+"' to the registry",
					sequence.getSequenceID(),
					sequence.getIndex()
					);
			return sequence;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new operation sequence with the database
	 * @param yamlFile
	 * @return
	 */
	public BigDataStackOperationSequence registerOperationSequence(File yamlFile) {
		try {
			String yaml = GDTFileUtil.file2String(yamlFile, "UTF-8");
			return registerOperationSequence(yaml);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new operation sequence with the database
	 * @param yamlFile
	 * @return
	 */
	public BigDataStackOperationSequence registerOperationSequence(File yamlFile, String namespace) {
		try {
			String yaml = GDTFileUtil.file2String(yamlFile, "UTF-8");
			return registerOperationSequence(yaml, namespace, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers a new operation sequence with the database
	 * @param yamlFile
	 * @return
	 */
	public BigDataStackOperationSequence registerOperationSequence(File yamlFile, String namespace, String owner) {
		try {
			String yaml = GDTFileUtil.file2String(yamlFile, "UTF-8");
			return registerOperationSequence(yaml, namespace, owner);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Attempts to start the monitoring service for a namespace
	 * @param namespace
	 * @param owner
	 * @return
	 */
	public boolean startMonitoringNamespace(String namespace, String owner) {

		try {

			// load the GDT Monitor
			loadPlaybook(GDTFileUtil.file2String(new File("resources/gdt/monitor.playbook.yaml"), "UTF-8"), owner, namespace);

			// Launch the Operation Sequence
			Map<String,String> parameters = new HashMap<String,String>();
			// database info
			parameters.put("dbtype", gdtConfig.getDatabase().getType());
			parameters.put("dbhost", gdtConfig.getDatabase().getHost());
			parameters.put("dbport", String.valueOf(gdtConfig.getDatabase().getPort()));
			parameters.put("dbname", gdtConfig.getDatabase().getName());
			// database credentials
			parameters.put("dbusername", databaseCredential.getUsername());
			parameters.put("dbpassword", databaseCredential.getPassword());
			// openshift info
			parameters.put("occlient", gdtConfig.getOpenshift().getClient());
			parameters.put("ochost", gdtConfig.getOpenshift().getHost());
			parameters.put("ocport", String.valueOf(gdtConfig.getOpenshift().getPort()));
			parameters.put("ochostextension", gdtConfig.getOpenshift().getHostExtension());
			parameters.put("ocimagerepositoryhost", gdtConfig.getOpenshift().getImageRepositoryHost());

			// openshift credentials
			parameters.put("ocusername", openshiftCredential.getUsername());
			parameters.put("ocpassword", openshiftCredential.getPassword());
			// rabbitmq info
			parameters.put("rmqhost", gdtConfig.getRabbitmq().getHost());
			parameters.put("rmqport", String.valueOf(gdtConfig.getRabbitmq().getPort()));
			// rabbitmq credentials
			parameters.put("rmqusername", rabbitMQCredential.getUsername());
			parameters.put("rmqpassword", rabbitMQCredential.getPassword());

			BigDataStackOperationSequence existingSequenceTemplate = sequenceTemplateClient.getSequence("gdtdefaultapp", "seq-gdtmonitor");
			boolean monitoringOk = executeSequenceFromTemplateSync(existingSequenceTemplate, parameters);

			if (!monitoringOk) return false;

			// load the GDT Prometheus Instance
			loadPlaybook(GDTFileUtil.file2String(new File("resources/gdt/prometheus.playbook.yaml"), "UTF-8"), owner, namespace);

			existingSequenceTemplate = sequenceTemplateClient.getSequence("gdtdefaultapp", "seq-prometheusdeploy");
			boolean prometheusOk = executeSequenceFromTemplateSync(existingSequenceTemplate, parameters);

			if (!prometheusOk) return false;

			// load the GDT API Instance
			loadPlaybook(GDTFileUtil.file2String(new File("resources/gdt/api.playbook.yaml"), "UTF-8"), owner, namespace);

			existingSequenceTemplate = sequenceTemplateClient.getSequence("gdtdefaultapp", "seq-gdtapi");
			boolean apiOk = executeSequenceFromTemplateSync(existingSequenceTemplate, parameters);

			if (!apiOk) return false;

			if (gdtConfig.getOpenshift().getOpenshiftPrometheus()!=null) {
				// load the GDT Resource Monitor Instance
				loadPlaybook(GDTFileUtil.file2String(new File("resources/gdt/resource.playbook.yaml"), "UTF-8"), owner, namespace);

				parameters.put("prometheusHost", gdtConfig.getOpenshift().getOpenshiftPrometheus());

				existingSequenceTemplate = sequenceTemplateClient.getSequence("gdtdefaultapp", "seq-resourcemonitor");
				boolean resourceOk = executeSequenceFromTemplateSync(existingSequenceTemplate, parameters);

				if (!resourceOk) return false;
			}




			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	/**
	 * Halts monitoring of a namespace
	 * @param namespace
	 * @param owner
	 * @return
	 */
	public boolean stopMonitoringNamespace(String namespace, String owner) {

		try {


			boolean stoppedMonitor =  stopOperationSequenceInstances(owner, namespace, "gdtdefaultapp", "seq-gdtmonitor");
			boolean stoppedPrometheus =  stopOperationSequenceInstances(owner, namespace, "gdtdefaultapp", "seq-prometheusdeploy");
			boolean stoppedAPI =  stopOperationSequenceInstances(owner, namespace, "gdtdefaultapp", "seq-gdtapi");

			return (stoppedMonitor && stoppedPrometheus && stoppedAPI);


		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}


	/**
	 * Triggers the execution of an operation sequence from an existing sequence template. This will
	 * generate a sequence instance, and as needed, object instances. 
	 * 
	 * This is the asynchronous version of the call, i.e. it will generate a pod to orchestrate the sequence.
	 * @param sequenceTemplate
	 * @param parameters
	 * @return
	 */
	public BigDataStackOperationSequence executeSequenceFromTemplate(BigDataStackOperationSequence sequenceTemplate, Map<String,String> parameters) {

		if (parameters==null) parameters = new HashMap<String,String>();
		parameters.put("appID", sequenceTemplate.getAppID());
		parameters.put("namespace", sequenceTemplate.getNamespace());
		parameters.put("owner", sequenceTemplate.getOwner());
		parameters.put("sequenceID", sequenceTemplate.getSequenceID());
		parameters.put("dbusername", databaseCredential.getUsername());
		parameters.put("dbpassword", databaseCredential.getPassword());
		parameters.put("dbtype", gdtConfig.getDatabase().getType());
		parameters.put("dbhost", gdtConfig.getDatabase().getHost());
		parameters.put("dbport", String.valueOf(gdtConfig.getDatabase().getPort()));
		parameters.put("dbname", gdtConfig.getDatabase().getName());
		parameters.put("occlient", gdtConfig.getOpenshift().getClient());
		parameters.put("ochost", gdtConfig.getOpenshift().getHost());
		parameters.put("ocport", String.valueOf(gdtConfig.getOpenshift().getPort()));
		parameters.put("ocusername", openshiftCredential.getUsername());
		parameters.put("ocpassword", openshiftCredential.getPassword());
		parameters.put("ochostextension", gdtConfig.getOpenshift().getHostExtension());
		parameters.put("ocimagerepositoryhost", gdtConfig.getOpenshift().getImageRepositoryHost());
		parameters.put("rmqhost", gdtConfig.getRabbitmq().getHost());
		parameters.put("rmqport", String.valueOf(gdtConfig.getRabbitmq().getPort()));
		parameters.put("rmqusername", rabbitMQCredential.getUsername());
		parameters.put("rmqpassword", rabbitMQCredential.getPassword());

		// attempt to create the instance now rather than creating it later in the operation sequence thread as we need
		// to set parameters now rather than pass them to the container
		int failedAttempts = 0;
		boolean sequenceAdded = false;

		BigDataStackOperationSequence newSequenceInstance = null;

		try {
			while (!sequenceAdded) {
				List<BigDataStackOperationSequence> existingSequenceInstances = sequenceInstanceClient.getOperationSequences(sequenceTemplate.getAppID(), sequenceTemplate.getSequenceID());
				int highestIndex = 0;
				for (BigDataStackOperationSequence sequenceInstance : existingSequenceInstances) {
					if (sequenceInstance.getIndex()>highestIndex) highestIndex = sequenceInstance.getIndex();
				}

				int newIndex = highestIndex+1;
				newSequenceInstance = sequenceTemplate.clone();
				newSequenceInstance.setIndex(newIndex);

				if (parameters!=null) {
					for (String paramKey : parameters.keySet()) {
						newSequenceInstance.getParameters().put(paramKey, parameters.get(paramKey));
					}
					parameters.put("sequenceInstance", String.valueOf(newIndex));
				}

				sequenceAdded = sequenceInstanceClient.addSequence(newSequenceInstance);
				if (!sequenceAdded) {
					failedAttempts++;
					if (failedAttempts>=5) {
						return null;
					} else continue;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		try {
			if (sequenceAdded) {
				// now try launching the operation sequence pod
				eventUtil.registerEvent(
						newSequenceInstance.getAppID(),
						newSequenceInstance.getOwner(),
						newSequenceInstance.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Info,
						"New Operation Sequence Created: '"+newSequenceInstance.getSequenceID()+"'",
						"The user created a new operation sequence for app '"+newSequenceInstance.getAppID()+"', it has been registered and is being processed (SequenceID='"+newSequenceInstance.getSequenceID()+"', Index='"+newSequenceInstance.getIndex()+"')",
						newSequenceInstance.getSequenceID(),
						newSequenceInstance.getIndex()
						);

				BigDataStackObjectDefinition operationsequenceDef = null;
				
				if (newSequenceInstance!=null) {
					if (newSequenceInstance.getParameters().containsKey("overrideSequencePodDef")) {
						
						eventUtil.registerEvent(
								newSequenceInstance.getAppID(),
								newSequenceInstance.getOwner(),
								newSequenceInstance.getNamespace(),
								BigDataStackEventType.GlobalDecisionTracker,
								BigDataStackEventSeverity.Info,
								"Operation Sequence: '"+newSequenceInstance.getSequenceID()+"' uses an alternative runner pod definition "+newSequenceInstance.getParameters().get("overrideSequencePodDef"),
								"Detected an override request for the default operation sequence runner in app '"+newSequenceInstance.getAppID()+"' (SequenceID='"+newSequenceInstance.getSequenceID()+"', Index='"+newSequenceInstance.getIndex()+"')",
								newSequenceInstance.getSequenceID(),
								newSequenceInstance.getIndex()
								);
						
						operationsequenceDef = objectTemplateClient.getObject(newSequenceInstance.getParameters().get("overrideSequencePodDef"), newSequenceInstance.getOwner());
						
						if (operationsequenceDef==null) {
							eventUtil.registerEvent(
									newSequenceInstance.getAppID(),
									newSequenceInstance.getOwner(),
									newSequenceInstance.getNamespace(),
									BigDataStackEventType.GlobalDecisionTracker,
									BigDataStackEventSeverity.Warning,
									"Operation Sequence: '"+newSequenceInstance.getSequenceID()+"' requested alternative runner pod definition "+newSequenceInstance.getParameters().get("overrideSequencePodDef")+" but an object with that id was not found!",
									"Failed to switch to the specified operation sequence runner in app '"+newSequenceInstance.getAppID()+"' (SequenceID='"+newSequenceInstance.getSequenceID()+"', Index='"+newSequenceInstance.getIndex()+"')",
									newSequenceInstance.getSequenceID(),
									newSequenceInstance.getIndex()
									);
						}
					}
				}
				
				if (operationsequenceDef == null) {
					operationsequenceDef = GDTFileUtil.readObjectFromString(GDTFileUtil.file2String(new File("resources/gdt/operationsequence.pod.yaml"), "UTF-8"),null);
				}
				
				
				
				operationsequenceDef.setNamespace(sequenceTemplate.getNamespace());
				operationsequenceDef.setAppID(sequenceTemplate.getAppID());
				operationsequenceDef.setOwner(sequenceTemplate.getOwner());
				String yaml = operationsequenceDef.getYamlSource();

				for (String paramKey : parameters.keySet()) {
					yaml = yaml.replaceAll("\\$"+paramKey+"\\$", parameters.get(paramKey));
				}
				for (String paramKey : newSequenceInstance.getParameters().keySet()) {
					yaml = yaml.replaceAll("\\$"+paramKey+"\\$", newSequenceInstance.getParameters().get(paramKey));
				}
				operationsequenceDef.setYamlSource(yaml);

				// Now register the sequence pod (this is needed so we can track its progress)
				int failedReg = 0;
				boolean regOK = false;
				while (!regOK) {
					List<BigDataStackObjectDefinition> objects = objectInstanceClient.getObjects(operationsequenceDef.getObjectID(), operationsequenceDef.getOwner(), null, null);
					int highestIndex = 0;
					for (BigDataStackObjectDefinition objectInstance : objects) {
						if (objectInstance.getInstance()>highestIndex) highestIndex = objectInstance.getInstance();
					}

					int newIndex = highestIndex+1;
					operationsequenceDef.setInstance(newIndex);

					String yamlCopy = ""+yaml;
					yamlCopy = yamlCopy.replaceAll("\\$runnerIndex\\$", String.valueOf(newIndex));
					operationsequenceDef.setYamlSource(yamlCopy);

					regOK = objectInstanceClient.addObject(operationsequenceDef);
					if (!regOK) failedReg++;
					else parameters.put("runnerIndex", String.valueOf(newIndex));
					if (failedReg==5) {
						eventUtil.registerEvent(
								newSequenceInstance.getAppID(),
								newSequenceInstance.getOwner(),
								newSequenceInstance.getNamespace(),
								BigDataStackEventType.GlobalDecisionTracker,
								BigDataStackEventSeverity.Error,
								"Failed to register the operation sequence running for '"+newSequenceInstance.getSequenceID()+"', with the database",
								"The last step of launcing an operation sequence is registering the runner object, but this failed for '"+newSequenceInstance.getSequenceID()+"', this means that the sequence runner will be un-tracked and hence subsequent deletion of the sequence will not kill the runner if it is still active",
								newSequenceInstance.getSequenceID(),
								newSequenceInstance.getIndex()
								);
						return null;
					}
				}


				yaml = yaml.replaceAll("\\$runnerIndex\\$", parameters.get("runnerIndex"));
				operationsequenceDef.setYamlSource(yaml);

				newSequenceInstance.getParameters().put("runnerIndex", parameters.get("runnerIndex"));
				sequenceInstanceClient.updateSequence(newSequenceInstance);

				openshiftOperationClient.applyOperation(operationsequenceDef);

			} else {
				eventUtil.registerEvent(
						newSequenceInstance.getAppID(),
						newSequenceInstance.getOwner(),
						newSequenceInstance.getNamespace(),
						BigDataStackEventType.ObjectRegistry,
						BigDataStackEventSeverity.Error,
						"Operation Sequence Creation Failed for: '"+newSequenceInstance.getSequenceID()+"'",
						"Tried to create a new operation sequence for app '"+newSequenceInstance.getAppID()+"' but failed when adding to the Object Registry (SequenceID='"+newSequenceInstance.getSequenceID()+"', Index='"+newSequenceInstance.getIndex()+"')",
						newSequenceInstance.getSequenceID(),
						newSequenceInstance.getIndex()
						);
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return newSequenceInstance;
	}

	/**
	 * Triggers the execution of an operation sequence from an existing sequence template. This will
	 * generate a sequence instance, and as needed, object instances. 
	 * 
	 * This is the asynchronous version of the call, i.e. it will generate a pod to orchestrate the sequence.
	 * @param sequenceTemplate
	 * @return
	 */
	public BigDataStackOperationSequence executeSequenceFromTemplate(BigDataStackOperationSequence sequenceTemplate) {
		Map<String,String> params = new HashMap<String,String>();
		return executeSequenceFromTemplate(sequenceTemplate, params);

	}


	public void shutdown() {
		openshiftOperationClient.close();
		openshiftStatusClient.close();
	}

	public BigDataStackApplicationIO getAppClient() {
		return appClient;
	}

	public BigDataStackObjectIO getObjectTemplateClient() {
		return objectTemplateClient;
	}

	public BigDataStackOperationSequenceIO getSequenceTemplateClient() {
		return sequenceTemplateClient;
	}

	public BigDataStackOperationSequenceIO getSequenceInstanceClient() {
		return sequenceInstanceClient;
	}

	/**
	 * Directly deletes an object instance, if there are underlying pods they will also be deleted. 
	 * @param owner
	 * @param appID
	 * @param objectID
	 * @param instance
	 * @return
	 */
	public boolean deleteObjectInstance(String owner, String appID, String objectID, int instance) {
		try {
			BigDataStackObjectDefinition object = objectInstanceClient.getObject(objectID, owner, instance);
			if (object==null) return false;

			boolean deleted =  openshiftOperationClient.deleteOperation(object);
			if (!deleted) return false;
			eventUtil.registerEvent(
					appID,
					owner,
					object.getNamespace(),
					BigDataStackEventType.Openshift,
					BigDataStackEventSeverity.Info,
					"Object '"+objectID+"("+instance+")' Deleted",
					"Deleted the cluster instance associated to '"+objectID+"("+instance+")', this will kill running pods, but the object will remain in the database.",
					objectID,
					object.getInstance()
					);

			object.getStatus().add("Deleted");
			objectInstanceClient.updateObject(object);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Directly deletes all instances for an object, if there are underlying pods they will also be deleted. 
	 * @param owner
	 * @param appID
	 * @param objectID
	 * @param instance
	 * @return
	 */
	public boolean deleteObjectInstances(String owner, String appID, String objectID) {
		boolean anyFailed = false;
		try {
			List<BigDataStackObjectDefinition> objects = objectInstanceClient.getObjects(objectID, owner, null, appID);
			for (BigDataStackObjectDefinition object : objects) {
				boolean deleted =  openshiftOperationClient.deleteOperation(object);
				if (deleted==false) {
					anyFailed = true;
					continue;
				}
				eventUtil.registerEvent(
						appID,
						owner,
						object.getNamespace(),
						BigDataStackEventType.Openshift,
						BigDataStackEventSeverity.Info,
						"Object '"+objectID+"("+object.getInstance()+")' Deleted",
						"Deleted the cluster instance associated to '"+objectID+"("+object.getInstance()+")', this will kill running pods, but the object will remain in the database.",
						objectID,
						object.getInstance()
						);

				object.getStatus().add("Deleted");
				objectInstanceClient.updateObject(object);
			}

			return anyFailed;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean stopOperationSequenceInstances(String owner, String namespace, String appID, String sequenceID) {
		try {
			List<BigDataStackOperationSequence> sequenceInstances = sequenceInstanceClient.getOperationSequences(owner, appID, sequenceID);
			for (BigDataStackOperationSequence sequence : sequenceInstances) {
				if (!sequence.getNamepace().equalsIgnoreCase(namespace)) continue;
				boolean ok = stopOperationSequenceInstance(owner, appID, sequenceID, sequence.getIndex(), namespace);
				if (ok ) {
					eventUtil.registerEvent(
							appID,
							owner,
							sequence.getNamespace(),
							BigDataStackEventType.Openshift,
							BigDataStackEventSeverity.Info,
							"Stopped Operation Sequence Instance '"+sequenceID+"("+sequence.getIndex()+")'",
							"Stopped operation sequence instance '"+sequenceID+"("+sequence.getIndex()+")' and all of its components on the cluster",
							sequenceID,
							sequence.getIndex()
							);
				} else {
					eventUtil.registerEvent(
							appID,
							owner,
							sequence.getNamespace(),
							BigDataStackEventType.Openshift,
							BigDataStackEventSeverity.Info,
							"Failure detected during stop for operation sequence instance '"+sequenceID+"("+sequence.getIndex()+")'",
							"Stopped operation sequence instance '"+sequenceID+"("+sequence.getIndex()+")' failed due to an internal error",
							sequenceID,
							sequence.getIndex()
							);
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	public boolean stopOperationSequenceInstance(String owner, String appID, String sequenceID, int instance, String namespace) {
		try {
			BigDataStackOperationSequence sequence = sequenceInstanceClient.getSequence(appID, sequenceID, instance);
			if (sequence==null) return false;
			if (namespace!=null && !sequence.getNamepace().equalsIgnoreCase(namespace)) return false;

			// First try and kill the sequence runner (this may not exist)
			List<BigDataStackObjectDefinition> sequenceRunners = objectInstanceClient.getObjects("operationsequence", "gdt", sequence.getNamepace(), appID);
			boolean foundRunner = false;
			for (BigDataStackObjectDefinition sequenceRunner : sequenceRunners) {
				// weird sub-case where we want to check whether the object yaml has labels
				JsonNode yamlSource = yamlMapper.readTree(sequenceRunner.getYamlSource());
				JsonNode metadata = yamlSource.get("metadata");
				JsonNode labels = metadata.get("labels");
				if (!labels.has("appID") || !labels.get("appID").asText().equalsIgnoreCase(appID)) continue;
				if (!labels.has("sequenceID") || !labels.get("sequenceID").asText().equalsIgnoreCase(sequenceID)) continue;
				if (!labels.has("sequenceInstance") || !labels.get("sequenceInstance").asText().equalsIgnoreCase(String.valueOf(instance))) continue;
				foundRunner = true;

				// if we get to here then we have found a sequence runner for the specified sequence and instance
				boolean deleted =  openshiftOperationClient.deleteOperation(sequenceRunner); // try deleting it

				if (deleted) {
					eventUtil.registerEvent(
							appID,
							owner,
							sequenceRunner.getNamespace(),
							BigDataStackEventType.Openshift,
							BigDataStackEventSeverity.Info,
							"Stop Operation Sequence: Runner for '"+sequenceID+"("+instance+")' Deleted",
							"Deleted an operation sequence runner for '"+sequenceID+"("+instance+")', this will stop future operations from starting.",
							sequenceID,
							sequenceRunner.getInstance()
							);

					// update the object status
					Set<String> status = new HashSet<String>();
					status.add("Killed");
					sequenceRunner.setStatus(status);
					objectInstanceClient.updateObject(sequenceRunner);
				}

			}

			if (!foundRunner) {
				eventUtil.registerEvent(
						appID,
						owner,
						sequence.getNamespace(),
						BigDataStackEventType.Openshift,
						BigDataStackEventSeverity.Info,
						"Stop Operation Sequence: No runner found '"+sequenceID+"("+instance+")', skipping",
						"Searcher for a matching runner pod for '"+sequenceID+"("+instance+")', but found none.",
						sequenceID,
						-1
						);
			}

			// Now try and delete all of the underlying objects spawned from this operation sequence
			for (BigDataStackOperation operation : sequence.getOperations()) {

				//System.err.println(operation.getClassName());

				// Apply Operations can spawn objects that need deleted
				if (operation.getClassName().equalsIgnoreCase("eu.bigdatastack.gdt.operations.Apply")) {
					String instanceRef = operation.getObjectID(); // apply operations use the instance ref as their object id

					// resolve the instance ref to get the identifiers for the object
					String sourceObjectID = sequence.getParameters().get(instanceRef).split(":")[0];
					int instanceNo = Integer.valueOf(sequence.getParameters().get(instanceRef).split(":")[1]);

					// get the object instance
					BigDataStackObjectDefinition objectInstance = objectInstanceClient.getObject(sourceObjectID, owner, instanceNo);

					if (!openshiftOperationClient.deleteOperation(objectInstance)) {
						eventUtil.registerEvent(
								operation.getAppID(),
								operation.getOwner(),
								operation.getNamespace(),
								BigDataStackEventType.Openshift,
								BigDataStackEventSeverity.Error,
								"Tried to deleted object '"+sourceObjectID+"("+instanceNo+")', but failed",
								"Tried to delete the object '"+sourceObjectID+"("+instanceNo+" on the cluster, but openshift rejected it",
								sourceObjectID,
								instanceNo
								);

					} else {

						Set<String> status = new HashSet<String>();
						status.add("Killed");
						objectInstance.setStatus(status);
						objectInstanceClient.updateObject(objectInstance);

						eventUtil.registerEvent(
								operation.getAppID(),
								operation.getOwner(),
								operation.getNamespace(),
								BigDataStackEventType.Openshift,
								BigDataStackEventSeverity.Info,
								"Deleted cluster object '"+sourceObjectID+"("+instanceNo+")'",
								"Deleted the cluster object '"+sourceObjectID+"("+instanceNo+")",
								sourceObjectID,
								instanceNo
								);
					}
				}

			}




			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}


	public boolean cleanupSequenceRunners(String owner, String namespace, String appID) {
		try {

			// First try and kill the sequence runner (this may not exist)
			List<BigDataStackObjectDefinition> sequenceRunners = objectInstanceClient.getObjects("operationsequence", "gdt", namespace, appID);
			for (BigDataStackObjectDefinition sequenceRunner : sequenceRunners) {
				// weird sub-case where we want to check whether the object yaml has labels
				JsonNode yamlSource = yamlMapper.readTree(sequenceRunner.getYamlSource());
				JsonNode metadata = yamlSource.get("metadata");
				JsonNode labels = metadata.get("labels");
				if (!labels.has("appID") || !labels.get("appID").asText().equalsIgnoreCase(appID)) continue;

				// if we get to here then we have found a sequence runner for the specified sequence and instance
				boolean deleted =  openshiftOperationClient.deleteOperation(sequenceRunner); // try deleting it
				objectInstanceClient.delete(owner, namespace, appID, sequenceRunner.getObjectID(), -1);
				if (!deleted) return false;

			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Aggregates status information for the Realization Core Components
	 * @param owner
	 * @param namespace
	 * @return
	 */
	public RealizationStatus generateRealizationStatus(String owner, String namespace) {
		try {
			// Stage 1: Get Realization engine status
			RealizationStatus realizationStatuses = new RealizationStatus();

			List<BigDataStackObjectDefinition> deploymentconfigs = objectInstanceClient.getObjectList(owner, namespace, "gdtdefaultapp", BigDataStackObjectType.DeploymentConfig);

			for (BigDataStackObjectDefinition dc :  deploymentconfigs) {
				Set<String> reportedStatuses = dc.getStatus();
				if (reportedStatuses.contains("Available")) {
					if (dc.getObjectID().equalsIgnoreCase("gdtapi")) realizationStatuses.getApiInstance2Status().put(String.valueOf(dc.getInstance()), reportedStatuses);
					if (dc.getObjectID().equalsIgnoreCase("costestimator")) realizationStatuses.getCostestimatorInstance2Status().put(String.valueOf(dc.getInstance()), reportedStatuses);
					if (dc.getObjectID().equalsIgnoreCase("gdtmonitor")) realizationStatuses.getMonitorInstance2Status().put(String.valueOf(dc.getInstance()), reportedStatuses);
					if (dc.getObjectID().equalsIgnoreCase("prometheus")) realizationStatuses.getPrometheusInstance2Status().put(String.valueOf(dc.getInstance()), reportedStatuses);
				}
			}

			// if we got this far then the db is ok
			Set<String> dbStatus = new HashSet<String>();
			dbStatus.add("Available");
			realizationStatuses.getDbInstance2Status().put("unmanaged", dbStatus);

			return realizationStatuses;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Generates a per-hour sum of the different event severity types 
	 * @param owner
	 * @param namespace
	 * @return
	 */
	public EventTimeSeries generateEventTimeSeries(String owner, String namespace) {
		try {
			List<BigDataStackApplication> apps = appClient.getApplications(owner);


			List<BigDataStackEvent> allEvents = new ArrayList<BigDataStackEvent>();

			for (BigDataStackApplication app : apps) {
				if (app.getNamespace().equalsIgnoreCase(namespace)) {
					List<BigDataStackEvent> events = eventClient.getEvents(app.getAppID(), owner);
					allEvents.addAll(events);
				}
			}

			Collections.sort(allEvents);
			System.err.println("generateEventTimeSeries: Matched "+allEvents.size()+" events");

			List<Integer> infoCountPerHour = new ArrayList<Integer>();
			List<Integer> warnCountPerHour = new ArrayList<Integer>();
			List<Integer> errCountPerHour = new ArrayList<Integer>();
			List<Integer> alertCountPerHour = new ArrayList<Integer>();

			long startOfFirstHour = -1;
			long startOfHour = -1;
			int countInfoCurrentHour = 0;
			int countWarnCurrentHour = 0;
			int countErrCurrentHour = 0;
			int countAlertCurrentHour = 0;

			long oneHour = 1000*60*60;

			for (BigDataStackEvent event : allEvents) {
				if (startOfHour==-1) {
					startOfHour=event.getEventTime();
					startOfFirstHour = event.getEventTime();
				}

				while (event.getEventTime()>(startOfHour+oneHour)) {
					// new hour
					infoCountPerHour.add(countInfoCurrentHour);
					warnCountPerHour.add(countWarnCurrentHour);
					errCountPerHour.add(countErrCurrentHour);
					alertCountPerHour.add(countAlertCurrentHour);

					countInfoCurrentHour = 0;
					countWarnCurrentHour = 0;
					countErrCurrentHour = 0;
					countAlertCurrentHour = 0;

					startOfHour = (startOfHour+oneHour);
				}

				if (event.getSeverity() == BigDataStackEventSeverity.Info) countInfoCurrentHour++;
				if (event.getSeverity() == BigDataStackEventSeverity.Warning) countWarnCurrentHour++;
				if (event.getSeverity() == BigDataStackEventSeverity.Error) countErrCurrentHour++;
				if (event.getSeverity() == BigDataStackEventSeverity.Alert) countAlertCurrentHour++;

			}
			infoCountPerHour.add(countInfoCurrentHour);
			warnCountPerHour.add(countWarnCurrentHour);
			errCountPerHour.add(countErrCurrentHour);
			alertCountPerHour.add(countAlertCurrentHour);

			return new EventTimeSeries(infoCountPerHour, warnCountPerHour, errCountPerHour, alertCountPerHour, startOfFirstHour, startOfHour);




		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Generates a time series vector that contains the average values of all datapoints for a single pod during each hour. If multiple pods are 
	 * matched then pod averages are summed per hour. This can only retrieve data from up to a week in the past. 
	 * @param owner
	 * @param namespace
	 * @param metric
	 * @return
	 */
	public PerHourTimeSeries generatePerHourTimeSeries(String owner, String namespace, String metric) {
		return prometheusDataClient.perHourAvg(owner, namespace, null, null, null, metric, "6h");
	}

	/**
	 * Returns the number of sequences, deploymentconfigs, jobs, pods and services currently running
	 * @param owner
	 * @param namespace
	 * @return
	 */
	public ExecutingStatus generateExecutingStatus(String owner, String namespace) {
		try {
			List<BigDataStackApplication> apps = appClient.getApplications(owner);


			// Check Sequences
			int activeSequences = 0;
			for (BigDataStackApplication app : apps) {
				if (app.getNamespace().equalsIgnoreCase(namespace)) {

					List<BigDataStackOperationSequence> sequences = sequenceInstanceClient.getOperationSequences(owner, app.getAppID(), null);
					for (BigDataStackOperationSequence sequence : sequences) {
						if (sequence.isInProgress()) activeSequences++;
					}	
				}
			}

			// Check Deployment Configs, Jobs, Pods and Services
			int deploymentsActive = 0;
			int jobsActive = 0;
			int podsActive = 0;
			int servicesActive = 0;
			List<BigDataStackObjectDefinition> objectsForNamespace = objectInstanceClient.getObjectList(owner, namespace, null, null);
			for (BigDataStackObjectDefinition object : objectsForNamespace) {

				System.err.println(object.getObjectID()+" "+object.getInstance()+" "+object.getType().name()+" "+Arrays.toString(object.getStatus().toArray()));

				if (object.getStatus().contains("Available") || object.getStatus().contains("Running") || object.getStatus().contains("In Progress")) {
					if (object.getType() == BigDataStackObjectType.DeploymentConfig) deploymentsActive++;
					if (object.getType() == BigDataStackObjectType.Job) jobsActive++;
					//if (object.getType() == BigDataStackObjectType.Pod) podsActive++;

					if (object.getType() == BigDataStackObjectType.DeploymentConfig || object.getType() == BigDataStackObjectType.Job) {
						List<BigDataStackPodStatus> podStatuses = podStatusClient.getPodStatuses(null, object.getOwner(), object.getObjectID(), null, -1);
						for (BigDataStackPodStatus podStatus : podStatuses) {
							if (podStatus.getStatus().equalsIgnoreCase("Running")) podsActive++;

						}
					}

				}

				if (object.getType() == BigDataStackObjectType.Service && !object.getStatus().contains("Deleted") && !object.getStatus().contains("Killed")) servicesActive++;

			}
			ExecutingStatus exeStatus = new ExecutingStatus(activeSequences, deploymentsActive, jobsActive, podsActive, servicesActive);
			return exeStatus;


		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}


	}

	/**
	 * Returns a list of routes for each app in a namespace for an owner
	 * @param owner
	 * @param namespace
	 * @return
	 */
	public RouteList generateRouteList(String owner, String namespace) {

		Map<String,Map<String,String>> app2URLs = new HashMap<String,Map<String,String>>();

		try {
			List<BigDataStackApplication> apps = appClient.getApplications(owner);

			for (BigDataStackApplication app : apps) {

				Map<String,String> urls2Descs = new HashMap<String,String>();
				List<BigDataStackObjectDefinition> objectsForNamespace = objectInstanceClient.getObjectList(owner, namespace, app.getAppID(), BigDataStackObjectType.Route);
				for (BigDataStackObjectDefinition route : objectsForNamespace) {

					JsonNode routeAsJson = yamlMapper.readTree(route.getYamlSource());
					if (routeAsJson.has("spec")) {
						JsonNode spec = routeAsJson.get("spec");
						if (spec.has("host")) {
							if (!spec.has("to")) urls2Descs.put(spec.get("host").asText(), "External http endpoint, unknown target");
							else {
								JsonNode to = spec.get("to");
								urls2Descs.put(spec.get("host").asText(), "External http endpoint, directing traffic to "+to.get("name")+" (type="+to.get("kind")+")");
							}

						}
					}

				}
				app2URLs.put(app.getAppID()+": "+app.getName(), urls2Descs);


			}
			RouteList rl = new RouteList(app2URLs);

			return rl;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	public RealizationReport generateStatusReport(String owner, String namespace) {

		try {
			// Stage 1: Get Realization engine status
			RealizationStatus realizationStatuses = generateRealizationStatus(owner, namespace);

			// Stage 2: Get Event Time Series Data
			EventTimeSeries eventTimeSeries = generateEventTimeSeries(owner, namespace);

			// Stage 3: Get Pod Costs
			PerHourTimeSeries costTimeSeries = generatePerHourTimeSeries(owner, namespace, "costPerHour");

			// Stage 4: Get Number of Objects running
			ExecutingStatus exeStatus = generateExecutingStatus(owner, namespace);

			// Stage 5: Get Routes
			RouteList routeList = generateRouteList(owner, namespace);

			RealizationReport report = new RealizationReport(realizationStatuses,eventTimeSeries,costTimeSeries,exeStatus,routeList);

			return report;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Gets the status of a specified app
	 * @param owner
	 * @param namespace
	 * @param appID
	 * @return
	 */
	public List<BigDataStackAppState> getApplicationStates(String owner, String namespace, String appID) {
		return ApplicationStateUtil.getActiveStates(objectInstanceClient, sequenceInstanceClient, appStateClient, owner, namespace, appID);
	}



	public void printTimings() {
		System.out.println("### Timings ###");
		System.out.println("> eventClient: "+(eventClient.timeSpent()/1000)+"s");
		System.out.println("> metricClient: "+(metricClient.timeSpent()/1000)+"s");
		System.out.println("> objectInstanceClient: "+(objectInstanceClient.timeSpent()/1000)+"s");
		System.out.println("> objectTemplateClient: "+(objectTemplateClient.timeSpent()/1000)+"s");
		System.out.println("> sequenceInstanceClient: "+(sequenceInstanceClient.timeSpent()/1000)+"s");
		System.out.println("> sequenceTemplateClient: "+(sequenceTemplateClient.timeSpent()/1000)+"s");
		System.out.println("> podStatusClient: "+(podStatusClient.timeSpent()/1000)+"s");
		System.out.println("> namespaceStateClient: "+(namespaceStateClient.timeSpent()/1000)+"s");
		System.out.println("> credentialsClient: "+(credentialsClient.timeSpent()/1000)+"s");
	}

	
	public List<String> loadPlaybook(String yaml, String owner, String namespace, String parameterOverrides) {
		
		for (String param : parameterOverrides.split(",")) {
			String paramKey = param.split(":")[0];
			String value = param.split(":")[1];
			yaml = yaml.replaceAll("\\$"+paramKey+"\\$", value);
		}
		
		return loadPlaybook(yaml, owner, namespace);
		
	}
	
	public List<String> loadPlaybook(String yaml, String owner, String namespace) {

		List<String> changes = new ArrayList<String>();

		try {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			JsonNode node = mapper.readTree(yaml);

			List<BigDataStackApplicationType> types = new ArrayList<BigDataStackApplicationType>();
			Iterator<JsonNode> typeI = node.get("types").iterator();
			while (typeI.hasNext()) types.add(BigDataStackApplicationType.valueOf(typeI.next().textValue()));

			BigDataStackApplication app = null;

			if (namespace==null) namespace = node.get("namespace").asText();
			if (owner==null) owner = node.get("owner").asText();

			app = new BigDataStackApplication(
					node.get("appID").asText(), 
					node.get("name").asText(), 
					node.get("description").asText(), 
					owner, 
					namespace,
					types);

			changes.add(jsonMapper.writeValueAsString(registerApplication(new YAMLMapper().writeValueAsString(app))));

			if (node.has("metrics")) {
				JsonNode metrics = node.get("metrics");
				Iterator<JsonNode> metricI = metrics.iterator();
				while (metricI.hasNext()) {
					String jsonAsYaml = new YAMLMapper().writeValueAsString(metricI.next());
					jsonAsYaml = replaceDefaultParameters(jsonAsYaml, app.getAppID(), owner, namespace, gdtConfig.getOpenshift().getHostExtension(), gdtConfig.getOpenshift().getImageRepositoryHost());
					changes.add(jsonMapper.writeValueAsString(registerMetric(jsonAsYaml, owner)));
				}
			}

			if (node.has("objects")) {
				JsonNode objects = node.get("objects");
				Iterator<JsonNode> objectsI = objects.iterator();
				while (objectsI.hasNext()) {
					String jsonAsYaml = new YAMLMapper().writeValueAsString(objectsI.next());
					jsonAsYaml = replaceDefaultParameters(jsonAsYaml, app.getAppID(), owner, namespace, gdtConfig.getOpenshift().getHostExtension(), gdtConfig.getOpenshift().getImageRepositoryHost());
					changes.add(jsonMapper.writeValueAsString(registerObject(jsonAsYaml, app)));
				}
			}


			if (node.has("sequences")) {
				JsonNode sequences = node.get("sequences");
				Iterator<JsonNode> sequencesI = sequences.iterator();
				while (sequencesI.hasNext()) {
					String jsonAsYaml = new YAMLMapper().writeValueAsString(sequencesI.next());
					jsonAsYaml = replaceDefaultParameters(jsonAsYaml, app.getAppID(), owner, namespace, gdtConfig.getOpenshift().getHostExtension(), gdtConfig.getOpenshift().getImageRepositoryHost());
					changes.add(jsonMapper.writeValueAsString(registerOperationSequence(jsonAsYaml, namespace, owner)));
				}
			}

			if (node.has("triggers")) {
				JsonNode sequences = node.get("triggers");
				Iterator<JsonNode> sequencesI = sequences.iterator();
				while (sequencesI.hasNext()) {
					String jsonAsYaml = new YAMLMapper().writeValueAsString(sequencesI.next());
					jsonAsYaml = replaceDefaultParameters(jsonAsYaml, app.getAppID(), owner, namespace, gdtConfig.getOpenshift().getHostExtension(), gdtConfig.getOpenshift().getImageRepositoryHost());
					changes.add(jsonMapper.writeValueAsString(registerSLO(jsonAsYaml, namespace, owner, app.getAppID())));
				}
			}

			if (node.has("states")) {
				JsonNode states = node.get("states");
				Iterator<JsonNode> statesI = states.iterator();
				while (statesI.hasNext()) {
					String jsonAsYaml = new YAMLMapper().writeValueAsString(statesI.next());
					jsonAsYaml = replaceDefaultParameters(jsonAsYaml, app.getAppID(), owner, namespace, gdtConfig.getOpenshift().getHostExtension(), gdtConfig.getOpenshift().getImageRepositoryHost());
					changes.add(jsonMapper.writeValueAsString(registerApplicationState(jsonAsYaml, app)));
				}
			}




		} catch (Exception e) {
			e.printStackTrace();

		}

		return changes;

	}

	/**
	 * Perform a full shutdown of a user application and delete all underlying object trackers
	 * @param owner
	 * @param namespace
	 * @param appID
	 */
	public boolean deleteApp(String owner, String namespace, String appID) {

		try {
			// Stage 1: Get App
			BigDataStackApplication app = appClient.getApp(appID, owner, namespace);
			if (app==null) return false;


			// Stage 2: Delete objects on the cluster
			List<BigDataStackObjectDefinition> objects = objectInstanceClient.getObjects(null, owner, namespace, appID);
			for (BigDataStackObjectDefinition object : objects) {
				openshiftOperationClient.deleteOperation(object);
			}

			// Stage 3: Delete realization engine objects
			sloClient.delete(owner, namespace, appID, null);
			metricValueClient.delete(owner, namespace, appID, null, null);
			objectInstanceClient.delete(owner, namespace, appID, null, -1);
			sequenceTemplateClient.delete(owner, namespace, appID, null, -1);
			sequenceInstanceClient.delete(owner, namespace, appID, null, -1);
			sequenceTemplateClient.delete(owner, namespace, appID, null, -1);
			podStatusClient.delete(owner, namespace, appID, null, -1);
			appClient.delete(owner, namespace, appID);
			eventClient.delete(owner, namespace, appID, null);

			cleanupSequenceRunners(owner, namespace, appID);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}


	}

	protected static String replaceDefaultParameters(String yaml, String appID, String owner, String namespace, String hostExtension, String imageRepositoryHost) {
		yaml = yaml.replaceAll("\\$appID\\$", appID);
		yaml = yaml.replaceAll("\\$owner\\$", owner);
		yaml = yaml.replaceAll("\\$namespace\\$", namespace);
		yaml = yaml.replaceAll("\\$hostExtension\\$", hostExtension);
		yaml = yaml.replaceAll("\\$imageRepositoryHost\\$", imageRepositoryHost);


		yaml = yaml.replaceAll("\\$appid\\$", appID);

		yaml = yaml.replaceAll("\\$APPID\\$", appID);
		yaml = yaml.replaceAll("\\$OWNER\\$", owner);
		yaml = yaml.replaceAll("\\$NAMESPACE\\$", namespace);

		Random r = new Random();
		long randomLong = r.nextLong();
		yaml = yaml.replaceAll("\\$random\\$", String.valueOf(randomLong));
		yaml = yaml.replaceAll("\\$RANDOM\\$", String.valueOf(randomLong));

		return yaml;
	}


}
