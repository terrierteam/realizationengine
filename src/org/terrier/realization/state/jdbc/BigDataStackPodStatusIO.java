package org.terrier.realization.state.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.terrier.realization.structures.Timed;
import org.terrier.realization.structures.data.BigDataStackPodStatus;

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

public class BigDataStackPodStatusIO implements Timed {

	protected final String tableName = "BigDataStackPodStatus";

	JDBCDB client;
	long totalTime = 0;
	boolean init = false;
	public BigDataStackPodStatusIO(JDBCDB client) throws SQLException {
		this.client = client;

		//initTable();
	}

	/**
	 * Check whether the table exists in the DB already and if not creates it
	 * 
	 * @throws SQLException
	 */
	public void initTable() throws SQLException {
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();

		DatabaseMetaData md = conn.getMetaData();
		ResultSet rs = md.getTables(null, null, "%", null);

		boolean tableExists = false;

		while (rs.next()) {
			if (rs.getString(3).equalsIgnoreCase(tableName)) {
				tableExists = true;
			}
		}

		if (!tableExists) {
			Statement statement = conn.createStatement();
			statement.executeUpdate("CREATE TABLE " + tableName + " ( " + "appID VARCHAR(100), "
					+ "owner VARCHAR(140), " + "podID VARCHAR(100), " + "objectID VARCHAR(100), " + "namespace VARCHAR(140), " + "instance INT, "
					+ "status VARCHAR(100), " + "podIP VARCHAR(50), " + "hostIP VARCHAR(50), "
					+ "PRIMARY KEY (podID)" + ")");

			conn.commit();
		}
		totalTime+=System.currentTimeMillis()-startTime;
		conn.close();
	}

	/**
	 * Add a new BigDataStack pod status to the database. The event will only be added if
	 * it is unique.
	 * 
	 * @param status
	 * @return whether the insert was successful
	 * @throws SQLException
	 */
	public boolean addPodStatus(BigDataStackPodStatus status) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		
		Connection conn = client.openConnection();

		Statement statement = conn.createStatement();
		try {
			statement.executeUpdate("INSERT INTO " + tableName
					+ " (appID, owner, podID, objectID, namespace, status, podIP, hostIP, instance)"
					+ " VALUES ( " + SQLUtils.prepareText(status.getAppID(), 100) + ", "
					+ SQLUtils.prepareText(status.getOwner(), 140) + ", "
					+ SQLUtils.prepareText(status.getPodID(), 100) + ", "
					+ SQLUtils.prepareText(status.getObjectID(), 100) + ", "
					+ SQLUtils.prepareText(status.getNamespace(), 140) + ", "
					+ SQLUtils.prepareText(status.getStatus(), 100) + ", "
					+ SQLUtils.prepareText(status.getPodIP(), 50) + ", "
					+ SQLUtils.prepareText(status.getHostIP(), 50) + ", "
					+ status.getInstance() + " )");
		} catch (Exception e) {
			//e.printStackTrace();
			conn.close();
			return false;
		}

		conn.commit();
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return true;
	}

	/**
	 * Returns the status for a particular named pod. 
	 * @param appID
	 * @param objectID
	 * @param podID
	 * @return
	 * @throws SQLException
	 */
	public BigDataStackPodStatus getPodStatus(String podID) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		Statement statement = conn.createStatement();
		
		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT * FROM "+tableName+" WHERE podID='"+podID+"'");
		
		statement.execute(baseStatement.toString());
		ResultSet results = statement.getResultSet();
		
		BigDataStackPodStatus status = null;
		
		 try {
			while (results.next()) {
				 
				status = new BigDataStackPodStatus(
						results.getString("appID"),
						results.getString("owner"),
						results.getString("namespace"),
						results.getString("objectID"),
						results.getInt("instance"),
						results.getString("podID"),
						results.getString("status"),
						results.getString("hostIP"),
						results.getString("podIP")
						);

			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return status;
	}
	
	/**
	 * Returns the status for a particular named pod. 
	 * @param appID
	 * @param objectID
	 * @param podID
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackPodStatus> getPodStatuses(String appID, String owner, String objectID, String namespace, int instance) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		Statement statement = conn.createStatement();
		
		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT * FROM "+tableName+" WHERE owner='"+owner+"'");
		if (namespace!=null) baseStatement.append(" AND namespace='"+namespace+"'");
		if (appID!=null) baseStatement.append(" AND appID='"+appID+"'");
		if (objectID!=null) baseStatement.append(" AND objectID='"+objectID+"'");
		if (instance>=0) baseStatement.append(" AND instance="+instance);
		statement.execute(baseStatement.toString());
		ResultSet results = statement.getResultSet();
		
		List<BigDataStackPodStatus> statuses = new ArrayList<BigDataStackPodStatus>();
		
		 try {
			while (results.next()) {
				 
				BigDataStackPodStatus status = new BigDataStackPodStatus(
						results.getString("appID"),
						results.getString("owner"),
						results.getString("namespace"),
						results.getString("objectID"),
						results.getInt("instance"),
						results.getString("podID"),
						results.getString("status"),
						results.getString("hostIP"),
						results.getString("podIP")
						);
				
				statuses.add(status);

			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return statuses;
	}
	
	/**
	 * Update the data for an existing BigDataStack status for a pod. 
	 * @param status
	 * @return
	 * @throws SQLException
	 */
	public boolean updatePodStatus(BigDataStackPodStatus status) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		try {
			
			PreparedStatement statement = conn.prepareStatement("UPDATE "+tableName+" SET status=?, podIP=?, hostIP=?"+
					" WHERE appID="+SQLUtils.prepareText(status.getAppID(),100)+
					" AND objectID="+SQLUtils.prepareText(status.getObjectID(),100)+
					" AND instance="+status.getInstance()+
					" AND podID="+SQLUtils.prepareText(status.getPodID(),100));
			
			statement.setNString(1, SQLUtils.prepareTextNoQuote(status.getStatus(),100));
			statement.setNString(2, SQLUtils.prepareTextNoQuote(status.getPodIP(),50));
			statement.setNString(3, SQLUtils.prepareTextNoQuote(status.getHostIP(),50));
			

			statement.executeUpdate();
			conn.commit();
			
		} catch (Exception e) {
			e.printStackTrace();
			conn.close();
			return false;
		}
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return true;
	}
	
	public boolean delete(String owner, String namespace, String appID, String objectID, int instance) {

		try {
			if (!init) { initTable(); init=true;}
			long startTime = System.currentTimeMillis();
			Connection conn = client.openConnection();

			Statement statement = conn.createStatement();

			StringBuilder baseStatement = new StringBuilder();
			baseStatement.append("DELETE FROM "+tableName+" WHERE owner='"+owner+"'");
			if (namespace!=null) baseStatement.append(" AND namespace='"+namespace+"'");
			if (appID!=null) baseStatement.append(" AND appID='"+appID+"'");
			if (objectID!=null) baseStatement.append(" AND objectID='"+objectID+"'");
			if (instance>=0) baseStatement.append(" AND instance="+instance);
			

			
			statement.execute(baseStatement.toString());
				
			conn.commit();
			conn.close();
			totalTime+=System.currentTimeMillis()-startTime;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
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
			totalTime+=System.currentTimeMillis()-startTime;
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
