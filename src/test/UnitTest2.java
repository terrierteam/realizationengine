package test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import eu.bigdatastack.gdt.application.GDTManager;
import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.config.GDTConfig;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackNamespaceState;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;
import eu.bigdatastack.gdt.threads.OpenshiftProjectMonitoringThread;

public class UnitTest2 {

	/*@Test
	public void monitoringLocalDeploy() throws Exception{
		// Config
		GDTConfig config = new GDTConfig(new File("gdt.config.json"));
		TestUtil.clearDatabase(config.getDatabase());

		// Manager
		GDTManager manager = new GDTManager(config);

		BigDataStackNamespaceState namespace = manager.registerNamespace(new File("resources/bigdatastack/unitTest2/unitTest2.namespace,yaml"));

		assertNotNull(namespace);
		
		String dbhost = config.getDatabase().getHost();
		int dbport = config.getDatabase().getPort();
		String dbname = config.getDatabase().getName();
		String dbusername = config.getDatabase().getUsername();
		String dbpassword = config.getDatabase().getPassword();
		
		LXDB database = new LXDB(dbhost, dbport, dbname, dbusername, dbpassword);
		
		String ochost =  config.getOpenshift().getHost();
		int ocport = config.getOpenshift().getPort();
		String ocusername = config.getOpenshift().getUsername();
		String ocpassword = config.getOpenshift().getPassword();
		
		OpenshiftStatusClient openshiftStatus = new OpenshiftStatusClient(ochost, ocport, ocusername, ocpassword);
		openshiftStatus.connectToOpenshift();
		
		String rmqhost = config.getRabbitmq().getHost();
		int rmqport = config.getRabbitmq().getPort();
		String rmqusername = config.getRabbitmq().getUsername();
		String rmqpassword = config.getRabbitmq().getPassword();
		
		RabbitMQClient rabbitMQClient = new RabbitMQClient(rmqhost, rmqport, rmqusername, rmqpassword);
		
		String owner =  "richardm";
		String namespaceName =  namespace.getNamespace();
		
		OpenshiftProjectMonitoringThread monitorThread = new OpenshiftProjectMonitoringThread(openshiftStatus, rabbitMQClient, database, owner, namespaceName);
		Thread thread = new Thread(monitorThread);
		thread.start();
		
		// Create Application
		BigDataStackApplication app = manager.registerApplication(new File("resources/bigdatastack/unitTest2/unitTest2.app.yaml"));
		assertNotNull(app);

		// Create Object Template
		BigDataStackObjectDefinition object = manager.registerObject(new File("resources/bigdatastack/unitTest2/sleep.job.yaml"));
		assertNotNull(object);
		
		// Create Sequence
		BigDataStackOperationSequence seq = manager.registerOperationSequence(new File("resources/bigdatastack/unitTest2/sleep.seq.yaml"));
		assertNotNull(seq);
		
		assertTrue(manager.executeSequenceFromTemplateSync(seq));
		
		Thread.sleep(50000);
		
		monitorThread.kill();
		
		manager.shutdown();
		
	}*/

	@Test
	public void monitoringClusterDeploySync() throws Exception{

		// Config
		GDTConfig config = new GDTConfig(new File("gdt.config.json"));
		TestUtil.clearDatabase(config.getDatabase());

		// Manager
		GDTManager manager = new GDTManager(config);

		// Register Namespace
		BigDataStackNamespaceState namespace = manager.registerNamespace(new File("resources/bigdatastack/unitTest2/unitTest2.namespace,yaml"));
		assertNotNull(namespace);

		// Start Namespace Monitoring
		assertTrue(manager.startMonitoringNamespace(namespace, "richardm"));
		
		// Create Application
		BigDataStackApplication app = manager.registerApplication(new File("resources/bigdatastack/unitTest2/unitTest2.app.yaml"));
		assertNotNull(app);

		// Create Object Template
		BigDataStackObjectDefinition object = manager.registerObject(new File("resources/bigdatastack/unitTest2/sleep.job.yaml"));
		assertNotNull(object);
		
		// Create Sequence
		BigDataStackOperationSequence seq = manager.registerOperationSequence(new File("resources/bigdatastack/unitTest2/sleep.seq.yaml"));
		assertNotNull(seq);
		
		// Run test sequence
		assertTrue(manager.executeSequenceFromTemplateSync(seq));
		
		// Shut down monitoring
		assertTrue(manager.stopMonitoringNamespace(namespace, "richardm"));
		
		manager.printTimings();
		
		manager.shutdown();


	}
	
	
	@Test
	public void monitoringClusterDeployASync() throws Exception{

		// Config
		GDTConfig config = new GDTConfig(new File("gdt.config.json"));
		TestUtil.clearDatabase(config.getDatabase());

		// Manager
		GDTManager manager = new GDTManager(config);

		// Register Namespace
		BigDataStackNamespaceState namespace = manager.registerNamespace(new File("resources/bigdatastack/unitTest2/unitTest2.namespace,yaml"));
		assertNotNull(namespace);

		// Start Namespace Monitoring
		assertTrue(manager.startMonitoringNamespace(namespace, "richardm"));
		
		// Create Application
		BigDataStackApplication app = manager.registerApplication(new File("resources/bigdatastack/unitTest2/unitTest2.app.yaml"));
		assertNotNull(app);

		// Create Object Template
		BigDataStackObjectDefinition object = manager.registerObject(new File("resources/bigdatastack/unitTest2/sleep.job.yaml"));
		assertNotNull(object);
		
		// Create Sequence
		BigDataStackOperationSequence seq = manager.registerOperationSequence(new File("resources/bigdatastack/unitTest2/sleep.seq.yaml"));
		assertNotNull(seq);
		
		// Run test sequence
		assertTrue(manager.executeSequenceFromTemplate(seq));
		
		// Shut down monitoring
		//assertTrue(manager.stopMonitoringNamespace(namespace, "richardm"));
		
		manager.printTimings();
		
		manager.shutdown();


	}

}
