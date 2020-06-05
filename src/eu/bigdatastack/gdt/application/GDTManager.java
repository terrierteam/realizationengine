package eu.bigdatastack.gdt.application;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;

import eu.bigdatastack.gdt.lxdb.BigDataStackApplicationIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackCredentialsIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackEventIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackMetricIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackMetricValueIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackNamespaceStateIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackOperationSequenceIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackPodStatusIO;
import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.operations.Apply;
import eu.bigdatastack.gdt.operations.BigDataStackOperation;
import eu.bigdatastack.gdt.operations.Instantiate;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.config.DatabaseConf;
import eu.bigdatastack.gdt.structures.config.GDTConfig;
import eu.bigdatastack.gdt.structures.config.OpenshiftConfig;
import eu.bigdatastack.gdt.structures.config.RabbitMQConf;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplicationType;
import eu.bigdatastack.gdt.structures.data.BigDataStackCredentials;
import eu.bigdatastack.gdt.structures.data.BigDataStackCredentialsType;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetric;
import eu.bigdatastack.gdt.structures.data.BigDataStackNamespaceState;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequenceMode;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.EventUtil;
import eu.bigdatastack.gdt.util.GDTFileUtil;

/**
 * This is the main manager class for the Global Decision Tracker
 * @author EbonBlade
 *
 */
public class GDTManager implements Manager {

	GDTConfig gdtConfig; 

	ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

	LXDB database;
	OpenshiftOperationClient openshiftOperationClient;
	OpenshiftStatusClient openshiftStatusClient;
	RabbitMQClient mailboxClient;
	PrometheusDataClient prometheusDataClient;

	EventUtil eventUtil;

	BigDataStackApplicationIO appClient;
	BigDataStackEventIO eventClient;
	BigDataStackMetricIO metricClient;
	BigDataStackObjectIO objectInstanceClient;
	BigDataStackObjectIO objectTemplateClient;
	BigDataStackOperationSequenceIO sequenceInstanceClient;
	BigDataStackOperationSequenceIO sequenceTemplateClient;
	BigDataStackPodStatusIO podStatusClient;
	BigDataStackNamespaceStateIO namespaceStateClient;
	BigDataStackCredentialsIO credentialsClient;
	BigDataStackMetricValueIO metricValueClient;

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
		if (dbconf.getPassword()!=null && dbconf.getPassword().length()>0) database = new LXDB(dbconf.getHost(), dbconf.getPort(), dbconf.getName(), dbconf.getUsername(), dbconf.getPassword());
		else database = new LXDB(dbconf.getHost(), dbconf.getPort(), dbconf.getName(), dbconf.getUsername());

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


		// Add database credentials
		databaseCredential = new BigDataStackCredentials("GDT", gdtConfig.getDatabase().getUsername(), gdtConfig.getDatabase().getPassword(), BigDataStackCredentialsType.database);
		//if (!credentialsClient.addCredential(databaseCredential)) credentialsClient.updatePassweord("GDT", BigDataStackCredentialsType.database, gdtConfig.getDatabase().getUsername(), gdtConfig.getDatabase().getPassword());

		// Initalize Openshift Clients
		OpenshiftConfig openshiftConf = gdtConfig.getOpenshift();
		openshiftCredential = new BigDataStackCredentials("GDT", gdtConfig.getOpenshift().getUsername(), gdtConfig.getOpenshift().getPassword(), BigDataStackCredentialsType.openshift);
		//if (!credentialsClient.addCredential(openshiftCredential)) credentialsClient.updatePassweord("GDT", BigDataStackCredentialsType.openshift, gdtConfig.getOpenshift().getUsername(), gdtConfig.getOpenshift().getPassword());
		openshiftOperationClient = new OpenshiftOperationClient(openshiftConf.getHost(), openshiftConf.getPort(), openshiftConf.getUsername(), openshiftConf.getPassword());
		openshiftOperationClient.connectToOpenshift();
		openshiftStatusClient = new OpenshiftStatusClient(openshiftOperationClient.getClient());

		// Initalize RabbitMQ Client
		RabbitMQConf rabbitMQConf = gdtConfig.getRabbitmq();
		mailboxClient = new RabbitMQClient(rabbitMQConf.getHost(), rabbitMQConf.getPort(), rabbitMQConf.getUsername(), rabbitMQConf.getPassword());
		rabbitMQCredential = new BigDataStackCredentials("GDT", gdtConfig.getRabbitmq().getUsername(), gdtConfig.getRabbitmq().getPassword(), BigDataStackCredentialsType.rabbitmq);
		//if (!credentialsClient.addCredential(rabbitMQCredential)) credentialsClient.updatePassweord("GDT", BigDataStackCredentialsType.rabbitmq, gdtConfig.getRabbitmq().getUsername(), gdtConfig.getRabbitmq().getPassword());


		// Initalize Prometheus Data Client
		prometheusDataClient = new PrometheusDataClient();


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
							app.getAppID()
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
					app.getAppID()
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
		return registerObject(yaml, null, null);
	}
	
	/**
	 * Registers a new BigDataStack Object Definition with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackObjectDefinition registerObject(String yaml, String namespace, String owner) {
		try {
			BigDataStackObjectDefinition object = GDTFileUtil.readObjectFromString(yaml);
			if (namespace!=null) object.setNamespace(namespace);
			if (owner!=null) object.setOwner(owner);
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
							object.getObjectID()
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
					object.getObjectID()
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
							object.getName()
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
					object.getName()
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
						object.getObjectID()
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
						object.getObjectID()
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
						namespace.getNamespace()
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
						namespace.getNamespace()
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
							sequence.getSequenceID()
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
					sequence.getSequenceID()
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
	public boolean startMonitoringNamespace(BigDataStackNamespaceState namespace, String owner) {

		try {

			// get or register the default app
			BigDataStackApplication app = getAppClient().getApp("gdtdefaultapp", owner, namespace.getNamespace());
			if (app == null) app = registerApplication(new File("resources/gdt/gdtdefault.app.yaml"), namespace.getNamespace(), owner);
			if (app == null) {

				eventUtil.registerEvent(
						"gdtdefaultapp",
						owner,
						namespace.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Failed to launch namespace monitoring for namespace: '"+namespace.getNamespace()+"'",
						"Tried to get or register the GDT default application, but it was rejected by the registry",
						namespace.getNamespace()
						);
				return false;
			}




			// get or register the prometheus config object
			BigDataStackObjectDefinition prometheusCM = getObjectTemplateClient().getObject("gdtdefaultapp-prometheusconfig", owner);
			if (prometheusCM == null) prometheusCM = registerObject(new File("resources/gdt/prometheus.config.yaml"), namespace.getNamespace(), owner);
			if (prometheusCM == null) {

				eventUtil.registerEvent(
						"gdtdefaultapp",
						owner,
						namespace.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Failed to register namespace prometheus configmap instance for namespace: '"+namespace.getNamespace()+"'",
						"Tried to get or register the GDT Prometheus configmap, but it was rejected by the registry",
						"gdtdefaultapp-prometheusconfig"
						);
				return false;
			}

			// get or register the prometheus service account
			BigDataStackObjectDefinition prometheusSA = getObjectTemplateClient().getObject("gdtdefaultapp-prometheussa", owner);
			if (prometheusSA == null) prometheusSA = registerObject(new File("resources/gdt/prometheus.sa.yaml"), namespace.getNamespace(), owner);
			if (prometheusSA == null) {

				eventUtil.registerEvent(
						"gdtdefaultapp",
						owner,
						namespace.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Failed to register namespace prometheus service account for namespace: '"+namespace.getNamespace()+"'",
						"Tried to get or register the GDT prometheus service account object, but it was rejected by the registry",
						"gdtdefaultapp-prometheussa"
						);
				return false;
			}

			// get or register the prometheus service account role
			BigDataStackObjectDefinition prometheusR = getObjectTemplateClient().getObject("gdtdefaultapp-prometheusrole", owner);
			if (prometheusR == null) prometheusR = registerObject(new File("resources/gdt/prometheus.role.yaml"), namespace.getNamespace(), owner);
			if (prometheusR == null) {

				eventUtil.registerEvent(
						"gdtdefaultapp",
						owner,
						namespace.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Failed to register namespace prometheus role for namespace: '"+namespace.getNamespace()+"'",
						"Tried to get or register the GDT prometheus role object, but it was rejected by the registry",
						"gdtdefaultapp-prometheusrole"
						);
				return false;
			}

			// get or register the prometheus cluster role binding for service account
			BigDataStackObjectDefinition prometheusCRB = getObjectTemplateClient().getObject("gdtdefaultapp-prometheusrb", owner);
			if (prometheusCRB == null) prometheusCRB = registerObject(new File("resources/gdt/prometheus.rb.yaml"), namespace.getNamespace(), owner);
			if (prometheusCRB == null) {

				eventUtil.registerEvent(
						"gdtdefaultapp",
						owner,
						namespace.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Failed to register namespace prometheus service account role binding for namespace: '"+namespace.getNamespace()+"'",
						"Tried to get or register the GDT prometheus service account role binding object, but it was rejected by the registry",
						"gdtdefaultapp-prometheusrb"
						);
				return false;
			}

			// get or register the prometheus object
			BigDataStackObjectDefinition prometheusDC = getObjectTemplateClient().getObject("gdtdefaultapp-prometheus", owner);
			if (prometheusDC == null) prometheusDC = registerObject(new File("resources/gdt/prometheus.dc.yaml"), namespace.getNamespace(), owner);
			if (prometheusDC == null) {

				eventUtil.registerEvent(
						"gdtdefaultapp",
						owner,
						namespace.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Failed to register namespace prometheus instance for namespace: '"+namespace.getNamespace()+"'",
						"Tried to get or register the GDT Prometheus object, but it was rejected by the registry",
						"gdtdefaultapp-prometheus"
						);
				return false;
			}

			// get or register the prometheus service object
			BigDataStackObjectDefinition prometheusSRV = getObjectTemplateClient().getObject("gdtdefaultapp-prometheussrv", owner);
			if (prometheusSRV == null) prometheusSRV = registerObject(new File("resources/gdt/prometheus.srv.yaml"), namespace.getNamespace(), owner);
			if (prometheusSRV == null) {

				eventUtil.registerEvent(
						"gdtdefaultapp",
						owner,
						namespace.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Failed to register namespace prometheus service instance for namespace: '"+namespace.getNamespace()+"'",
						"Tried to get or register the GDT Prometheus service object, but it was rejected by the registry",
						"gdtdefaultapp-prometheussrv"
						);
				return false;
			}

			// get or register the prometheus route object
			BigDataStackObjectDefinition prometheusRoute = getObjectTemplateClient().getObject("gdtdefaultapp-prometheusroute", owner);
			if (prometheusRoute == null) prometheusRoute = registerObject(new File("resources/gdt/prometheus.route.yaml"), namespace.getNamespace(), owner);
			if (prometheusRoute == null) {

				eventUtil.registerEvent(
						"gdtdefaultapp",
						owner,
						namespace.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Failed to register namespace prometheus route instance for namespace: '"+namespace.getNamespace()+"'",
						"Tried to get or register the GDT Prometheus route object, but it was rejected by the registry",
						"gdtdefaultapp-prometheusroute"
						);
				return false;
			}

			// get or create the operation sequence
			BigDataStackOperationSequence prometheusSequenceTemplate = getSequenceTemplateClient().getOperationSequence("gdtdefaultapp", "seq-prometheusdeploy", 0);
			if (prometheusSequenceTemplate==null) prometheusSequenceTemplate = registerOperationSequence(new File("resources/gdt/prometheus.seq.yaml"), namespace.getNamespace(), owner);
			if (prometheusSequenceTemplate==null) {

				eventUtil.registerEvent(
						"gdtdefaultapp",
						owner,
						namespace.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Failed to launch namespace prometheus instance for namespace: '"+namespace.getNamespace()+"'",
						"Tried to get or register the GDT Prometheus object operation sequence template, but it was rejected by the registry",
						"seq-prometheusdeploy"
						);

				return false;
			}

			boolean prometheusOk = executeSequenceFromTemplateSync(prometheusSequenceTemplate);

			if (!prometheusOk) return false;

			// get or register the monitor object
			BigDataStackObjectDefinition monitorDC = getObjectTemplateClient().getObject("gdtdefaultapp-gdtmonitor", owner);
			if (monitorDC == null) monitorDC = registerObject(new File("resources/gdt/gdtmain.dc.yaml"), namespace.getNamespace(), owner);
			if (monitorDC == null) {

				eventUtil.registerEvent(
						"gdtdefaultapp",
						owner,
						namespace.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Failed to launch namespace monitoring for namespace: '"+namespace.getNamespace()+"'",
						"Tried to get or register the GDT Monitoring object, but it was rejected by the registry",
						"gdtdefaultapp-gdtmonitor"
						);
				return false;
			}

			// get or create the operation sequence
			BigDataStackOperationSequence existingSequenceTemplate = getSequenceTemplateClient().getOperationSequence("gdtdefaultapp", "seq-gdtmonitor", 0);
			if (existingSequenceTemplate==null) existingSequenceTemplate = registerOperationSequence(new File("resources/gdt/gdtmonitor.seq.yaml"), namespace.getNamespace(), owner);
			if (existingSequenceTemplate==null) {

				eventUtil.registerEvent(
						"gdtdefaultapp",
						owner,
						namespace.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Failed to launch namespace monitoring for namespace: '"+namespace.getNamespace()+"'",
						"Tried to get or register the GDT Monitoring operation sequence template, but it was rejected by the registry",
						"seq-gdtmonitor"
						);

				return false;
			}

			// Launch the Operation Sequence
			Map<String,String> parameters = new HashMap<String,String>();
			// database info
			parameters.put("dbhost", gdtConfig.getDatabase().getHost());
			parameters.put("dbport", String.valueOf(gdtConfig.getDatabase().getPort()));
			parameters.put("dbname", gdtConfig.getDatabase().getName());
			// database credentials
			parameters.put("dbusername", databaseCredential.getUsername());
			parameters.put("dbpassword", databaseCredential.getPassword());
			// openshift info
			parameters.put("ochost", gdtConfig.getOpenshift().getHost());
			parameters.put("ocport", String.valueOf(gdtConfig.getOpenshift().getPort()));
			// openshift credentials
			parameters.put("ocusername", openshiftCredential.getUsername());
			parameters.put("ocpassword", openshiftCredential.getPassword());
			// rabbitmq info
			parameters.put("rmqhost", gdtConfig.getRabbitmq().getHost());
			parameters.put("rmqport", String.valueOf(gdtConfig.getRabbitmq().getPort()));
			// rabbitmq credentials
			parameters.put("rmqusername", rabbitMQCredential.getUsername());
			parameters.put("rmqpassword", rabbitMQCredential.getPassword());

			boolean monitoringOk = executeSequenceFromTemplateSync(existingSequenceTemplate, parameters);

			if (!monitoringOk) return false;


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
	public boolean stopMonitoringNamespace(BigDataStackNamespaceState namespace, String owner) {

		try {
			List<BigDataStackObjectDefinition> objects = objectInstanceClient.getObjects("gdtmonitor", owner, namespace.getNamespace(), "gdtdefaultapp");
			if (objects.size()==0) return false;

			int succeeded = 0;
			int failures = 0;
			for (BigDataStackObjectDefinition object : objects) {
				if (!openshiftOperationClient.deleteOperation(object)) {
					eventUtil.registerEvent(
							"gdtdefaultapp",
							owner,
							namespace.getNamespace(),
							BigDataStackEventType.Openshift,
							BigDataStackEventSeverity.Error,
							"Failed to halt namespace monitoring for namespace: '"+namespace.getNamespace()+"' via "+object.getObjectID()+"'("+object.getInstance()+")'",
							"Tried to delete the deployment config for the monitoring process as listed in the registry: "+object.getObjectID()+"'("+object.getInstance()+", but openshift rejected it",
							namespace.getNamespace()
							);
					failures++;
				} else {
					eventUtil.registerEvent(
							"gdtdefaultapp",
							owner,
							namespace.getNamespace(),
							BigDataStackEventType.Openshift,
							BigDataStackEventSeverity.Info,
							"Halted namespace monitoring for namespace: '"+namespace.getNamespace()+"' via '"+object.getObjectID()+"("+object.getInstance()+")'",
							"Deleted the deployment config for the monitoring process as listed in the registry: '"+object.getObjectID()+"("+object.getInstance()+")'",
							namespace.getNamespace()
							);
					succeeded++;
				}
			}

			if (succeeded>0) {
				eventUtil.registerEvent(
						"gdtdefaultapp",
						owner,
						namespace.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Info,
						"Halted namespace monitoring for namespace: '"+namespace.getNamespace()+"'",
						"Monitoring of the namespace '"+namespace.getNamespace()+"' has stopped.",
						namespace.getNamespace()
						);
			} else if (failures>0) {
				eventUtil.registerEvent(
						"gdtdefaultapp",
						owner,
						namespace.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Warning,
						"Failed to halt namespace monitoring for namespace: '"+namespace.getNamespace()+"'",
						"Tried to delete the deployed monitoring instance(s) for the namespace '"+namespace.getNamespace()+"', but no request succeeded, likely this means that monitoring is now not running.",
						namespace.getNamespace()
						);
				return false;
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
	 * This is the asynchronous version of the call, i.e. it will generate a pod to orchestrate the sequence.
	 * @param sequenceTemplate
	 * @param parameters
	 * @return
	 */
	public boolean executeSequenceFromTemplate(BigDataStackOperationSequence sequenceTemplate, Map<String,String> parameters) {

		if (parameters==null) parameters = new HashMap<String,String>();
		parameters.put("appID", sequenceTemplate.getAppID());
		parameters.put("namespace", sequenceTemplate.getNamespace());
		parameters.put("owner", sequenceTemplate.getOwner());
		parameters.put("sequenceID", sequenceTemplate.getSequenceID());
		parameters.put("dbusername", databaseCredential.getUsername());
		parameters.put("dbpassword", databaseCredential.getPassword());
		parameters.put("dbhost", gdtConfig.getDatabase().getHost());
		parameters.put("dbport", String.valueOf(gdtConfig.getDatabase().getPort()));
		parameters.put("dbname", gdtConfig.getDatabase().getName());
		parameters.put("ochost", gdtConfig.getOpenshift().getHost());
		parameters.put("ocport", String.valueOf(gdtConfig.getOpenshift().getPort()));
		parameters.put("ocusername", openshiftCredential.getUsername());
		parameters.put("ocpassword", openshiftCredential.getPassword());
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
						return false;
					} else continue;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
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
						newSequenceInstance.getSequenceID()
						);

				BigDataStackObjectDefinition operationsequenceDef = GDTFileUtil.readObjectFromString(GDTFileUtil.file2String(new File("resources/gdt/operationsequence.pod.yaml"), "UTF-8"));

				String yaml = operationsequenceDef.getYamlSource();

				for (String paramKey : parameters.keySet()) {
					yaml = yaml.replaceAll("\\$"+paramKey+"\\$", parameters.get(paramKey));
				}
				operationsequenceDef.setYamlSource(yaml);

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
						newSequenceInstance.getSequenceID()
						);
				return false;
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
	 * This is the asynchronous version of the call, i.e. it will generate a pod to orchestrate the sequence.
	 * @param sequenceTemplate
	 * @return
	 */
	public boolean executeSequenceFromTemplate(BigDataStackOperationSequence sequenceTemplate) {
		Map<String,String> params = new HashMap<String,String>();
		return executeSequenceFromTemplate(sequenceTemplate, params);

	}

	/**
	 * Deletes any sequence template runners that have been completed to avoid cluttering the cluster. In this case,
	 * completed means any pod in Terminating, Completed or Failed status. It uses the operationsequence=True label
	 * to find the pods.
	 */
	public void cleanUpEndedSequenceTemplates(String namespace) {

		IProject project  = openshiftStatusClient.getProject(namespace);

		List<IPod> pods = openshiftStatusClient.getPods(project, false, true, "operationsequence=True");

		for (IPod pod : pods) {
			openshiftOperationClient.getClient().delete(pod);
		}

	}


	public void shutdown() {
		openshiftOperationClient.close();
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


	public void loadPlaybook(String yaml) {
		
		try {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			JsonNode node = mapper.readTree(yaml);
			
			List<BigDataStackApplicationType> types = new ArrayList<BigDataStackApplicationType>();
			Iterator<JsonNode> typeI = node.get("types").iterator();
			while (typeI.hasNext()) types.add(BigDataStackApplicationType.valueOf(typeI.next().textValue()));
			
			BigDataStackApplication app = new BigDataStackApplication(
					node.get("appID").asText(), 
					node.get("name").asText(), 
					node.get("description").asText(), 
					node.get("owner").asText(), 
					node.get("namespace").asText(),
					types);
			
			registerApplication(new YAMLMapper().writeValueAsString(app));
			
			if (node.has("metrics")) {
				JsonNode metrics = node.get("metrics");
				Iterator<JsonNode> metricI = metrics.iterator();
				while (metricI.hasNext()) {
					String jsonAsYaml = new YAMLMapper().writeValueAsString(metricI.next());
					jsonAsYaml = replaceDefaultParameters(jsonAsYaml, app.getAppID(), app.getOwner(), app.getNamespace());
					registerMetric(jsonAsYaml);
				}
			}
			
			if (node.has("objects")) {
				JsonNode objects = node.get("objects");
				Iterator<JsonNode> objectsI = objects.iterator();
				while (objectsI.hasNext()) {
					String jsonAsYaml = new YAMLMapper().writeValueAsString(objectsI.next());
					jsonAsYaml = replaceDefaultParameters(jsonAsYaml, app.getAppID(), app.getOwner(), app.getNamespace());
					registerObject(jsonAsYaml);
				}
			}
			
			
			if (node.has("sequences")) {
				JsonNode sequences = node.get("sequences");
				Iterator<JsonNode> sequencesI = sequences.iterator();
				while (sequencesI.hasNext()) {
					String jsonAsYaml = new YAMLMapper().writeValueAsString(sequencesI.next());
					jsonAsYaml = replaceDefaultParameters(jsonAsYaml, app.getAppID(), app.getOwner(), app.getNamespace());
					registerOperationSequence(jsonAsYaml);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	protected static String replaceDefaultParameters(String yaml, String appID, String owner, String namespace) {
		yaml = yaml.replaceAll("\\$appID\\$", appID);
		yaml = yaml.replaceAll("\\$owner\\$", owner);
		yaml = yaml.replaceAll("\\$namespace\\$", namespace);
		
		yaml = yaml.replaceAll("\\$appid\\$", appID);
		
		yaml = yaml.replaceAll("\\$APPID\\$", appID);
		yaml = yaml.replaceAll("\\$OWNER\\$", owner);
		yaml = yaml.replaceAll("\\$NAMESPACE\\$", namespace);
		
		return yaml;
	}


}
