package org.terrier.realization.state.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.terrier.realization.structures.Timed;
import org.terrier.realization.structures.data.BigDataStackEvent;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;

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
 * IO layer for reading and writing events from the Database
 *
 */
public class BigDataStackEventIO implements Timed {

	protected final String tableName = "BigDataStackEvents";

	JDBCDB client;
	long totalTime = 0;
	boolean init = false;
	public BigDataStackEventIO(JDBCDB client) throws SQLException {
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
					+ "owner VARCHAR(140), " + "eventNo INT, " + "objectID VARCHAR(100), " + "namespace VARCHAR(140), "
					+ "title VARCHAR(140), " + "description VARCHAR(1000), " + "eventTime BIGINT, "
					+ "type VARCHAR(100), " + "severity VARCHAR(100), " + "instance INT, " + "PRIMARY KEY (appID,owner,eventNo)" + ")");

			conn.commit();
		}
		totalTime+=System.currentTimeMillis()-startTime;
		conn.close();
	}

	/**
	 * Add a new BigDataStack event to the database. The event will only be added if
	 * it is unique.
	 * 
	 * @param app
	 * @return whether the insert was successful
	 * @throws SQLException
	 */
	public boolean addEvent(BigDataStackEvent event) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();

		Statement statement = conn.createStatement();
		try {
			statement.executeUpdate("INSERT INTO " + tableName
					+ " (appID, owner, eventNo, objectID, namespace, title, description, eventTime, type, severity, instance)"
					+ " VALUES ( " + SQLUtils.prepareText(event.getAppID(), 100) + ", "
					+ SQLUtils.prepareText(event.getOwner(), 140) + ", "
					+ event.getEventNo() + ", "
					+ SQLUtils.prepareText(event.getObjectID(), 100) + ", "
					+ SQLUtils.prepareText(event.getNamepace(), 140) + ", "
					+ SQLUtils.prepareText(event.getTitle(), 140) + ", "
					+ SQLUtils.prepareText(event.getDescription(), 1000) + ", "
					+ event.getEventTime() + ", "
					+ SQLUtils.prepareText(event.getType().name(), 100) + ", "
					+ SQLUtils.prepareText(event.getSeverity().name(), 100) + ", "
					+ event.getInstance() + " )");
		} catch (Exception e) {
			//e.printStackTrace();
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
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner, BigDataStackEventType type, BigDataStackEventSeverity severity, String objectID, int instance,  long startTime, long endTime) throws SQLException {
		if (!init) { initTable(); init=true;}
		long ostartTime = System.currentTimeMillis();
		Connection conn = client.openConnection();

		Statement statement = conn.createStatement();

		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT * FROM "+tableName+" WHERE owner='"+owner+"'");
		if (appID!=null) baseStatement.append(" AND appID='"+appID+"'");
		if (type!=null) baseStatement.append(" AND type='"+type.name()+"'");
		if (severity!=null) baseStatement.append(" AND severity='"+severity.name()+"'");
		if (objectID!=null) baseStatement.append(" AND objectID='"+objectID+"'");
		if (instance>=0) baseStatement.append(" AND instance='"+instance+"'");

		statement.execute(baseStatement.toString());
		ResultSet results = statement.getResultSet();

		List<BigDataStackEvent> retrievedEvents = new ArrayList<BigDataStackEvent>(30);

		try {
			while (results.next()) {

				BigDataStackEvent event = new BigDataStackEvent(
						results.getString("appID"),
						results.getString("owner"),
						results.getInt("eventNo"),
						results.getString("namespace"),
						results.getLong("eventTime"),
						BigDataStackEventType.valueOf(results.getString("type")),
						BigDataStackEventSeverity.valueOf(results.getString("severity")),
						results.getString("title"),
						results.getString("description"),
						results.getString("objectID"),
						results.getInt("instance")
						);


				if (startTime>=0) if (event.getEventTime()<startTime) continue;
				if (endTime>=0) if (event.getEventTime()>endTime) continue;

				retrievedEvents.add(event);


			}
		} catch (Exception e) {
			e.printStackTrace();
		} 

		Collections.sort(retrievedEvents);
		Collections.reverse(retrievedEvents);

		conn.close();
		totalTime+=System.currentTimeMillis()-ostartTime;
		return retrievedEvents;
	}

	/**
	 * Returns the number of events for an app. 
	 * @param appID
	 * @param owner
	 * @return
	 * @throws SQLException
	 */
	public int getEventCount(String appID, String owner) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();

		Statement statement = conn.createStatement();

		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT COUNT(*) FROM "+tableName+" WHERE appID='"+appID+"' AND owner='"+owner+"'");

		int count = 0;



		try {
			statement.execute(baseStatement.toString());
			ResultSet results = statement.getResultSet();

			while (results.next()) {

				count = results.getInt(1);


			}
		} catch (Exception e) {
			e.printStackTrace();
		} 

		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return count;
	}


	/**
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @param type
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner) throws SQLException {
		return getEvents(appID,owner,null, null, null, -1,-1,-1);
	}
	
	/**
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @param type
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner, int instance) throws SQLException {
		return getEvents(appID,owner,null, null, null, instance,-1,-1);
	}

	/**
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @param type
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner, BigDataStackEventType type) throws SQLException {
		return getEvents(appID,owner,type, null, null, -1,-1,-1);
	}

	/**
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @param severity
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner, BigDataStackEventSeverity severity) throws SQLException {
		return getEvents(appID,owner,null, severity, null, -1,-1,-1);
	}

	/**
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @param type
	 * @param severity
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner, BigDataStackEventType type, BigDataStackEventSeverity severity) throws SQLException {
		return getEvents(appID,owner,type, severity, null, -1,-1,-1);
	}

	/**
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @param type
	 * @param objectID
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner, BigDataStackEventType type, String objectID) throws SQLException {
		return getEvents(appID,owner,type, null, objectID, -1,-1,-1);
	}

	/**
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @param severity
	 * @param objectID
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner, BigDataStackEventSeverity severity, String objectID) throws SQLException {
		return getEvents(appID,owner,null, severity, objectID, -1,-1,-1);
	}

	/**
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @param type
	 * @param severity
	 * @param objectID
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner, BigDataStackEventType type, BigDataStackEventSeverity severity, String objectID) throws SQLException {
		return getEvents(appID,owner,type, severity, objectID, -1,-1,-1);
	}

	/**
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @param type
	 * @param startTime
	 * @param endTime
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner, BigDataStackEventType type, long startTime, long endTime) throws SQLException {
		return getEvents(appID,owner,type, null, null, -1,startTime,endTime);
	}

	/**
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @param severity
	 * @param startTime
	 * @param endTime
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner, BigDataStackEventSeverity severity, long startTime, long endTime) throws SQLException {
		return getEvents(appID,owner,null, severity, null, -1,startTime,endTime);
	}

	/**
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @param type
	 * @param severity
	 * @param startTime
	 * @param endTime
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner, BigDataStackEventType type, BigDataStackEventSeverity severity, long startTime, long endTime) throws SQLException {
		return getEvents(appID,owner,type, severity, null, -1,startTime,endTime);
	}

	/**
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @param type
	 * @param objectID
	 * @param startTime
	 * @param endTime
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner, BigDataStackEventType type, String objectID, long startTime, long endTime) throws SQLException {
		return getEvents(appID,owner,type, null, objectID, -1,startTime,endTime);
	}

	/**
	 * Returns all events in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @param severity
	 * @param objectID
	 * @param startTime
	 * @param endTime
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackEvent> getEvents(String appID, String owner, BigDataStackEventSeverity severity, String objectID, long startTime, long endTime) throws SQLException {
		return getEvents(appID,owner,null, severity, objectID, -1,startTime,endTime);
	}
	
	public boolean delete(String owner, String namespace, String appID, String objectID) {

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
