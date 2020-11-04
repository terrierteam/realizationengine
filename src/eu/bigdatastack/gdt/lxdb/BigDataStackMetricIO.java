package eu.bigdatastack.gdt.lxdb;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bigdatastack.gdt.structures.Timed;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetric;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetricClassname;

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
			"metricClassname VARCHAR(20), "+
			"summary VARCHAR(3000), "+
			"maximumValue VARCHAR(40), "+
			"minimumValue VARCHAR(40), "+
			"higherIsBetter BOOLEAN, "+
			"displayUnit VARCHAR(100), "+
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
			statement.executeUpdate("INSERT INTO "+tableName+" (owner, name, metricClassname, summary, maximumValue, minimumValue, higherIsBetter, displayUnit)"+
			" VALUES ( "+
			SQLUtils.prepareText(metric.getOwner(),140)+", "+
			SQLUtils.prepareText(metric.getName(),140)+", "+
			SQLUtils.prepareText(metric.getMetricClassname().name(),20)+", "+
			SQLUtils.prepareText(metric.getSummary(),3000)+", "+
			formater.format(metric.getMaximumValue())+", "+
			formater.format(metric.getMinimumValue())+", "+
			metric.isHigherIsBetter()+", "+
			SQLUtils.prepareText(metric.getDisplayUnit(),100)+
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
		if (metricName!=null) queryBuilder.append(" AND metricName='"+metricName+"'");
		
		Statement statement = conn.createStatement();
		statement.execute(queryBuilder.toString());
		ResultSet results = statement.getResultSet();
		
		BigDataStackMetric metric = null;
		
		 try {
			while (results.next()) {

				 
				 metric = new BigDataStackMetric(
						 results.getString("owner"),
						 results.getString("name"),
						 BigDataStackMetricClassname.valueOf(results.getString("metricClassname")),
						 results.getString("summary"),
						 Double.parseDouble(results.getString("maximumValue")),
						 Double.parseDouble(results.getString("minimumValue")),
						 results.getBoolean("higherIsBetter"),
						 results.getString("displayUnit"));
				 
				 
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
		queryBuilder.append("SELECT DISTINCT * FROM "+tableName+" WHERE owner='"+owner+"'");
		if (metricName!=null) queryBuilder.append(" AND metricName='"+metricName+"'");
		
		Statement statement = conn.createStatement();
		statement.execute(queryBuilder.toString());
		ResultSet results = statement.getResultSet();
		
		List<BigDataStackMetric> metrics = new ArrayList<BigDataStackMetric>();
		
		 try {
			while (results.next()) {

				 
				BigDataStackMetric metric = new BigDataStackMetric(
						 results.getString("owner"),
						 results.getString("name"),
						 BigDataStackMetricClassname.valueOf(results.getString("metricClassname")),
						 results.getString("summary"),
						 Double.parseDouble(results.getString("maximumValue")),
						 Double.parseDouble(results.getString("minimumValue")),
						 results.getBoolean("higherIsBetter"),
						 results.getString("displayUnit"));
				 
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
					"metricClassname="+SQLUtils.prepareText(metric.getMetricClassname().name(),20)+", "+
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
