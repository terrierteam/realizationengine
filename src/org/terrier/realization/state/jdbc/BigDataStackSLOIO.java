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
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackSLO;

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

public class BigDataStackSLOIO implements Timed {

	protected final String tableName = "BigDataStackSLO";

	JDBCDB client;
	long totalTime = 0;
	boolean init = false;
	
	protected ObjectMapper mapper = new ObjectMapper();
	
	public BigDataStackSLOIO(JDBCDB client) throws SQLException {
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
			statement.executeUpdate("CREATE TABLE " + tableName + " ( " 
			+ "appID VARCHAR(100), "
			+ "owner VARCHAR(140), " 
			+ "namespace VARCHAR(140), "
			+ "triggerID VARCHAR(140), "
			+ "metricName VARCHAR(140), " 
			+ "triggerMessage VARCHAR(3000), "
			+ "type VARCHAR(100), " 
			+ "value DOUBLE, " 
			+ "breachSeverity VARCHAR(100), " 
			+ "action VARCHAR(100), "
			+ "safetyChecks TEXT(65535), "
			+ "coolDownMins INT, " 
			+ "PRIMARY KEY (owner,namespace,appID,triggerID)" + ")");

			conn.commit();
		}
		totalTime+=System.currentTimeMillis()-startTime;
		conn.close();
		
	}

	/**
	 * Add a new BigDataStack SLO to the database.
	 * 
	 * @param slo
	 * @return whether the insert was successful
	 * @throws SQLException
	 */
	public boolean addSLO(BigDataStackSLO slo) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		
		
		
		Connection conn = client.openConnection();

		try {
			
			String operationsAsJson = mapper.writeValueAsString(slo.getSafetyChecks());
			if (operationsAsJson.length()>=65535) return false;
			
			PreparedStatement statement = conn.prepareStatement(
					"INSERT INTO "+tableName+" (appID, owner, namespace, triggerID, metricName, triggerMessage, type, value, breachSeverity, action, safetyChecks, coolDownMins)"+
					" VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");

			
			statement.setNString(1, SQLUtils.prepareTextNoQuote(slo.getAppID(), 100));
			statement.setNString(2, SQLUtils.prepareTextNoQuote(slo.getOwner(), 140));
			statement.setNString(3, SQLUtils.prepareTextNoQuote(slo.getNamespace(), 140));
			statement.setNString(4, SQLUtils.prepareTextNoQuote(slo.getTriggerID(), 140));
			statement.setNString(5, SQLUtils.prepareTextNoQuote(slo.getMetricName(), 140));
			statement.setNString(6, SQLUtils.prepareTextNoQuote(slo.getTriggerMessage(), 3000));
			statement.setNString(7, SQLUtils.prepareTextNoQuote(slo.getType(), 100));
			statement.setDouble(8, slo.getValue());
			statement.setNString(9, SQLUtils.prepareTextNoQuote(slo.getBreachSeverity().name(),100));
			statement.setNString(10, SQLUtils.prepareTextNoQuote(slo.getAction(), 140));
			statement.setNString(11, operationsAsJson);
			statement.setInt(12, slo.getCoolDownMins());
			
			
			statement.executeUpdate();
			conn.commit();
		} catch (Exception e) {
			//e.printStackTrace();
			conn.close();
			return false;
		}

		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return true;
	}

	/**
	 * Returns a specified SLO
	 * @param owner
	 * @param appID
	 * @param objectID
	 * @param instance
	 * @param metricName
	 * @param sloIndex
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackSLO> getSLOs(String owner, String appID, String triggerID) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		Statement statement = conn.createStatement();
		
		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT * FROM "+tableName+" WHERE owner='"+owner+"'");
		if (appID!=null) baseStatement.append(" AND appID='"+appID+"'");
		if (triggerID!=null) baseStatement.append(" AND triggerID='"+triggerID+"'");
		
		statement.execute(baseStatement.toString());
		ResultSet results = statement.getResultSet();
		BigDataStackSLO slo = null;
		
		List<BigDataStackSLO> slos = new ArrayList<BigDataStackSLO>(5);
		
		 try {
			while (results.next()) {
				
				
				List<BigDataStackOperation> operations = new ArrayList<BigDataStackOperation>(5);
				
				Iterator<JsonNode> operationArray = mapper.readTree(results.getString("safetyChecks")).iterator();
				while (operationArray.hasNext()) {
					JsonNode node = operationArray.next();
					
					String className = node.get("className").asText();
					
					BigDataStackOperation operation = (BigDataStackOperation) mapper.readValue(node.toString(), Class.forName(className));
					
					operations.add(operation);
				}
				

				slo = new BigDataStackSLO(
						results.getString("appID"),
						results.getString("owner"),
						results.getString("namespace"),
						results.getString("triggerID"),
						results.getString("metricName"),
						results.getString("triggerMessage"),
						results.getString("type"),
						results.getDouble("value"),
						BigDataStackEventSeverity.valueOf(results.getString("breachSeverity")),
						results.getString("action"),
						operations,
						results.getInt("coolDownMins")
						);
				
				slos.add(slo);

			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return slos;
	}
	
	
	/**
	 * Update the data for an existing BigDataStack SLO. 
	 * @param status
	 * @return
	 * @throws SQLException
	 */
	public boolean updateSLO(BigDataStackSLO slo) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		try {
			
			String operationsAsJson = mapper.writeValueAsString(slo.getSafetyChecks());
			if (operationsAsJson.length()>=65535) return false;
			
			PreparedStatement statement = conn.prepareStatement("UPDATE "+tableName+" SET triggerID=?, metricName=?, triggerMessage=?, type=?, value=?, breachSeverity=?, action=?, safetyChecks=?, coolDownMins=?"+
					" WHERE appID="+SQLUtils.prepareText(slo.getAppID(),100)
					+" AND owner="+SQLUtils.prepareText(slo.getOwner(),140)
					+" AND namespace="+SQLUtils.prepareText(slo.getNamespace(),140));
			
			
			statement.setNString(1, slo.getTriggerID());
			statement.setNString(2, slo.getMetricName());
			statement.setNString(3, slo.getTriggerMessage());
			statement.setNString(4, slo.getType());
			statement.setDouble(5, slo.getValue());
			statement.setNString(6, slo.getBreachSeverity().name());
			statement.setNString(7, slo.getAction());
			statement.setNString(8, operationsAsJson);
			statement.setInt(9, slo.getCoolDownMins());
			
			
			statement.executeUpdate();
			conn.commit();
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
	
	public boolean delete(String owner, String namespace, String appID, String triggerID) {

		try {
			if (!init) { initTable(); init=true;}
			long startTime = System.currentTimeMillis();
			Connection conn = client.openConnection();

			Statement statement = conn.createStatement();

			StringBuilder baseStatement = new StringBuilder();
			baseStatement.append("DELETE FROM "+tableName+" WHERE owner='"+owner+"'");
			if (namespace!=null) baseStatement.append(" AND namespace='"+namespace+"'");
			if (appID!=null) baseStatement.append(" AND appID='"+appID+"'");
			if (triggerID!=null) baseStatement.append(" AND triggerID='"+triggerID+"'");
			

			
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
