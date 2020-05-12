package eu.bigdatastack.gdt.application;

import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.threads.OpenshiftProjectMonitoringThread;

public class GDTMain {

	public static void main(String[] args) {
		
		if (args[0].equalsIgnoreCase("namespaceMonitor")) {
			
			
			String dbhost = System.getenv("dbhost");
			int dbport = Integer.parseInt(System.getenv("dbport"));
			String dbname = System.getenv("dbname");
			String dbusername = System.getenv("dbusername");
			String dbpassword = System.getenv("dbpassword");
			
			LXDB database = new LXDB(dbhost, dbport, dbname, dbusername, dbpassword);
			
			String ochost =  System.getenv("ochost");
			int ocport = Integer.parseInt(System.getenv("ocport"));
			String ocusername = System.getenv("ocusername");
			String ocpassword = System.getenv("ocpassword");
			
			OpenshiftStatusClient openshiftStatus = new OpenshiftStatusClient(ochost, ocport, ocusername, ocpassword);
			openshiftStatus.connectToOpenshift();
			
			String rmqhost = System.getenv("rmqhost");
			int rmqport = Integer.parseInt(System.getenv("rmqport"));
			String rmqusername = System.getenv("rmqusername");
			String rmqpassword = System.getenv("rmqpassword");
			
			RabbitMQClient rabbitMQClient = new RabbitMQClient(rmqhost, rmqport, rmqusername, rmqpassword);
			
			String owner =  System.getenv("owner");
			String namespace =  System.getenv("namespace");
			
			OpenshiftProjectMonitoringThread monitorThread = new OpenshiftProjectMonitoringThread(openshiftStatus, rabbitMQClient, database, owner, namespace);
			monitorThread.run();
			openshiftStatus.close();
		}
		
	}
	
}
