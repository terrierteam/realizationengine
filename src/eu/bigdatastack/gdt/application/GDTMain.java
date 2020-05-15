package eu.bigdatastack.gdt.application;

import java.util.Map;

import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.config.DatabaseConf;
import eu.bigdatastack.gdt.structures.config.GDTConfig;
import eu.bigdatastack.gdt.structures.config.OpenshiftConfig;
import eu.bigdatastack.gdt.structures.config.RabbitMQConf;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;
import eu.bigdatastack.gdt.threads.OpenshiftProjectMonitoringThread;

public class GDTMain {

	public static void main(String[] args) throws Exception {
		
		// Parse Common Environment Variables
		String owner =  System.getenv("owner");
		String namespace =  System.getenv("namespace");
		String appID =  System.getenv("appID");
		String sequenceID =  System.getenv("sequenceID");
		String sequenceInstance =  System.getenv("sequenceInstance");
		String dbhost = System.getenv("dbhost");
		String dbport = System.getenv("dbport");
		String dbname = System.getenv("dbname");
		String dbusername = System.getenv("dbusername");
		String dbpassword = System.getenv("dbpassword");
		String ochost =  System.getenv("ochost");
		String ocport = System.getenv("ocport");
		String ocusername = System.getenv("ocusername");
		String ocpassword = System.getenv("ocpassword");
		String rmqhost = System.getenv("rmqhost");
		String rmqport = System.getenv("rmqport");
		String rmqusername = System.getenv("rmqusername");
		String rmqpassword = System.getenv("rmqpassword");
		
		
		// Launch as a monitoring instance
		if (args[0].equalsIgnoreCase("namespaceMonitor")) {
			
			
			LXDB database = new LXDB(dbhost, Integer.parseInt(dbport), dbname, dbusername, dbpassword);
			
			OpenshiftStatusClient openshiftStatus = new OpenshiftStatusClient(ochost, Integer.parseInt(ocport), ocusername, ocpassword);
			openshiftStatus.connectToOpenshift();
			
			RabbitMQClient rabbitMQClient = new RabbitMQClient(rmqhost, Integer.parseInt(rmqport), rmqusername, rmqpassword);
			
			OpenshiftProjectMonitoringThread monitorThread = new OpenshiftProjectMonitoringThread(openshiftStatus, rabbitMQClient, database, owner, namespace);
			monitorThread.run();
			openshiftStatus.close();
			
		// Launch as a processor for an Operation Sequence
		} else if (args[0].equalsIgnoreCase("operationSequence")) {
			
			DatabaseConf databaseConf = new DatabaseConf(dbhost, Integer.parseInt(dbport), dbname, dbusername, dbpassword);
			OpenshiftConfig openshiftConf = new OpenshiftConfig(ochost, Integer.parseInt(ocport), ocusername, ocpassword);
			RabbitMQConf rabbitMQConf = new RabbitMQConf(rmqhost, Integer.parseInt(rmqport), rmqusername, rmqpassword);
			GDTConfig gdtConf = new GDTConfig(databaseConf,rabbitMQConf,openshiftConf);
			
			GDTManager manager = new GDTManager(gdtConf);
			
			// This should only ever return 1 sequence because we are connecting to the template client
			BigDataStackOperationSequence sequence = manager.getSequenceInstanceClient().getOperationSequence(appID, sequenceID, Integer.parseInt(sequenceInstance));
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
						"Attempted to create the pod object for operation sequence '"+sequenceID+"' with index "+sequence.getIndex()+", but the cluster rejected it (has this sequence instance already been launched?)",
						sequenceID
						);
			}
			
			manager.printTimings();
			
			manager.shutdown();
			
			return;
			
			
		}
		
	}
	
}
