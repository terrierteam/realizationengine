package org.terrier.realization.state.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.terrier.realization.structures.Timed;
import org.terrier.realization.structures.data.BigDataStackCredentials;
import org.terrier.realization.structures.data.BigDataStackCredentialsType;

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

public class BigDataStackCredentialsIO implements Timed {
	protected final String tableName = "BigDataStackCredentials";
	protected ObjectMapper mapper = new ObjectMapper();

	JDBCDB client;
	long totalTime = 0;
	boolean init = false;
	public BigDataStackCredentialsIO(JDBCDB client) throws SQLException {
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
					"owner VARCHAR(140), "+
					"username VARCHAR(140), "+
					"password VARCHAR(140), "+
					"tokens VARCHAR(1000), "+
					"type VARCHAR(100), "+
					"PRIMARY KEY (owner,type)"+
					")");

			conn.commit();
		}
		totalTime+=System.currentTimeMillis()-startTime;
		conn.close();
	}

	/**
	 * Add a new BigDataStack credentials to the database.
	 * @param app
	 * @return whether the insert was successful
	 * @throws SQLException
	 */
	public boolean addCredential(BigDataStackCredentials credential) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();

		try {
			PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO "+tableName+" (owner, username, password, tokens, type)"+
				" VALUES ( ?, ?, ?, ?, ? )");

		
			statement.setString(1, SQLUtils.prepareTextNoQuote(credential.getOwner(),140));
			statement.setString(2, SQLUtils.prepareTextNoQuote(credential.getUsername(),140));
			statement.setString(3, SQLUtils.prepareTextNoQuote(credential.getPassword(),140));
			statement.setString(4, SQLUtils.prepareTextNoQuote(mapper.writeValueAsString(credential.getTokens()),1000));
			statement.setString(5, SQLUtils.prepareTextNoQuote(credential.getType().name(),100));
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
	 * Returns a previously stored BigDataStack Credential for a service type. 
	 * @param owner
	 * @param type
	 * @return
	 * @throws SQLException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public BigDataStackCredentials getCredential(String owner, BigDataStackCredentialsType type) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();

		Statement statement = conn.createStatement();
		statement.execute("SELECT DISTINCT * FROM "+tableName+" WHERE owner='"+owner+"' AND type='"+type.name()+"'");
		ResultSet results = statement.getResultSet();

		BigDataStackCredentials credential = null;

		try {
			while (results.next()) {

				Map<String,Long> tokens = mapper.readValue(results.getString("tokens"), Map.class);

				credential = new BigDataStackCredentials(
						results.getString("owner"),
						results.getString("username"),
						results.getString("password"),
						tokens,
						BigDataStackCredentialsType.valueOf(results.getString("type"))
					);


			}
		} catch (Exception e) {
			e.printStackTrace();
		} 



		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return credential;
	}

	/**
	 * Mint a new connection token for a user.
	 * @param owner
	 * @param password
	 * @param type
	 * @return
	 * @throws SQLException
	 */
	public String getNewToken(String owner, String password, BigDataStackCredentialsType type) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		BigDataStackCredentials credentials = getCredential(owner, type);
		if (!credentials.getPassword().equals(password)) return null;
		
		String token = credentials.generateToken(password);
		
		Connection conn = client.openConnection();

		try {
			Statement statement = conn.createStatement();
			statement.executeUpdate("UPDATE "+tableName+" SET "+
					"tokens="+SQLUtils.prepareTextNoQuote(mapper.writeValueAsString(credentials.getTokens()),1000)+", "+
					" WHERE owner="+SQLUtils.prepareText(owner,140)+" AND type="+SQLUtils.prepareText(type.name(),100));
		} catch (Exception e) {
			e.printStackTrace();
			conn.close();
			totalTime+=System.currentTimeMillis()-startTime;
			return null;
		}



		conn.commit();
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return token;
	}
	
	/**
	 * Update credentials password.
	 * @param app
	 * @return
	 * @throws SQLException
	 */
	public boolean updatePassweord(String owner, BigDataStackCredentialsType type, String username, String password) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		BigDataStackCredentials credentials = getCredential(owner, type);
		if (!credentials.getPassword().equals(password) || !username.equals(credentials.getUsername())) return false;
		
		credentials.setUsername(username);
		credentials.setPassword(password);
		
		Connection conn = client.openConnection();

		try {
			Statement statement = conn.createStatement();
			statement.executeUpdate("UPDATE "+tableName+" SET "+
					"username="+ SQLUtils.prepareText(credentials.getUsername(),140)+", "+
					"password="+ SQLUtils.prepareText(credentials.getPassword(),140)+", "+
					"tokens="+SQLUtils.prepareText(mapper.writeValueAsString(credentials.getTokens()),1000)+
					" WHERE owner="+SQLUtils.prepareText(owner,140)+" AND type="+SQLUtils.prepareText(type.name(),100));
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
