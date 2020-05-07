package test;

import java.sql.SQLException;

import eu.bigdatastack.gdt.lxdb.BigDataStackApplicationIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackEventIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackMetricIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackNamespaceStateIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackOperationSequenceIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackPodStatusIO;
import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.structures.config.DatabaseConf;

/**
 * Provides utility methods for running tests
 * @author EbonBlade
 *
 */
public class TestUtil {

	/**
	 * Clears contents of all database tables
	 * @param dbconf
	 * @throws Exception
	 */
	public static void clearDatabase(DatabaseConf dbconf) throws Exception {
		
		LXDB database;
		if (dbconf.getPassword()!=null && dbconf.getPassword().length()>0) database = new LXDB(dbconf.getHost(), dbconf.getPort(), dbconf.getName(), dbconf.getUsername(), dbconf.getPassword());
		else database = new LXDB(dbconf.getHost(), dbconf.getPort(), dbconf.getName(), dbconf.getUsername());
		
		BigDataStackApplicationIO appClient = new BigDataStackApplicationIO(database);
		appClient.clearTable();
		
		BigDataStackEventIO eventClient = new BigDataStackEventIO(database);
		eventClient.clearTable();
		
		BigDataStackMetricIO metricClient = new BigDataStackMetricIO(database);
		metricClient.clearTable();
		
		BigDataStackObjectIO objectClient = new BigDataStackObjectIO(database, false);
		objectClient.clearTable();
		
		BigDataStackObjectIO objectClient2 = new BigDataStackObjectIO(database, true);
		objectClient2.clearTable();
		
		BigDataStackOperationSequenceIO osClient = new BigDataStackOperationSequenceIO(database, false);
		osClient.clearTable();
		
		BigDataStackOperationSequenceIO osClient2 = new BigDataStackOperationSequenceIO(database, true);
		osClient2.clearTable();
		
		BigDataStackPodStatusIO podStatusClient = new BigDataStackPodStatusIO(database);
		podStatusClient.clearTable();
		
		BigDataStackNamespaceStateIO namespaceStateClient = new BigDataStackNamespaceStateIO(database);
		namespaceStateClient.clearTable();
		
	}
	
}
