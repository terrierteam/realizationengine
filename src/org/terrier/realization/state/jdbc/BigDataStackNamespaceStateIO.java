package org.terrier.realization.state.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.terrier.realization.structures.Timed;
import org.terrier.realization.structures.data.BigDataStackNamespaceState;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

public class BigDataStackNamespaceStateIO implements Timed {

	protected final String tableName = "BigDataStackNamespaceStatus";
	protected ObjectMapper mapper = new ObjectMapper();

	JDBCDB client;
	long totalTime = 0;
	boolean init = false;
	public BigDataStackNamespaceStateIO(JDBCDB client) throws SQLException {
		this.client = client;

		//initTable();
	}

	/**
	 * Check whether the table exists in the DB already and if not creates it
	 * @throws SQLException
	 */
	public void initTable() throws SQLException {
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();

		DatabaseMetaData md = conn.getMetaData();
		ResultSet rs = null;
		try {
			rs = md.getTables(null, null, "%", null);
		} catch (java.lang.NullPointerException e) {}

		boolean tableExists = false;

		if (rs!=null) {
			while (rs.next()) {
				if (rs.getString(3).equalsIgnoreCase(tableName)) {
					tableExists = true;
				}
			}
		}
		

		if (!tableExists) {
			Statement statement = conn.createStatement();
			statement.executeUpdate("CREATE TABLE "+tableName+" ( "+
					"namespace VARCHAR(140), "+
					"host VARCHAR(200), "+
					"port INT, "+
					"clusterMonitoringActive BOOLEAN, "+
					"clusterMonitoringHost VARCHAR(200), "+
					"clusterMonitoringPort INT, "+
					"metricStoreActive BOOLEAN, "+
					"metricStoreHost VARCHAR(200), "+
					"metricStorePort INT, "+
					"logSearchActive BOOLEAN, "+
					"logSearchHost VARCHAR(200), "+
					"logSearchPort INT, "+
					"eventExchangeActive BOOLEAN, "+
					"eventExchangeHost VARCHAR(200), "+
					"eventExchangePort INT, "+
					"PRIMARY KEY (namespace)"+
					")");

			conn.commit();
		}
		totalTime+=System.currentTimeMillis()-startTime;
		conn.close();
	}

	/**
	 * Add a new BigDataStack namespace to the database. The namespace will only be added if unique.
	 * @param app
	 * @return whether the insert was successful
	 * @throws SQLException
	 */
	public boolean addNamespace(BigDataStackNamespaceState namespace) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();

		Statement statement = conn.createStatement();
		try {
			statement.executeUpdate("INSERT INTO "+tableName+" (namespace, host, port, clusterMonitoringActive, clusterMonitoringHost, clusterMonitoringPort, metricStoreActive, metricStoreHost, metricStorePort, logSearchActive, logSearchHost, logSearchPort, eventExchangeActive, eventExchangeHost, eventExchangePort)"+
					" VALUES ( "+
					SQLUtils.prepareText(namespace.getNamespace(),140)+", "+
					SQLUtils.prepareText(namespace.getHost(),200)+", "+
					namespace.getPort()+","+
					namespace.isClusterMonitoringActive()+","+
					SQLUtils.prepareText(namespace.getClusterMonitoringHost(),200)+", "+
					namespace.getClusterMonitoringPort()+","+
					namespace.isMetricStoreActive()+","+
					SQLUtils.prepareText(namespace.getMetricStoreHost(),200)+", "+
					namespace.getMetricStorePort()+","+
					namespace.isLogSearchActive()+","+
					SQLUtils.prepareText(namespace.getLogSearchHost(),200)+", "+
					namespace.getLogSearchPort()+","+
					namespace.isEventExchangeActive()+","+
					SQLUtils.prepareText(namespace.getEventExchangeHost(),200)+", "+
					namespace.getEventExchangePort()+
					" )");
		} catch (Exception e) {
			e.printStackTrace();
			conn.close();
			return false;
		}
	
		conn.commit();
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return true;
	}

	/**
	 * Returns a previously stored BigDataStack Namespace. 
	 * @param namespace
	 * @return
	 * @throws SQLException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public BigDataStackNamespaceState getNamespace(String namespace) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();

		Statement statement = conn.createStatement();
		statement.execute("SELECT DISTINCT * FROM "+tableName+" WHERE namespace='"+namespace+"'");
		ResultSet results = statement.getResultSet();

		BigDataStackNamespaceState state = null;

		try {
			while (results.next()) {


				state = new BigDataStackNamespaceState(
						results.getString("namespace"),
						results.getString("host"),
						results.getInt("port"),
						results.getBoolean("clusterMonitoringActive"),
						results.getString("clusterMonitoringHost"),
						results.getInt("clusterMonitoringPort"),
						results.getBoolean("metricStoreActive"),
						results.getString("metricStoreHost"),
						results.getInt("metricStorePort"),
						results.getBoolean("logSearchActive"),
						results.getString("logSearchHost"),
						results.getInt("logSearchPort"),
						results.getBoolean("eventExchangeActive"),
						results.getString("eventExchangeHost"),
						results.getInt("eventExchangePort")
				);


			}
		} catch (Exception e) {
			e.printStackTrace();
		} 



		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return state;
	}

	/**
	 * Update the data for an existing BigDataStack namespace.
	 * @param namespace
	 * @return
	 * @throws SQLException
	 */
	public boolean updateNamespace(BigDataStackNamespaceState namespace) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();

		try {
			Statement statement = conn.createStatement();
			statement.executeUpdate("UPDATE "+tableName+" SET "+
					"host="+SQLUtils.prepareText(namespace.getHost(),200)+", "+
					"port="+namespace.getPort()+","+
					"clusterMonitoringActive="+namespace.isClusterMonitoringActive()+","+
					"clusterMonitoringHost="+SQLUtils.prepareText(namespace.getClusterMonitoringHost(),200)+", "+
					"clusterMonitoringPort="+namespace.getClusterMonitoringPort()+","+
					"metricStoreActive="+namespace.isMetricStoreActive()+","+
					"metricStoreHost="+SQLUtils.prepareText(namespace.getMetricStoreHost(),200)+", "+
					"metricStorePort="+namespace.getMetricStorePort()+","+
					"logSearchActive="+namespace.isLogSearchActive()+","+
					"logSearchHost="+SQLUtils.prepareText(namespace.getLogSearchHost(),200)+", "+
					"logSearchPort="+namespace.getLogSearchPort()+","+
					"eventExchangeActive="+namespace.isEventExchangeActive()+","+
					"eventExchangeHost="+SQLUtils.prepareText(namespace.getEventExchangeHost(),200)+", "+
					"eventExchangePort="+namespace.getEventExchangePort()+
					" WHERE namespace="+SQLUtils.prepareText(namespace.getNamespace(),140));
		} catch (Exception e) {
			e.printStackTrace();
			conn.close();
			totalTime+=System.currentTimeMillis()-startTime;
			return false;
		}



		conn.commit();
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return true;
	}

	
	
	/**
	 * Deletes the table in the database and re-creates it
	 * @return
	 * @throws SQLException
	 */
	public boolean clearTable() throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		try {
			Statement statement = conn.createStatement();
			statement.execute("DROP TABLE \""+tableName+"\"");

			conn.commit();
			conn.close();
			
			initTable();
			
		} catch (Exception e) {
			e.printStackTrace();
			conn.close();
			return false;
		}
		totalTime+=System.currentTimeMillis()-startTime;
		return true;
	}
	
	@Override
	public long timeSpent() {
		return totalTime;
	}

	
}
