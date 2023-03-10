package org.terrier.realization.state.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.terrier.realization.structures.Timed;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackObjectType;

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

public class BigDataStackObjectIO implements Timed {

	protected String tableName = "BigDataStackObjectDefinitions";
	protected ObjectMapper mapper = new ObjectMapper();
	
	JDBCDB client;
	long totalTime = 0;
	boolean init = false;
	public BigDataStackObjectIO(JDBCDB client, boolean template) throws SQLException {
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
			"objectID VARCHAR(100), "+
			"owner VARCHAR(140), "+
			"type VARCHAR(100), "+
			"status VARCHAR(1000), "+
			"yamlSource TEXT(65535), "+
			"instance INT, "+
			"namespace VARCHAR(140), "+
			"appID VARCHAR(100), "+
			"PRIMARY KEY (objectID,owner,instance)"+
			")");
			
			conn.commit();
		}
		totalTime+=System.currentTimeMillis()-startTime;
		conn.close();
	}

	/**
	 * Add a new BigDataStack object definition to the LXS database. 
	 * Use updateObject to change the content for an existing object
	 * @param app
	 * @return whether the insert was successful
	 * @throws SQLException
	 */
	public boolean addObject(BigDataStackObjectDefinition object) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		if (object.getYamlSource().length()>=65535) {
			System.err.println("Yaml too long");
			return false;
		}
		
		if (getObject(object.getObjectID(), object.getOwner(), object.getInstance())!=null) {
			System.err.println("Object match found");
			return false;
		}
		
		Connection conn = client.openConnection();
		
		try {
			PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO "+tableName+" (objectID, owner, type, status, yamlSource, instance, namespace, appID)"+
				" VALUES ( ?, ?, ?, ?, ?, ?, ?, ? )");

		
			statement.setString(1, SQLUtils.prepareTextNoQuote(object.getObjectID(),100));
			statement.setString(2, SQLUtils.prepareTextNoQuote(object.getOwner(),140));
			statement.setString(3, SQLUtils.prepareTextNoQuote(object.getType().name(),100));
			statement.setString(4, SQLUtils.prepareTextNoQuote(mapper.writeValueAsString(object.getStatus()),1000));
			statement.setString(5, object.getYamlSource());
			statement.setInt(6, object.getInstance());
			statement.setString(7, SQLUtils.prepareTextNoQuote(object.getNamespace(),140));
			statement.setString(8, SQLUtils.prepareTextNoQuote(object.getAppID(),100));
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
	 * Returns a previously stored BigDataStack Application with a particular appID. 
	 * @return
	 * @throws SQLException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public BigDataStackObjectDefinition getObject(String objectID, String owner, int instance) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		Statement statement = conn.createStatement();
		statement.execute("SELECT DISTINCT * FROM "+tableName+" WHERE objectID='"+objectID+"' AND owner='"+owner+"' AND instance='"+instance+"'");
		ResultSet results = statement.getResultSet();
		
		BigDataStackObjectDefinition object = null;
		
		 try {
			while (results.next()) {

				//System.err.println(results.getString("status"));
				
				@SuppressWarnings("unchecked")
				Set<String> status = mapper.readValue(results.getString("status"), Set.class);
			
				object = new BigDataStackObjectDefinition(
						 results.getString("objectID"),
						 results.getString("owner"),
						 BigDataStackObjectType.valueOf(results.getString("type")),
						 results.getString("yamlSource"),
						 status,
						 results.getInt("instance"),
						 results.getString("namespace"),
						 results.getString("appID"));

			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return object;
	}
	
	/**
	 * Returns a set of previously stored BigDataStack Objects. 
	 * @return
	 * @throws SQLException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public List<BigDataStackObjectDefinition> getObjects(String objectID, String owner, String namespace, String appID) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT DISTINCT * FROM "+tableName+" WHERE owner='"+owner+"'");
		if (namespace!=null) query.append(" AND namespace='"+namespace+"'");
		if (appID!=null) query.append(" AND appID='"+appID+"'");
		if (objectID!=null) query.append(" AND objectID='"+objectID+"'");
		
		Statement statement = conn.createStatement();
		statement.execute(query.toString());
		ResultSet results = statement.getResultSet();
		
		List<BigDataStackObjectDefinition> objectList = new ArrayList<BigDataStackObjectDefinition>(3);
		
		 try {
			while (results.next()) {

				//System.err.println(results.getString("status"));
				
				@SuppressWarnings("unchecked")
				Set<String> status = mapper.readValue(results.getString("status"), Set.class);
			
				BigDataStackObjectDefinition object = new BigDataStackObjectDefinition(
						 results.getString("objectID"),
						 results.getString("owner"),
						 BigDataStackObjectType.valueOf(results.getString("type")),
						 results.getString("yamlSource"),
						 status,
						 results.getInt("instance"),
						 results.getString("namespace"),
						 results.getString("appID"));
				
				objectList.add(object);

			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return objectList;
	}
	
	/**
	 * Gets all objects stored for an owner, namespace and appID
	 * @return
	 * @throws SQLException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public List<BigDataStackObjectDefinition> getObjectList(String owner, String namespace, String appID, BigDataStackObjectType type) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("SELECT DISTINCT * FROM "+tableName+" WHERE owner='"+owner+"'");
		if (namespace!=null) queryBuilder.append(" AND namespace='"+namespace+"'");
		if (appID!=null) queryBuilder.append(" AND appID='"+appID+"'");
		if (type!=null) queryBuilder.append(" AND type='"+type.name()+"'");
		Statement statement = conn.createStatement();
		statement.execute(queryBuilder.toString());
		ResultSet results = statement.getResultSet();
		
		List<BigDataStackObjectDefinition> objectList = new ArrayList<BigDataStackObjectDefinition>(10);
		
		 try {
			while (results.next()) {

				//System.err.println(results.getString("status"));
				
				@SuppressWarnings("unchecked")
				Set<String> status = mapper.readValue(results.getString("status"), Set.class);
			
				BigDataStackObjectDefinition object = new BigDataStackObjectDefinition(
						 results.getString("objectID"),
						 results.getString("owner"),
						 BigDataStackObjectType.valueOf(results.getString("type")),
						 results.getString("yamlSource"),
						 status,
						 results.getInt("instance"),
						 results.getString("namespace"),
						 results.getString("appID"));
				
				objectList.add(object);

			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return objectList;
	}
	
	
	/**
	 * Returns a previously stored BigDataStack Application with a particular appID (first instance, or template). 
	 * @return
	 * @throws SQLException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public BigDataStackObjectDefinition getObject(String objectID, String owner) throws SQLException {
		return getObject(objectID, owner, 0);
	}
	
	/**
	 * Update the data for an existing BigDataStack application. This cannot change the appID.
	 * @param app
	 * @return
	 * @throws SQLException
	 */
	public boolean updateObject(BigDataStackObjectDefinition object) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		if (object.getYamlSource().length()>=65535) return false;
		
		Connection conn = client.openConnection();
		
		try {
			
			PreparedStatement statement = conn.prepareStatement("UPDATE "+tableName+" SET type=?, status=?, yamlSource=?, namespace=?, appID=?"+
					" WHERE owner="+SQLUtils.prepareText(object.getOwner(),140)+
					" AND objectID="+SQLUtils.prepareText(object.getObjectID(),100)+
					" AND instance="+object.getInstance());
			
			statement.setNString(1, SQLUtils.prepareTextNoQuote(object.getType().name(),100));
			statement.setNString(2, SQLUtils.prepareTextNoQuote(mapper.writeValueAsString(object.getStatus()),1000));
			statement.setNString(3, object.getYamlSource());
			statement.setNString(4, SQLUtils.prepareTextNoQuote(object.getNamespace(),140));
			statement.setNString(5, SQLUtils.prepareTextNoQuote(object.getAppID(),100));
			
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
	 * Returns the number of instances of an object. 
	 * @param objectID
	 * @param owner
	 * @return
	 * @throws SQLException
	 */
	public int getObjectCount(String objectID, String owner) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();

		Statement statement = conn.createStatement();

		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT COUNT(*) FROM "+tableName+" WHERE objectID='"+objectID+"' AND owner='"+owner+"'");

		int count = 0;
		
		try {
			statement.execute(baseStatement.toString());
			ResultSet results = statement.getResultSet();

			while (results.next()) {

				count = results.getInt(1);


			}
		} catch (Exception e) {
			//e.printStackTrace();
		} 

		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return count;
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
