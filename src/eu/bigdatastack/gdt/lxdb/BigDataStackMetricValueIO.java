package eu.bigdatastack.gdt.lxdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bigdatastack.gdt.structures.Timed;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetricValue;

public class BigDataStackMetricValueIO implements Timed {

	protected final String tableName = "BigDataStackMetricValues";
	protected ObjectMapper mapper = new ObjectMapper();

	LXDB client;
	long totalTime = 0;

	public BigDataStackMetricValueIO(LXDB client) throws SQLException {
		this.client = client;

		initTable();
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
					"namespace VARCHAR(140), "+
					"appID VARCHAR(100), "+
					"objectID VARCHAR(100), "+
					"metricName VARCHAR(140), "+
					"valueString VARCHAR(5000), "+
					"lastUpdated VARCHAR(5000), "+
					"labels VARCHAR(10000), "+
					"PRIMARY KEY (appID,owner,namespace,objectID,metricName)"+
					")");
			

			conn.commit();
		}
		totalTime+=System.currentTimeMillis()-startTime;
		conn.close();
	}

	/**
	 * Add a new BigDataStack Metric Value tracker to the database.
	 * @param app
	 * @return whether the insert was successful
	 * @throws SQLException
	 */
	public boolean addMetricValue(BigDataStackMetricValue metricValue) throws Exception {
		long startTime = System.currentTimeMillis();
		
		String valuesAsJson = mapper.writeValueAsString(metricValue.getValue());
		String timesAsJson = mapper.writeValueAsString(metricValue.getLastUpdated());
		String labelsAsJson = mapper.writeValueAsString(metricValue.getLabels());
		
		if (valuesAsJson.length()>=5000) return false;
		if (timesAsJson.length()>=5000) return false;
		if (labelsAsJson.length()>=10000) return false;
		
		Connection conn = client.openConnection();

		try {
			
			PreparedStatement statement = conn.prepareStatement(
					"INSERT INTO "+tableName+" (owner, namespace, appID, objectID, metricName, valueString, lastUpdated, labels)"+
					" VALUES ( ?, ?, ?, ?, ?, ?, ?, ? )");

			
				statement.setString(1,SQLUtils.prepareTextNoQuote(metricValue.getOwner(),140));
				statement.setString(2,SQLUtils.prepareTextNoQuote(metricValue.getNamespace(),140));
				statement.setString(3,SQLUtils.prepareTextNoQuote(metricValue.getAppID(),100));
				statement.setString(4, SQLUtils.prepareTextNoQuote(metricValue.getObjectID(),100));
				statement.setString(5, SQLUtils.prepareTextNoQuote(metricValue.getMetricname(),140));
				statement.setString(6, valuesAsJson);
				statement.setString(7, timesAsJson);
				statement.setString(8, labelsAsJson);
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
	 * Returns a previously stored Metric Values. 
	 * @return
	 * @throws SQLException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public List<BigDataStackMetricValue> getMetricValues(String appID, String owner, String namepace, String objectID) throws SQLException {
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();

		Statement statement = conn.createStatement();
		StringBuilder statementBuild = new StringBuilder();
		statementBuild.append("SELECT DISTINCT * FROM "+tableName+" WHERE");
		statementBuild.append(" owner='"+owner+"'");
		if (appID!=null) statementBuild.append(" AND appID='"+appID+"'");
		if (namepace!=null) statementBuild.append(" AND namespace='"+namepace+"'");
		if (objectID!=null) statementBuild.append(" AND objectID='"+objectID+"'");
		statement.execute(statementBuild.toString());
		ResultSet results = statement.getResultSet();

		List<BigDataStackMetricValue> values = new ArrayList<BigDataStackMetricValue>();

		try {
			while (results.next()) {

				@SuppressWarnings("unchecked")
				List<Map<String,String>> labels = mapper.readValue(results.getString("labels"), List.class);
				@SuppressWarnings("unchecked")
				List<String> valueL = mapper.readValue(results.getString("valueString"), List.class);
				@SuppressWarnings("unchecked")
				List<Long> timeL = mapper.readValue(results.getString("lastUpdated"), List.class);

				BigDataStackMetricValue value = new BigDataStackMetricValue(
						results.getString("owner"),
						results.getString("namespace"),
						results.getString("appID"),
						results.getString("objectID"),
						results.getString("metricName"),
						valueL,
						timeL,
						labels);

				values.add(value);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 



		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return values;
	}

	/**
	 * Update an existing metric value (only alters 'value', 'lastUpdated' and 'labels')
	 * @param app
	 * @return
	 * @throws SQLException
	 */
	public boolean updateMetricValue(BigDataStackMetricValue metricValue) throws Exception {
		long startTime = System.currentTimeMillis();
		
		
		String valuesAsJson = mapper.writeValueAsString(metricValue.getValue());
		String timesAsJson = mapper.writeValueAsString(metricValue.getLastUpdated());
		String labelsAsJson = mapper.writeValueAsString(metricValue.getLabels());
		
		if (valuesAsJson.length()>=5000) return false;
		if (timesAsJson.length()>=5000) return false;
		if (labelsAsJson.length()>=10000) return false;
		
		Connection conn = client.openConnection();
		try {
			
			PreparedStatement statement = conn.prepareStatement("UPDATE "+tableName+" SET valueString=?, lastUpdated=?, labels=?"+
					" WHERE owner="+SQLUtils.prepareText(metricValue.getOwner(),140)+
					" AND namespace="+SQLUtils.prepareText(metricValue.getNamespace(),140)+
					" AND appID="+SQLUtils.prepareText(metricValue.getAppID(),100)+
					" AND objectID="+SQLUtils.prepareText(metricValue.getObjectID(),100));
			
			statement.setNString(1, valuesAsJson);
			statement.setNString(2, timesAsJson);
			statement.setNString(3, labelsAsJson);
			
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
	 * Deletes the table in the database and re-creates it
	 * @return
	 * @throws SQLException
	 */
	public boolean clearTable() throws SQLException {
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		try {
			Statement statement = conn.createStatement();
			statement.execute("DROP TABLE \""+client.username+"\".\""+tableName+"\"");

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
