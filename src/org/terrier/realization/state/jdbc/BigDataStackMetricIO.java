package org.terrier.realization.state.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.terrier.realization.structures.Timed;
import org.terrier.realization.structures.data.BigDataStackMetric;
import org.terrier.realization.structures.data.BigDataStackMetricAggregation;
import org.terrier.realization.structures.data.BigDataStackMetricSource;

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

public class BigDataStackMetricIO implements Timed {

	protected final String tableName = "BigDataStackMetrics";
	protected ObjectMapper mapper = new ObjectMapper();
	protected DecimalFormat formater = new DecimalFormat("#.####");
	
	JDBCDB client;
	long totalTime = 0;
	boolean init = false;
	public BigDataStackMetricIO(JDBCDB client) throws SQLException {
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
			"name VARCHAR(140), "+
			"summary VARCHAR(3000), "+
			"displayUnit VARCHAR(100), "+
			"maximumValue VARCHAR(40), "+
			"minimumValue VARCHAR(40), "+
			"higherIsBetter BOOLEAN, "+
			"source VARCHAR(40), "+
			"aggregation VARCHAR(40), "+
			"PRIMARY KEY (name,owner)"+
			")");
			
			conn.commit();
		}
		totalTime+=System.currentTimeMillis()-startTime;
		conn.close();
	}

	/**
	 * Add a new BigDataStack metric to the database.
	 * @param app
	 * @return whether the insert was successful
	 * @throws SQLException
	 */
	public boolean addMetric(BigDataStackMetric metric) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		Statement statement = conn.createStatement();
		try {
			statement.executeUpdate("INSERT INTO "+tableName+" (owner, name, summary, displayUnit, maximumValue, minimumValue, higherIsBetter, source, aggregation)"+
			" VALUES ( "+
			SQLUtils.prepareText(metric.getOwner(),140)+", "+
			SQLUtils.prepareText(metric.getName(),140)+", "+
			SQLUtils.prepareText(metric.getSummary(),3000)+", "+
			SQLUtils.prepareText(metric.getDisplayUnit(),100)+", "+
			formater.format(metric.getMaximumValue())+", "+
			formater.format(metric.getMinimumValue())+", "+
			metric.isHigherIsBetter()+", "+
			SQLUtils.prepareText(metric.getSource().name(),40)+", "+
			SQLUtils.prepareText(metric.getAggregation().name(),40)+
			" )");
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
	 * Returns a previously stored BigDataStack Metric with registered with a user. 
	 * @param owner
	 * @param metricName
	 * @return
	 * @throws SQLException
	 */
	public BigDataStackMetric getMetric(String owner, String metricName) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("SELECT DISTINCT * FROM "+tableName+" WHERE owner='"+owner+"'");
		if (metricName!=null) queryBuilder.append(" AND name='"+metricName+"'");
		
		Statement statement = conn.createStatement();
		statement.execute(queryBuilder.toString());
		ResultSet results = statement.getResultSet();
		
		BigDataStackMetric metric = null;
		
		 try {
			while (results.next()) {

				 
				 metric = new BigDataStackMetric(
						 results.getString("owner"),
						 results.getString("name"),
						 results.getString("summary"),
						 results.getString("displayUnit"),
						 Double.parseDouble(results.getString("maximumValue")),
						 Double.parseDouble(results.getString("minimumValue")),
						 results.getBoolean("higherIsBetter"),
						 BigDataStackMetricSource.valueOf(results.getString("source")),
						 BigDataStackMetricAggregation.valueOf(results.getString("aggregation"))
						);
				 
				 
			 }
		} catch (Exception e) {
			e.printStackTrace();
			conn.close();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return metric;
	}
	
	/**
	 * Returns a previously list of previously stored BigDataStack Metrics with registered with a user. 
	 * @param owner
	 * @param metricName
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackMetric> listMetrics(String owner, String metricName) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		StringBuilder queryBuilder = new StringBuilder();
		if (owner==null) {
			queryBuilder.append("SELECT DISTINCT * FROM "+tableName);
		} else {
			queryBuilder.append("SELECT DISTINCT * FROM "+tableName+" WHERE owner='"+owner+"'");
			if (metricName!=null) queryBuilder.append(" AND name='"+metricName+"'");
		}
		 
		
		
		Statement statement = conn.createStatement();
		statement.execute(queryBuilder.toString());
		ResultSet results = statement.getResultSet();
		
		List<BigDataStackMetric> metrics = new ArrayList<BigDataStackMetric>();
		
		 try {
			while (results.next()) {

				 
				BigDataStackMetric metric = new BigDataStackMetric(
						results.getString("owner"),
						 results.getString("name"),
						 results.getString("summary"),
						 results.getString("displayUnit"),
						 Double.parseDouble(results.getString("maximumValue")),
						 Double.parseDouble(results.getString("minimumValue")),
						 results.getBoolean("higherIsBetter"),
						 BigDataStackMetricSource.valueOf(results.getString("source")),
						 BigDataStackMetricAggregation.valueOf(results.getString("aggregation"))
						 );
				 
				metrics.add(metric);
			 }
		} catch (Exception e) {
			e.printStackTrace();
			conn.close();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return metrics;
	}
	
	/**
	 * Update the data for an existing BigDataStack metric.
	 * @param metric
	 * @return
	 * @throws SQLException
	 */
	public boolean updateMetric(BigDataStackMetric metric) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		try {
			Statement statement = conn.createStatement();
			statement.executeUpdate("UPDATE "+tableName+" SET "+
					"source="+SQLUtils.prepareText(metric.getSource().name(),40)+", "+
					"aggregation="+SQLUtils.prepareText(metric.getAggregation().name(),40)+", "+
					"summary="+SQLUtils.prepareText(metric.getSummary(),3000)+", "+
					"maximumValue='"+formater.format(metric.getMaximumValue())+"', "+
					"minimumValue='"+formater.format(metric.getMinimumValue())+"', "+
					"higherIsBetter="+metric.isHigherIsBetter()+", "+
					"displayUnit="+SQLUtils.prepareText(metric.getDisplayUnit(),100)+
					" WHERE name="+SQLUtils.prepareText(metric.getName(),140)+" AND owner="+SQLUtils.prepareText(metric.getOwner(),140));
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
	
	public boolean delete(String owner, String name) {

		try {
			if (!init) { initTable(); init=true;}
			long startTime = System.currentTimeMillis();
			Connection conn = client.openConnection();

			Statement statement = conn.createStatement();

			StringBuilder baseStatement = new StringBuilder();
			baseStatement.append("DELETE FROM "+tableName+" WHERE owner='"+owner+"'");
			if (name!=null) baseStatement.append(" AND name='"+name+"'");
			

			
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
