package eu.bigdatastack.gdt.lxdb;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import eu.bigdatastack.gdt.structures.Timed;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackSLO;

public class BigDataStackSLOIO implements Timed {

	protected final String tableName = "BigDataStackSLO";

	JDBCDB client;
	long totalTime = 0;
	boolean init = false;
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
			statement.executeUpdate("CREATE TABLE " + tableName + " ( " + "appID VARCHAR(100), "
					+ "owner VARCHAR(140), " + "metricName VARCHAR(140), " + "objectID VARCHAR(100), " + "namespace VARCHAR(140), " + "instance INT, "
					+ "sloIndex INT, " + "type VARCHAR(100), " + "value DOUBLE, " + "breachSeverity VARCHAR(100), " + "isRequirement BOOLEAN, "
					+ "PRIMARY KEY (owner,appID,objectID,instance,metricName,sloIndex)" + ")");

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

		Statement statement = conn.createStatement();
		try {
			statement.executeUpdate("INSERT INTO " + tableName
					+ " (appID, owner, objectID, namespace, instance, metricName, sloIndex, type, value, breachSeverity, isRequirement)"
					+ " VALUES ( " + SQLUtils.prepareText(slo.getAppID(), 100) + ", "
					+ SQLUtils.prepareText(slo.getOwner(), 140) + ", "
					+ SQLUtils.prepareText(slo.getObjectID(), 100) + ", "
					+ SQLUtils.prepareText(slo.getNamespace(), 140) + ", "
					+ slo.getInstance() + ", "
					+ SQLUtils.prepareText(slo.getMetricName(), 140) + ", "
					+ slo.getSloIndex() + ", "
					+ SQLUtils.prepareText(slo.getType(), 100) + ", "
					+ slo.getValue() + ", "
					+ SQLUtils.prepareText(slo.getBreachSeverity().name(),100) + ", "
					+ slo.isRequirement() + " )");
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
	public BigDataStackSLO getSLO(String owner, String appID, String objectID, int instance, String metricName, int sloIndex) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		Statement statement = conn.createStatement();
		
		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT * FROM "+tableName+" WHERE owner='"+owner+"'");
		if (appID!=null) baseStatement.append(" AND appID='"+appID+"'");
		if (objectID!=null) baseStatement.append(" AND objectID='"+objectID+"'");
		if (instance>=0) baseStatement.append(" AND instance="+instance+"");
		if (metricName!=null) baseStatement.append(" AND metricName='"+metricName+"'");
		if (sloIndex>=0) baseStatement.append(" AND sloIndex="+sloIndex+"");
		
		statement.execute(baseStatement.toString());
		ResultSet results = statement.getResultSet();
		BigDataStackSLO slo = null;
		
		 try {
			while (results.next()) {
				 
				slo = new BigDataStackSLO(
						results.getString("appID"),
						results.getString("owner"),
						results.getString("namespace"),
						results.getString("objectID"),
						results.getInt("instance"),
						results.getString("metricName"),
						results.getInt("sloIndex"),
						results.getString("type"),
						results.getDouble("value"),
						BigDataStackEventSeverity.valueOf(results.getString("breachSeverity")),
						results.getBoolean("isRequirement")
						);

			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		totalTime+=System.currentTimeMillis()-startTime;
		return slo;
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
	public List<BigDataStackSLO> getSLOs(String owner, String appID, String objectID, String namespace, int instance, String metricName, int sloIndex) throws SQLException {
		if (!init) { initTable(); init=true;}
		long startTime = System.currentTimeMillis();
		Connection conn = client.openConnection();
		
		Statement statement = conn.createStatement();
		
		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT * FROM "+tableName+" WHERE owner='"+owner+"'");
		if (appID!=null) baseStatement.append(" AND appID='"+appID+"'");
		if (objectID!=null) baseStatement.append(" AND objectID='"+objectID+"'");
		if (namespace!=null) baseStatement.append(" AND namespace='"+namespace+"'");
		if (instance>=0) baseStatement.append(" AND instance="+instance+"");
		if (metricName!=null) baseStatement.append(" AND metricName='"+metricName+"'");
		if (sloIndex>=0) baseStatement.append(" AND sloIndex="+sloIndex+"");
		
		statement.execute(baseStatement.toString());
		ResultSet results = statement.getResultSet();
		List<BigDataStackSLO> slos = new ArrayList<BigDataStackSLO>(5);
		
		 try {
			while (results.next()) {
				 
				BigDataStackSLO slo = new BigDataStackSLO(
						results.getString("appID"),
						results.getString("owner"),
						results.getString("namespace"),
						results.getString("objectID"),
						results.getInt("instance"),
						results.getString("metricName"),
						results.getInt("sloIndex"),
						results.getString("type"),
						results.getDouble("value"),
						BigDataStackEventSeverity.valueOf(results.getString("breachSeverity")),
						results.getBoolean("isRequirement")
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
			PreparedStatement statement = conn.prepareStatement("UPDATE "+tableName+" SET type=?, value=?, breachSeverity=?, isRequirement=?"+
					" WHERE appID="+SQLUtils.prepareText(slo.getAppID(),100)+
					" AND objectID="+SQLUtils.prepareText(slo.getObjectID(),100)+
					" AND instance="+slo.getInstance()+
					" AND owner="+SQLUtils.prepareText(slo.getOwner(),140)+
					" AND metricName="+SQLUtils.prepareText(slo.getMetricName(),140)+
					" AND sloIndex="+slo.getSloIndex());
			
			statement.setNString(1, SQLUtils.prepareTextNoQuote(slo.getType(),100));
			statement.setDouble(2, slo.getValue());
			statement.setNString(3, SQLUtils.prepareTextNoQuote(slo.getBreachSeverity().name(),100));
			statement.setBoolean(4, slo.isRequirement());

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
