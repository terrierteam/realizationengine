package org.terrier.realization.application;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.openshift.OpenshiftStatusFabric8ioClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.state.jdbc.MySQLDB;
import org.terrier.realization.structures.config.DatabaseConf;
import org.terrier.realization.structures.config.GDTConfig;
import org.terrier.realization.structures.config.OpenshiftConfig;
import org.terrier.realization.structures.config.RabbitMQConf;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.structures.data.BigDataStackOperationSequence;
import org.terrier.realization.threads.CostExporter;
import org.terrier.realization.threads.GPUFileExporter;
import org.terrier.realization.threads.GPUFileExporterController;
import org.terrier.realization.threads.OpenshiftProjectMonitoringThread;
import org.terrier.realization.threads.OpenshiftResourceMonitorThread;
import org.terrier.realization.threads.ROLE;

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
			if (occlient.equalsIgnoreCase("fabric8io")) openshiftStatus = new OpenshiftStatusFabric8ioClient(ochost, Integer.parseInt(ocport), ocusername, ocpassword, namespace);
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
			OpenshiftConfig openshiftConf = new OpenshiftConfig(occlient, ochost, Integer.parseInt(ocport), ocusername, ocpassword, ochostExtension,ocimagerepositoryhost,namespace);
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
						sequenceID,
						Integer.parseInt(sequenceInstance)
						);
				return;
			}
			
			Map<String,String> parameters = System.getenv();
			
			if (manager.executeSequenceFromTemplateSync(sequence, parameters)) {
				/*manager.eventUtil.registerEvent(
						appID,
						owner,
						namespace,
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Info,
						"Completed operation sequence pod on the cluster based on template : '"+sequenceID+"'",
						"Finished processing operation sequence '"+sequenceID+"' with index "+sequence.getIndex(),
						sequenceID,
						sequence.getIndex()
						);*/
				
				if (sequence.getOnSuccessDo()!=null) {
					// We have something to start if the previous sequence succeeded
					
					manager.eventUtil.registerEvent(
							appID,
							owner,
							namespace,
							BigDataStackEventType.GlobalDecisionTracker,
							BigDataStackEventSeverity.Info,
							"Operation Sequence: '"+sequenceID+"' succeeded, triggering follow on sequence '"+sequence.getOnSuccessDo()+"'",
							"An operation sequence runner pod for '"+sequenceID+"' with index "+sequence.getIndex()+" reported completion and has an on success trigger to start '"+sequence.getOnSuccessDo()+"'",
							sequenceID,
							sequence.getIndex()
							);
					
					BigDataStackOperationSequence seq = manager.sequenceTemplateClient.getOperationSequence(appID, sequence.getOnSuccessDo(), 0, null);
					if (seq==null) {
						manager.eventUtil.registerEvent(
								appID,
								owner,
								namespace,
								BigDataStackEventType.GlobalDecisionTracker,
								BigDataStackEventSeverity.Error,
								"Triggering operation sequence '"+sequence.getOnSuccessDo()+"' failed",
								"An operation sequence runner pod for '"+sequenceID+"' with index "+sequence.getIndex()+" reported completion and has an on success trigger to start '"+sequence.getOnSuccessDo()+"', but no sequence with that id was found",
								sequence.getOnFailDo(),
								0
								);
						return;
					}
					Map<String,String> params = new HashMap<String,String>();
					manager.executeSequenceFromTemplate(seq, params);
				}
				
			} else {
				/*manager.eventUtil.registerEvent(
						appID,
						owner,
						namespace,
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Failed operation sequence pod based on template : '"+sequenceID+"'",
						"Attempted to run operation sequence '"+sequenceID+"' with index "+sequence.getIndex()+", but one or more operations failed",
						sequenceID,
						sequence.getIndex()
						);*/
				
				if (sequence.getOnFailDo()!=null) {
					// We have something to start if the previous sequence failed
					
					manager.eventUtil.registerEvent(
							appID,
							owner,
							namespace,
							BigDataStackEventType.GlobalDecisionTracker,
							BigDataStackEventSeverity.Info,
							"Operation Sequence: '"+sequenceID+"' failed, triggering specified follow on sequence '"+sequence.getOnFailDo()+"'",
							"An operation sequence runner pod for '"+sequenceID+"' with index "+sequence.getIndex()+" reported failed, but has an on fail trigger to start '"+sequence.getOnFailDo()+"'",
							sequenceID,
							sequence.getIndex()
							);
					
					BigDataStackOperationSequence seq = manager.sequenceTemplateClient.getOperationSequence(appID, sequence.getOnFailDo(), 0, null);
					if (seq==null) {
						manager.eventUtil.registerEvent(
								appID,
								owner,
								namespace,
								BigDataStackEventType.GlobalDecisionTracker,
								BigDataStackEventSeverity.Error,
								"Triggering operation sequence '"+sequence.getOnFailDo()+"' failed",
								"An operation sequence runner pod for '"+sequenceID+"' with index "+sequence.getIndex()+" reported failure and has an on fail trigger to start '"+sequence.getOnFailDo()+"', but no sequence with that id was found",
								sequence.getOnFailDo(),
								0
								);
						return;
					}
					Map<String,String> params = new HashMap<String,String>();
					manager.executeSequenceFromTemplate(seq, params);
				}
			}
			
			manager.printTimings();
			
			manager.shutdown();
			
			return;
			
			
		} else if (args[0].equalsIgnoreCase("costEstimator")) {
			
			
			JDBCDB database = new MySQLDB(dbhost, Integer.parseInt(dbport), dbname, dbusername, dbpassword);
			
			OpenshiftStatusClient openshiftStatus = null;
			//if (occlient.equalsIgnoreCase("openshift3")) openshiftStatus = new OpenshiftStatusClientv3(ochost, Integer.parseInt(ocport), ocusername, ocpassword);
			if (occlient.equalsIgnoreCase("fabric8io")) openshiftStatus = new OpenshiftStatusFabric8ioClient(ochost, Integer.parseInt(ocport), ocusername, ocpassword, namespace);
			if (openshiftStatus==null) {
				System.err.println("Openshift client '"+occlient+"' is not supported");
				return;
			}
			openshiftStatus.connectToOpenshift();
			
			RabbitMQClient rabbitMQClient = new RabbitMQClient(rmqhost, Integer.parseInt(rmqport), rmqusername, rmqpassword);
			
			CostExporter monitorThread = new CostExporter(openshiftStatus, rabbitMQClient, database, owner, namespace);
			monitorThread.run();
			openshiftStatus.close();
		
		} else if (args[0].equalsIgnoreCase("role")) {
			
			DatabaseConf databaseConf = new DatabaseConf(dbtype, dbhost, Integer.parseInt(dbport), dbname, dbusername, dbpassword);
			OpenshiftConfig openshiftConf = new OpenshiftConfig(occlient, ochost, Integer.parseInt(ocport), ocusername, ocpassword, ochostExtension,ocimagerepositoryhost,namespace);
			RabbitMQConf rabbitMQConf = new RabbitMQConf(rmqhost, Integer.parseInt(rmqport), rmqusername, rmqpassword);
			GDTConfig gdtConf = new GDTConfig(databaseConf,rabbitMQConf,openshiftConf);
			
			GDTManager manager = new GDTManager(gdtConf);
			
			try {
				ROLE role = new ROLE(manager, owner, namespace);
				role.run();
			} catch (Exception e) {e.printStackTrace();}
			
		
			
		} else if (args[0].equalsIgnoreCase("api")) {
			
			DatabaseConf databaseConf = new DatabaseConf(dbtype, dbhost, Integer.parseInt(dbport), dbname, dbusername, dbpassword);
			OpenshiftConfig openshiftConf = new OpenshiftConfig(occlient, ochost, Integer.parseInt(ocport), ocusername, ocpassword, ochostExtension,ocimagerepositoryhost,namespace);
			RabbitMQConf rabbitMQConf = new RabbitMQConf(rmqhost, Integer.parseInt(rmqport), rmqusername, rmqpassword);
			GDTConfig gdtConf = new GDTConfig(databaseConf,rabbitMQConf,openshiftConf);
			
			GDTManager manager = new GDTManager(gdtConf);
			
			try {
				String[] commandArgs = {"server", "api.json"};
				new GDTAPI(manager).run(commandArgs); // Create a new online api and run it
			} catch (Exception e) {e.printStackTrace();}
			
		
		} else if (args[0].equalsIgnoreCase("resourceMonitor")) {
			
			
			JDBCDB database = new MySQLDB(dbhost, Integer.parseInt(dbport), dbname, dbusername, dbpassword);
			
			
			RabbitMQClient rabbitMQClient = new RabbitMQClient(rmqhost, Integer.parseInt(rmqport), rmqusername, rmqpassword);
			
			String centralPrometheusHost = System.getenv("prometheusHost");
			String writeDIR = System.getenv("writeDIR");
			
			OpenshiftResourceMonitorThread monitorThread = new OpenshiftResourceMonitorThread(rabbitMQClient, database, owner, namespace, centralPrometheusHost, writeDIR);
			monitorThread.run();
			
		} else if (args[0].equalsIgnoreCase("gpuFileExporter")) {
			
			String pod2gpuFile =  System.getenv("pod2gpuFile");
			String podID =  System.getenv("podID");
			GPUFileExporter exporter = new GPUFileExporter(pod2gpuFile, podID, owner, namespace);
			exporter.run();
			
		} else if (args[0].equalsIgnoreCase("gpuFileExporterController")) {
			
			String pod2gpuFile =  System.getenv("pod2gpuFile");
			GPUFileExporterController exporter = new GPUFileExporterController(pod2gpuFile, ochost, Integer.parseInt(ocport), ocusername, ocpassword, namespace);
			exporter.run();
			
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
