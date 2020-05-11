package eu.bigdatastack.gdt.application;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import eu.bigdatastack.gdt.lxdb.BigDataStackApplicationIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackCredentialsIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackEventIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackMetricIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackNamespaceStateIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackOperationSequenceIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackPodStatusIO;
import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.operations.Apply;
import eu.bigdatastack.gdt.operations.BigDataStackOperation;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.config.DatabaseConf;
import eu.bigdatastack.gdt.structures.config.GDTConfig;
import eu.bigdatastack.gdt.structures.config.OpenshiftConfig;
import eu.bigdatastack.gdt.structures.config.RabbitMQConf;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackCredentials;
import eu.bigdatastack.gdt.structures.data.BigDataStackCredentialsType;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
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
		
		
		// Add database credentials
		BigDataStackCredentials databaseCredential = new BigDataStackCredentials("GDT", gdtConfig.getDatabase().getUsername(), gdtConfig.getDatabase().getPassword(), BigDataStackCredentialsType.database);
		if (!credentialsClient.addCredential(databaseCredential)) credentialsClient.updatePassweord("GDT", BigDataStackCredentialsType.database, gdtConfig.getDatabase().getUsername(), gdtConfig.getDatabase().getPassword());
		
		// Initalize Openshift Clients
		OpenshiftConfig openshiftConf = gdtConfig.getOpenshift();
		BigDataStackCredentials openshiftCredential = new BigDataStackCredentials("GDT", gdtConfig.getOpenshift().getUsername(), gdtConfig.getOpenshift().getPassword(), BigDataStackCredentialsType.openshift);
		if (!credentialsClient.addCredential(openshiftCredential)) credentialsClient.updatePassweord("GDT", BigDataStackCredentialsType.openshift, gdtConfig.getOpenshift().getUsername(), gdtConfig.getOpenshift().getPassword());
		openshiftOperationClient = new OpenshiftOperationClient(openshiftConf.getHost(), openshiftConf.getPort(), openshiftConf.getUsername(), openshiftConf.getPassword());
		openshiftStatusClient = new OpenshiftStatusClient(openshiftOperationClient.getClient());
		openshiftOperationClient.connectToOpenshift();
		
		// Initalize RabbitMQ Client
		RabbitMQConf rabbitMQConf = gdtConfig.getRabbitmq();
		mailboxClient = new RabbitMQClient(rabbitMQConf.getHost(), rabbitMQConf.getPort(), rabbitMQConf.getUsername(), rabbitMQConf.getPassword());
		BigDataStackCredentials rabbitMQCredential = new BigDataStackCredentials("GDT", gdtConfig.getRabbitmq().getUsername(), gdtConfig.getRabbitmq().getPassword(), BigDataStackCredentialsType.rabbitmq);
		if (!credentialsClient.addCredential(rabbitMQCredential)) credentialsClient.updatePassweord("GDT", BigDataStackCredentialsType.rabbitmq, gdtConfig.getRabbitmq().getUsername(), gdtConfig.getRabbitmq().getPassword());
		
		
		// Initalize Prometheus Data Client
		prometheusDataClient = new PrometheusDataClient();
		
		
		// Initalize Utilities
		eventUtil = new EventUtil(database, mailboxClient);
		

	}
	
	/**
	 * Registers a new BigDataStack Application with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackApplication registerApplication(String yaml) {
		try {
			BigDataStackApplication app = yamlMapper.readValue(yaml, BigDataStackApplication.class);
			if (appClient.addApplication(app)) {
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
			} else {
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
	 * Registers a new BigDataStack Object Definition with the database from a yaml format String
	 * @param yaml
	 * @return
	 */
	public BigDataStackObjectDefinition registerObject(String yaml) {
		try {
			BigDataStackObjectDefinition object = GDTFileUtil.readObjectFromString(yaml);
			if (objectTemplateClient.addObject(object)) {
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
			} else {
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
	 * Creates a simple operation sequence template comprised of applying a single object
	 * provided. 
	 * @param object
	 * @return
	 */
	public boolean createOperationSequence(BigDataStackApplication app, BigDataStackObjectDefinition object, String sequenceID) {
		try {
			Apply applyOperation = new Apply(app.getAppID(), app.getOwner(), app.getNamespace(), object.getObjectID());
			List<BigDataStackOperation> operations = new ArrayList<BigDataStackOperation>(1);
			operations.add(applyOperation);
			
			BigDataStackOperationSequence sequence = new BigDataStackOperationSequence(
					app.getAppID(),
					object.getOwner(), 
					app.getNamespace(),
					0, 
					sequenceID, 
					"Generic auto-created sequence for '"+object.getObjectID()+"'",
					"This is an automatically generated sequence in Run mode comprising of Apply for '"+object.getObjectID()+"'", 
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
	public boolean executeSequenceFromTemplateSync(BigDataStackOperationSequence sequenceTemplate) {
		
		OperationSequenceThread thread = new OperationSequenceThread(
				database,
				openshiftOperationClient,
				openshiftStatusClient,
				mailboxClient,
				prometheusDataClient, 
				sequenceTemplate);
		
		thread.run();
		return !thread.hasFailed();
	}
	
	/**
	 * Registers a new namspace with the database (this does not trigger the start of monitoring)
	 * @param namespace
	 * @return
	 */
	public boolean registerNamespace(BigDataStackNamespaceState namespace) {
		try {
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
				return false;
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
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
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
	
	
	
	
}
