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
import java.util.Map;

import org.terrier.realization.operations.BigDataStackOperation;
import org.terrier.realization.structures.Timed;
import org.terrier.realization.structures.data.BigDataStackOperationSequence;
import org.terrier.realization.structures.data.BigDataStackOperationSequenceMode;

import com.fasterxml.jackson.core.JsonProcessingException;
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

/**
 * This class is responsible for writing data about operation sequences for a user's application
 * into the database. Note, these sequences have status information associated to them, so may be
 * updated semi-frequently. 
 *
 */
public class BigDataStackOperationSequenceIO implements Timed {

	protected String tableName = "BigDataStackOperationSequences";
	protected ObjectMapper mapper = new ObjectMapper();
	
	JDBCDB client;
	long totalTime = 0;
	boolean init = false;
	public BigDataStackOperationSequenceIO(JDBCDB client, boolean template) throws SQLException {
		this.client = client;
		
		if (template) tableName=tableName+"Templates";
		
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
        ResultSet rs = md.getTables(null, null, "%", null);
        
        boolean tableExists = false;
        
        while (rs.next()) {
          if (rs.getString(3).equalsIgnoreCase(tableName)) {
        	  tableExists = true;
          }
        }

		if (!tableExists) {
			Statement statement = conn.createStatement();
			statement.executeUpdate("CREATE TABLE "+tableName+" ( "+
			"appID VARCHAR(100), "+
			"owner VARCHAR(140), "+
			"namespace VARCHAR(140), "+
			"instance INT, "+
			"sequenceID VARCHAR(100), "+
			"name VARCHAR(140), "+
			"description VARCHAR(1000), "+
			"jsonOperations TEXT(65535), "+
			"parameters VARCHAR(5000), "+
			"mode VARCHAR(100), "+
			"PRIMARY KEY (appID,sequenceID,instance)"+
			")");
			
			conn.commit();
		}
		totalTime+=System.currentTimeMillis()-startTime;
		conn.close();
	}

	/**
	 * Add a new BigDataStack operation sequence to the LXS database. 
	 * Use updateSequence to change the content for an existing object
	 * @param app
	 * @return whether the insert was successful
	 * @throws SQLException
	 * @throws JsonProcessingException 
	 */
	public boolean addSequence(BigDataStackOperationSequence sequence) throws SQLException, JsonProcessingException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		String operationsAsJson = mapper.writeValueAsString(sequence.getOperations());
		String parametersAsJson = mapper.writeValueAsString(sequence.getParameters());
		
		if (operationsAsJson.length()>=65535) return false;
		if (parametersAsJson.length()>=5000) return false;
		
		if (getSequence(sequence.getAppID(), sequence.getSequenceID(), sequence.getIndex())!=null) return false;
		
		Connection conn = client.openConnection();
		
		try {
			PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO "+tableName+" (appID, owner, namespace, instance, sequenceID, name, description, jsonOperations, mode, parameters)"+
				" VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");

		
			statement.setString(1, SQLUtils.prepareTextNoQuote(sequence.getAppID(),100));
			statement.setString(2, SQLUtils.prepareTextNoQuote(sequence.getOwner(),140));
			statement.setString(3, SQLUtils.prepareTextNoQuote(sequence.getNamespace(),140));
			statement.setInt(4, sequence.getIndex());
			statement.setString(5, SQLUtils.prepareTextNoQuote(sequence.getSequenceID(),100));
			statement.setString(6, SQLUtils.prepareTextNoQuote(sequence.getName(),140));
			statement.setString(7, SQLUtils.prepareTextNoQuote(sequence.getDescription(),1000));
			statement.setString(8, operationsAsJson);
			statement.setString(9, SQLUtils.prepareTextNoQuote(sequence.getMode().name(),100));
			statement.setString(10,parametersAsJson);
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
	 * Returns a previously stored BigDataStack Operation Sequence. 
	 * @return
	 * @throws SQLException
	 */
	public BigDataStackOperationSequence getSequence(String appID, String sequenceID, int instance) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		Statement statement = conn.createStatement();
		statement.execute("SELECT DISTINCT * FROM "+tableName+" WHERE sequenceID='"+sequenceID+"' AND appID='"+appID+"' AND instance="+instance);
		ResultSet results = statement.getResultSet();
		
		BigDataStackOperationSequence sequence = null;
		
		 try {
			while (results.next()) {

				List<BigDataStackOperation> operations = new ArrayList<BigDataStackOperation>(5);
				
				Iterator<JsonNode> operationArray = mapper.readTree(results.getString("jsonOperations")).iterator();
				while (operationArray.hasNext()) {
					JsonNode node = operationArray.next();
					
					String className = node.get("className").asText();
					
					BigDataStackOperation operation = (BigDataStackOperation) mapper.readValue(node.toString(), Class.forName(className));
					
					operations.add(operation);
				}
				
				@SuppressWarnings("unchecked")
				Map<String,String> parameters = mapper.readValue(results.getString("parameters"), Map.class);
				
				
				sequence = new BigDataStackOperationSequence(
						results.getString("appID"),
						results.getString("owner"),
						results.getString("namespace"),
						results.getInt("instance"),
						results.getString("sequenceID"),
						results.getString("name"),
						results.getString("description"),
						parameters,
						operations,
						BigDataStackOperationSequenceMode.valueOf(results.getString("mode"))
					);
				
				if (sequence.getParameters().containsKey("onSuccessDo")) sequence.setOnSuccessDo(sequence.getParameters().get("onSuccessDo"));
				if (sequence.getParameters().containsKey("onFailDo")) sequence.setOnFailDo(sequence.getParameters().get("onFailDo"));
				
			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return sequence;
	}
	
	/**
	 * Returns a previously stored BigDataStack Operation Sequence, assumes instance ID 0 (template). 
	 * @return
	 * @throws SQLException
	 */
	public BigDataStackOperationSequence getSequence(String appID, String sequenceID) throws SQLException {
		return getSequence(appID, sequenceID, 0);
	}
	
	
	/**
	 * Update the data for an existing BigDataStack operation sequence. This can only change the constituent 
	 * operations, not the object metadata
	 * @param sequence
	 * @return
	 * @throws SQLException
	 * @throws JsonProcessingException 
	 */
	public boolean updateSequence(BigDataStackOperationSequence sequence) throws SQLException, JsonProcessingException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		String operationsAsJson = mapper.writeValueAsString(sequence.getOperations());
		String parametersAsJson = mapper.writeValueAsString(sequence.getParameters());
		
		if (operationsAsJson.length()>=65535) return false;
		if (parametersAsJson.length()>=5000) return false;
		
		Connection conn = client.openConnection();
		
		try {
			
			PreparedStatement statement = conn.prepareStatement("UPDATE "+tableName+" SET name=?, description=?, jsonOperations=?, mode=?, parameters=?"+
					" WHERE appID="+SQLUtils.prepareText(sequence.getAppID(),100)+
					" AND owner="+SQLUtils.prepareText(sequence.getOwner(),140)+
					" AND namespace="+SQLUtils.prepareText(sequence.getNamespace(),140)+
					" AND sequenceID="+SQLUtils.prepareText(sequence.getSequenceID(),100)+
					" AND instance="+sequence.getIndex());
			
			statement.setNString(1, sequence.getName());
			statement.setNString(2, sequence.getDescription());
			statement.setNString(3, operationsAsJson);
			statement.setNString(4, sequence.getMode().name());
			statement.setNString(5, parametersAsJson);
			
			statement.executeUpdate();
			conn.commit();
			
		} catch (Exception e) {
			e.printStackTrace();
			conn.close();
			totalTime+=System.currentTimeMillis()-startTime;
			return false;
		}
		
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return true;
	}
	
	
	
	/**
	 * Returns all operation sequences in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackOperationSequence> getOperationSequences(String owner, String appID, String sequenceID) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		Statement statement = conn.createStatement();
		
		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT * FROM "+tableName+" WHERE appID='"+appID+"'");
		if (owner!=null) baseStatement.append(" AND owner='"+owner+"'");
		if (sequenceID!=null) baseStatement.append(" AND sequenceID='"+sequenceID+"'");
		
		statement.execute(baseStatement.toString());
		ResultSet results = statement.getResultSet();
		
		List<BigDataStackOperationSequence> retrievedSequences = new ArrayList<BigDataStackOperationSequence>(5);
			
		 try {
			while (results.next()) {
				 
				List<BigDataStackOperation> operations = new ArrayList<BigDataStackOperation>(5);
				
				Iterator<JsonNode> operationArray = mapper.readTree(results.getString("jsonOperations")).iterator();
				while (operationArray.hasNext()) {
					JsonNode node = operationArray.next();
					
					String className = node.get("className").asText();
					
					BigDataStackOperation operation = (BigDataStackOperation) mapper.readValue(node.toString(), Class.forName(className));
					
					operations.add(operation);
				}
				
				@SuppressWarnings("unchecked")
				Map<String,String> parameters = mapper.readValue(results.getString("parameters"), Map.class);
				
				BigDataStackOperationSequence sequence = new BigDataStackOperationSequence(
						results.getString("appID"),
						results.getString("owner"),
						results.getString("namespace"),
						results.getInt("instance"),
						results.getString("sequenceID"),
						results.getString("name"),
						results.getString("description"),
						parameters,
						operations,
						BigDataStackOperationSequenceMode.valueOf(results.getString("mode"))
					);
				
				if (sequence.getParameters().containsKey("onSuccessDo")) sequence.setOnSuccessDo(sequence.getParameters().get("onSuccessDo"));
				if (sequence.getParameters().containsKey("onFailDo")) sequence.setOnFailDo(sequence.getParameters().get("onFailDo"));
				 
				retrievedSequences.add(sequence);
				 
				 
			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return retrievedSequences;
	}
	
	
	/**
	 * Returns all operation sequences in reverse chronological order for a particular application and sequenceID. 
	 * @param appID
	 * @param owner
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackOperationSequence> getOperationSequences(String appID, String sequenceID) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		Statement statement = conn.createStatement();
		
		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT * FROM "+tableName+" WHERE appID='"+appID+"' AND sequenceID='"+sequenceID+"'");
		
		statement.execute(baseStatement.toString());
		ResultSet results = statement.getResultSet();
		
		List<BigDataStackOperationSequence> retrievedSequences = new ArrayList<BigDataStackOperationSequence>(5);
			
		 try {
			while (results.next()) {
				 
				List<BigDataStackOperation> operations = new ArrayList<BigDataStackOperation>(5);
				
				Iterator<JsonNode> operationArray = mapper.readTree(results.getString("jsonOperations")).iterator();
				while (operationArray.hasNext()) {
					JsonNode node = operationArray.next();
					
					String className = node.get("className").asText();
					
					BigDataStackOperation operation = (BigDataStackOperation) mapper.readValue(node.toString(), Class.forName(className));
					
					operations.add(operation);
				}
				
				@SuppressWarnings("unchecked")
				Map<String,String> parameters = mapper.readValue(results.getString("parameters"), Map.class);
				
				BigDataStackOperationSequence sequence = new BigDataStackOperationSequence(
						results.getString("appID"),
						results.getString("owner"),
						results.getString("namespace"),
						results.getInt("instance"),
						results.getString("sequenceID"),
						results.getString("name"),
						results.getString("description"),
						parameters,
						operations,
						BigDataStackOperationSequenceMode.valueOf(results.getString("mode"))
					);
				
				if (sequence.getParameters().containsKey("onSuccessDo")) sequence.setOnSuccessDo(sequence.getParameters().get("onSuccessDo"));
				if (sequence.getParameters().containsKey("onFailDo")) sequence.setOnFailDo(sequence.getParameters().get("onFailDo"));
				 
				retrievedSequences.add(sequence);
				 
				 
			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return retrievedSequences;
	}
	
	/**
	 * Returns a particular operation sequence for a particular application and sequenceID. 
	 * @param appID
	 * @param owner
	 * @return
	 * @throws SQLException
	 */
	public BigDataStackOperationSequence getOperationSequence(String appID, String sequenceID, int instance, String owner) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		Statement statement = conn.createStatement();
		
		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT * FROM "+tableName+" WHERE appID='"+appID+"' AND sequenceID='"+sequenceID+"' AND instance="+instance);
		if (owner!=null) baseStatement.append(" AND owner='"+owner+"'");
		
		statement.execute(baseStatement.toString());
		ResultSet results = statement.getResultSet();
		
		BigDataStackOperationSequence sequence = null;
			
		 try {
			while (results.next()) {
				 
				List<BigDataStackOperation> operations = new ArrayList<BigDataStackOperation>(5);
				
				Iterator<JsonNode> operationArray = mapper.readTree(results.getString("jsonOperations")).iterator();
				while (operationArray.hasNext()) {
					JsonNode node = operationArray.next();
					
					String className = node.get("className").asText();
					
					BigDataStackOperation operation = (BigDataStackOperation) mapper.readValue(node.toString(), Class.forName(className));
					
					operations.add(operation);
				}
				
				@SuppressWarnings("unchecked")
				Map<String,String> parameters = mapper.readValue(results.getString("parameters"), Map.class);
				
				sequence = new BigDataStackOperationSequence(
						results.getString("appID"),
						results.getString("owner"),
						results.getString("namespace"),
						results.getInt("instance"),
						results.getString("sequenceID"),
						results.getString("name"),
						results.getString("description"),
						parameters,
						operations,
						BigDataStackOperationSequenceMode.valueOf(results.getString("mode"))
					);
				if (sequence.getParameters().containsKey("onSuccessDo")) sequence.setOnSuccessDo(sequence.getParameters().get("onSuccessDo"));
				if (sequence.getParameters().containsKey("onFailDo")) sequence.setOnFailDo(sequence.getParameters().get("onFailDo"));
				 
			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return sequence;
	}
	
	public boolean delete(String owner, String namespace, String appID, String sequenceID, int instance) {

		try {
			if (!init) { initTable(); init=true;}
			long startTime = System.currentTimeMillis();
			Connection conn = client.openConnection();

			Statement statement = conn.createStatement();

			StringBuilder baseStatement = new StringBuilder();
			baseStatement.append("DELETE FROM "+tableName+" WHERE owner='"+owner+"'");
			if (namespace!=null) baseStatement.append(" AND namespace='"+namespace+"'");
			if (appID!=null) baseStatement.append(" AND appID='"+appID+"'");
			if (sequenceID!=null) baseStatement.append(" AND sequenceID='"+sequenceID+"'");
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
