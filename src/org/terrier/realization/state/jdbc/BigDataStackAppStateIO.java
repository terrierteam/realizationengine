package org.terrier.realization.state.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.terrier.realization.operations.BigDataStackOperation;
import org.terrier.realization.structures.Timed;
import org.terrier.realization.structures.data.BigDataStackAppState;
import org.terrier.realization.structures.data.BigDataStackAppStateCondition;

import com.fasterxml.jackson.databind.JsonNode;
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

public class BigDataStackAppStateIO implements Timed {

	protected final String tableName = "BigDataStackAppState";
	protected ObjectMapper mapper = new ObjectMapper();

	JDBCDB client;
	long totalTime = 0;
	boolean init = false;
	public BigDataStackAppStateIO(JDBCDB client) throws SQLException {
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
					+ "owner VARCHAR(140), " + "namespace VARCHAR(140), " + "appStateID VARCHAR(100), "
					+ "name VARCHAR(1000), " + "notInStates VARCHAR(1000), " + "sequences VARCHAR(1000), " + "conditions VARCHAR(5000), "
					+ "PRIMARY KEY (owner,appID,namespace,appStateID)" + ")");

			conn.commit();
		}
		totalTime+=System.currentTimeMillis()-startTime;
		conn.close();
	}

	/**
	 * Add a new BigDataStack App State to the database.
	 * 
	 * @param appState
	 * @return whether the insert was successful
	 * @throws SQLException
	 */
	public boolean addAppState(BigDataStackAppState appState) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		
		
		
		
		Connection conn = client.openConnection();

		try {
			
			String notInStatesAsJson = mapper.writeValueAsString(appState.getNotInStates());
			String sequencesAsJson = mapper.writeValueAsString(appState.getSequences());
			String conditionsAsJson = mapper.writeValueAsString(appState.getConditions());
			
			if (notInStatesAsJson.length()>=1000) return false;
			if (sequencesAsJson.length()>=1000) return false;
			if (conditionsAsJson.length()>=5000) return false;
			
			PreparedStatement statement = conn.prepareStatement(
					"INSERT INTO "+tableName+" (appID, owner, namespace, appStateID, name, notInStates, sequences, conditions)"+
					" VALUES ( ?, ?, ?, ?, ?, ?, ?, ? )");

			
				statement.setString(1,SQLUtils.prepareTextNoQuote(appState.getAppID(),100));
				statement.setString(2,SQLUtils.prepareTextNoQuote(appState.getOwner(),140));
				statement.setString(3,SQLUtils.prepareTextNoQuote(appState.getNamespace(),140));
				statement.setString(4, SQLUtils.prepareTextNoQuote(appState.getAppStateID(),100));
				statement.setString(5, SQLUtils.prepareTextNoQuote(appState.getName(),1000));
				statement.setString(6, notInStatesAsJson);
				statement.setString(7, sequencesAsJson);
				statement.setString(8, conditionsAsJson);
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

	/**
	 * Returns a specified application states
	 * @param owner
	 * @param appID
	 * @param objectID
	 * @param instance
	 * @param metricName
	 * @param sloIndex
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackAppState> getAppStates(String owner, String appID, String namespace, String appStateID) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		Statement statement = conn.createStatement();
		
		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT * FROM "+tableName+" WHERE owner='"+owner+"'");
		if (appID!=null) baseStatement.append(" AND appID='"+appID+"'");
		if (namespace!=null) baseStatement.append(" AND namespace='"+namespace+"'");
		if (appStateID!=null) baseStatement.append(" AND appStateID='"+appStateID+"'");
		
		statement.execute(baseStatement.toString());
		ResultSet results = statement.getResultSet();
		List<BigDataStackAppState> appStates = new ArrayList<BigDataStackAppState>();
		
		 try {
			while (results.next()) {
				 
				List<String> notInStates = new ArrayList<String>();
				Iterator<JsonNode> notInStatesArray = mapper.readTree(results.getString("notInStates")).iterator();
				while (notInStatesArray.hasNext()) {
					JsonNode nis = notInStatesArray.next();
					notInStates.add(nis.asText());
				}
				
				
				List<String> sequences = new ArrayList<String>();
				Iterator<JsonNode> sequencesArray = mapper.readTree(results.getString("sequences")).iterator();
				while (sequencesArray.hasNext()) {
					JsonNode sa = sequencesArray.next();
					sequences.add(sa.asText());
				}
				
				List<BigDataStackAppStateCondition> conditions = new ArrayList<BigDataStackAppStateCondition>();
				Iterator<JsonNode> conditionsArray = mapper.readTree(results.getString("conditions")).iterator();
				while (conditionsArray.hasNext()) {
					JsonNode condition = conditionsArray.next();
					
					BigDataStackAppStateCondition conditionO = mapper.readValue(condition.toPrettyString(), BigDataStackAppStateCondition.class);
					
					conditions.add(conditionO);
				}
				
				
				BigDataStackAppState appState = new BigDataStackAppState(
						results.getString("appID"),
						results.getString("owner"),
						results.getString("namespace"),
						results.getString("appStateID"),
						results.getString("name"),
						notInStates,
						sequences,
						conditions
						);
				appStates.add(appState);
			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return appStates;
	}
	
	
	
	/**
	 * Update the data for an existing BigDataStack SLO. 
	 * @param status
	 * @return
	 * @throws SQLException
	 */
	public boolean updateAppState(BigDataStackAppState appState) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		
		
		try {
			String notInStatesAsJson = mapper.writeValueAsString(appState.getNotInStates());
			String sequencesAsJson = mapper.writeValueAsString(appState.getSequences());
			String conditionsAsJson = mapper.writeValueAsString(appState.getConditions());
			
			if (notInStatesAsJson.length()>=1000) return false;
			if (sequencesAsJson.length()>=1000) return false;
			if (conditionsAsJson.length()>=5000) return false;
			
			PreparedStatement statement = conn.prepareStatement("UPDATE "+tableName+" SET name=?, notInStates=?, sequences=?, conditions=?"+
					" WHERE appID="+SQLUtils.prepareText(appState.getAppID(),100)+
					" AND owner="+SQLUtils.prepareText(appState.getOwner(),140)+
					" AND namespace="+SQLUtils.prepareText(appState.getNamespace(),140)+
					" AND appStateID="+SQLUtils.prepareText(appState.getAppStateID(),100));
			
			statement.setNString(1, appState.getName());
			statement.setNString(2, notInStatesAsJson);
			statement.setNString(3, sequencesAsJson);
			statement.setNString(4, conditionsAsJson);

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
	
	public boolean delete(String owner, String namespace, String appID, String appStateID) {

		try {
			if (!init) { initTable(); init=true;}
			long startTime = System.currentTimeMillis();
			Connection conn = client.openConnection();

			Statement statement = conn.createStatement();

			StringBuilder baseStatement = new StringBuilder();
			baseStatement.append("DELETE FROM "+tableName+" WHERE owner='"+owner+"'");
			if (namespace!=null) baseStatement.append(" AND namespace='"+namespace+"'");
			if (appID!=null) baseStatement.append(" AND appID='"+appID+"'");
			if (appStateID!=null) baseStatement.append(" AND appStateID='"+appStateID+"'");
			

			
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