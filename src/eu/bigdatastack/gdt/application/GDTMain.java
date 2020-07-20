package eu.bigdatastack.gdt.application;

import java.io.File;
import java.util.Map;

import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.lxdb.MySQLDB;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusFabric8ioClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.config.DatabaseConf;
import eu.bigdatastack.gdt.structures.config.GDTConfig;
import eu.bigdatastack.gdt.structures.config.OpenshiftConfig;
import eu.bigdatastack.gdt.structures.config.RabbitMQConf;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;
import eu.bigdatastack.gdt.threads.CostExporter;
import eu.bigdatastack.gdt.threads.OpenshiftProjectMonitoringThread;

public class GDTMain {

	public static void main(String[] args) throws Exception {
		
		// Parse Common Environment Variables
		String owner =  System.getenv("owner");
		String namespace =  System.getenv("namespace");
		String appID =  System.getenv("appID");
		String sequenceID =  System.getenv("sequenceID");
		String sequenceInstance =  System.getenv("sequenceInstance");
		String dbtype = System.getenv("dbtype");
		String dbhost = System.getenv("dbhost");
		String dbport = System.getenv("dbport");
		String dbname = System.getenv("dbname");
		String dbusername = System.getenv("dbusername");
		String dbpassword = System.getenv("dbpassword");
		String occlient =  System.getenv("occlient");
		String ochost =  System.getenv("ochost");
		String ocport = System.getenv("ocport");
		String ocusername = System.getenv("ocusername");
		String ocpassword = System.getenv("ocpassword");
		String ochostExtension = System.getenv("ochostextension");
		String ocimagerepositoryhost = System.getenv("ocimagerepositoryhost");
		String rmqhost = System.getenv("rmqhost");
		String rmqport = System.getenv("rmqport");
		String rmqusername = System.getenv("rmqusername");
		String rmqpassword = System.getenv("rmqpassword");
		
		
		if (args.length==0) {
			System.out.println("### GDT Help ###");
			System.out.println("Available Commands: ");
			System.out.println("  register: Adds a new app, object, sequence or metric");
			System.out.println("  list: lists existing objects");
			System.out.println("  monitor: start or stop monitoring");
			System.out.println("  sequence: launch new operation sequences");
			System.out.println("  describe: describe objects");
			System.out.println("  reset: clear the state db");
			return;
		}
		
		// Launch as a monitoring instance
		if (args[0].equalsIgnoreCase("namespaceMonitor")) {
			
			
			JDBCDB database = new MySQLDB(dbhost, Integer.parseInt(dbport), dbname, dbusername, dbpassword);
			
			OpenshiftStatusClient openshiftStatus = null;
			//if (occlient.equalsIgnoreCase("openshift3")) openshiftStatus = new OpenshiftStatusClientv3(ochost, Integer.parseInt(ocport), ocusername, ocpassword);
			if (occlient.equalsIgnoreCase("fabric8io")) openshiftStatus = new OpenshiftStatusFabric8ioClient(ochost, Integer.parseInt(ocport), ocusername, ocpassword);
			if (openshiftStatus==null) {
				System.err.println("Openshift client '"+occlient+"' is not supported");
				return;
			}
			
			openshiftStatus.connectToOpenshift();
			
			RabbitMQClient rabbitMQClient = new RabbitMQClient(rmqhost, Integer.parseInt(rmqport), rmqusername, rmqpassword);
			
			OpenshiftProjectMonitoringThread monitorThread = new OpenshiftProjectMonitoringThread(openshiftStatus, rabbitMQClient, database, owner, namespace);
			monitorThread.run();
			openshiftStatus.close();
			
		// Launch as a processor for an Operation Sequence
		} else if (args[0].equalsIgnoreCase("operationSequence")) {
			
			DatabaseConf databaseConf = new DatabaseConf(dbtype, dbhost, Integer.parseInt(dbport), dbname, dbusername, dbpassword);
			OpenshiftConfig openshiftConf = new OpenshiftConfig(occlient, ochost, Integer.parseInt(ocport), ocusername, ocpassword, ochostExtension,ocimagerepositoryhost);
			RabbitMQConf rabbitMQConf = new RabbitMQConf(rmqhost, Integer.parseInt(rmqport), rmqusername, rmqpassword);
			GDTConfig gdtConf = new GDTConfig(databaseConf,rabbitMQConf,openshiftConf);
			
			GDTManager manager = new GDTManager(gdtConf);
			
			// This should only ever return 1 sequence because we are connecting to the template client
			BigDataStackOperationSequence sequence = manager.getSequenceInstanceClient().getOperationSequence(appID, sequenceID, Integer.parseInt(sequenceInstance), null);
			if (sequence==null) {
				manager.eventUtil.registerEvent(
						appID,
						owner,
						namespace,
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Aborted executing operation sequence based on template : '"+sequenceID+"'",
						"Was unable to retrieve the operation sequence instance for '"+sequenceID+"'",
						sequenceID
						);
				return;
			}
			
			Map<String,String> parameters = System.getenv();
			
			if (manager.executeSequenceFromTemplateSync(sequence, parameters)) {
				manager.eventUtil.registerEvent(
						appID,
						owner,
						namespace,
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Info,
						"Completed operation sequence pod on the cluster based on template : '"+sequenceID+"'",
						"Finished processing operation sequence '"+sequenceID+"' with index "+sequence.getIndex(),
						sequenceID
						);
				
			} else {
				manager.eventUtil.registerEvent(
						appID,
						owner,
						namespace,
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Failed operation sequence pod based on template : '"+sequenceID+"'",
						"Attempted to run operation sequence '"+sequenceID+"' with index "+sequence.getIndex()+", but one or more operations failed",
						sequenceID
						);
			}
			
			manager.printTimings();
			
			manager.shutdown();
			
			return;
			
			
		} else if (args[0].equalsIgnoreCase("costEstimator")) {
			
			
			JDBCDB database = new MySQLDB(dbhost, Integer.parseInt(dbport), dbname, dbusername, dbpassword);
			
			OpenshiftStatusClient openshiftStatus = null;
			//if (occlient.equalsIgnoreCase("openshift3")) openshiftStatus = new OpenshiftStatusClientv3(ochost, Integer.parseInt(ocport), ocusername, ocpassword);
			if (occlient.equalsIgnoreCase("fabric8io")) openshiftStatus = new OpenshiftStatusFabric8ioClient(ochost, Integer.parseInt(ocport), ocusername, ocpassword);
			if (openshiftStatus==null) {
				System.err.println("Openshift client '"+occlient+"' is not supported");
				return;
			}
			openshiftStatus.connectToOpenshift();
			
			RabbitMQClient rabbitMQClient = new RabbitMQClient(rmqhost, Integer.parseInt(rmqport), rmqusername, rmqpassword);
			
			CostExporter monitorThread = new CostExporter(openshiftStatus, rabbitMQClient, database, owner, namespace);
			monitorThread.run();
			openshiftStatus.close();
			
			
		} else if (args[0].equalsIgnoreCase("api")) {
			
			DatabaseConf databaseConf = new DatabaseConf(dbtype, dbhost, Integer.parseInt(dbport), dbname, dbusername, dbpassword);
			OpenshiftConfig openshiftConf = new OpenshiftConfig(occlient, ochost, Integer.parseInt(ocport), ocusername, ocpassword, ochostExtension,ocimagerepositoryhost);
			RabbitMQConf rabbitMQConf = new RabbitMQConf(rmqhost, Integer.parseInt(rmqport), rmqusername, rmqpassword);
			GDTConfig gdtConf = new GDTConfig(databaseConf,rabbitMQConf,openshiftConf);
			
			GDTManager manager = new GDTManager(gdtConf);
			
			try {
				String[] commandArgs = {"server", "api.json"};
				new GDTAPI(manager).run(commandArgs); // Create a new online api and run it
			} catch (Exception e) {e.printStackTrace();}
			
			
		// Launch as a processor for an Operation Sequence
		} else {
			
			System.out.print("Reading Config...");
			// config
			GDTConfig config;
			
			
			String gdtConfig = System.getenv("GDTConfig");
			if (gdtConfig!=null) config = new GDTConfig(new File(gdtConfig));
			else config = new GDTConfig(new File("gdt.config.json"));

			System.out.println("OK");

			// Manager
			System.out.print("Creating Manager and Checking State...");
			GDTManager manager = new GDTManager(config);
			System.out.println("OK");
			
			GDTCLI cli = new GDTCLI(manager);
			
			cli.processCommand(args);
		}
		
	}
	
}
