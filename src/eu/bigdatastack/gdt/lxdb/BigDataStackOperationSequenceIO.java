package eu.bigdatastack.gdt.lxdb;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bigdatastack.gdt.operations.BigDataStackOperation;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequenceMode;

/**
 * This class is responsible for writing data about operation sequences for a user's application
 * into the database. Note, these sequences have status information associated to them, so may be
 * updated semi-frequently. 
 * @author EbonBlade
 *
 */
public class BigDataStackOperationSequenceIO {

	protected String tableName = "BigDataStackOperationSequences";
	protected ObjectMapper mapper = new ObjectMapper();
	
	LXDB client;
	
	public BigDataStackOperationSequenceIO(LXDB client, boolean template) throws SQLException {
		this.client = client;
		
		if (template) tableName=tableName+"Templates";
		
		initTable();
	}
	
	/**
	 * Check whether the table exists in the DB already and if not creates it
	 * @throws SQLException
	 */
	public void initTable() throws SQLException {
		
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
			"jsonOperations VARCHAR(65535), "+
			"mode VARCHAR(100), "+
			"PRIMARY KEY (appID,sequenceID,instance)"+
			")");
			
			conn.commit();
		}
		
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
		
		String operationsAsJson = mapper.writeValueAsString(sequence.getOperations());
		
		if (operationsAsJson.length()>=65535) return false;
		
		if (getSequence(sequence.getAppID(), sequence.getSequenceID(), sequence.getIndex())!=null) return false;
		
		Connection conn = client.openConnection();
		
		try {
			PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO "+tableName+" (appID, owner, namespace, instance, sequenceID, name, description, jsonOperations, mode)"+
				" VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ? )");

		
			statement.setString(1, SQLUtils.prepareTextNoQuote(sequence.getAppID(),100));
			statement.setString(2, SQLUtils.prepareTextNoQuote(sequence.getOwner(),140));
			statement.setString(3, SQLUtils.prepareTextNoQuote(sequence.getNamepace(),140));
			statement.setInt(4, sequence.getIndex());
			statement.setString(5, SQLUtils.prepareTextNoQuote(sequence.getSequenceID(),100));
			statement.setString(6, SQLUtils.prepareTextNoQuote(sequence.getName(),140));
			statement.setString(7, SQLUtils.prepareTextNoQuote(sequence.getDescription(),1000));
			statement.setString(8, operationsAsJson);
			statement.setString(9, SQLUtils.prepareTextNoQuote(sequence.getMode().name(),100));
			statement.executeUpdate();
			
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
			conn.close();
			return false;
		} 
		
		
		conn.close();

		return true;
		
	}
	
	/**
	 * Returns a previously stored BigDataStack Operation Sequence. 
	 * @return
	 * @throws SQLException
	 */
	public BigDataStackOperationSequence getSequence(String appID, String sequenceID, int instance) throws SQLException {
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
				
				
				sequence = new BigDataStackOperationSequence(
						results.getString("appID"),
						results.getString("owner"),
						results.getString("namespace"),
						results.getInt("instance"),
						results.getString("sequenceID"),
						results.getString("name"),
						results.getString("description"),
						operations,
						BigDataStackOperationSequenceMode.valueOf(results.getString("mode"))
					);
				
			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		
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
		
		String operationsAsJson = mapper.writeValueAsString(sequence.getOperations());
		
		if (operationsAsJson.length()>=65535) return false;
		
		Connection conn = client.openConnection();
		
		try {
			
			PreparedStatement statement = conn.prepareStatement("UPDATE "+tableName+" SET name=?, description=?, jsonOperations=?, mode=?"+
					" WHERE appID="+SQLUtils.prepareText(sequence.getAppID(),100)+
					" AND owner="+SQLUtils.prepareText(sequence.getOwner(),140)+
					" AND namespace="+SQLUtils.prepareText(sequence.getNamepace(),140)+
					" AND sequenceID="+SQLUtils.prepareText(sequence.getSequenceID(),100)+
					" AND instance="+sequence.getIndex());
			
			statement.setNString(1, sequence.getName());
			statement.setNString(2, sequence.getDescription());
			statement.setNString(3, operationsAsJson);
			statement.setNString(4, sequence.getMode().name());
			
			statement.executeUpdate();
			conn.commit();
			
		} catch (Exception e) {
			e.printStackTrace();
			conn.close();
			return false;
		}
		
		 
		
		
		conn.close();
		
		return true;
	}
	
	
	
	/**
	 * Returns all operation sequences in reverse chronological order for a particular application. 
	 * @param appID
	 * @param owner
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackOperationSequence> getOperationSequences(String appID) throws SQLException {
		Connection conn = client.openConnection();
		
		Statement statement = conn.createStatement();
		
		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT * FROM "+tableName+" WHERE appID='"+appID+"'");
		
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
				
				BigDataStackOperationSequence sequence = new BigDataStackOperationSequence(
						results.getString("appID"),
						results.getString("owner"),
						results.getString("namespace"),
						results.getInt("instance"),
						results.getString("sequenceID"),
						results.getString("name"),
						results.getString("description"),
						operations,
						BigDataStackOperationSequenceMode.valueOf(results.getString("mode"))
					);
				 
				retrievedSequences.add(sequence);
				 
				 
			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		
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
				
				BigDataStackOperationSequence sequence = new BigDataStackOperationSequence(
						results.getString("appID"),
						results.getString("owner"),
						results.getString("namespace"),
						results.getInt("instance"),
						results.getString("sequenceID"),
						results.getString("name"),
						results.getString("description"),
						operations,
						BigDataStackOperationSequenceMode.valueOf(results.getString("mode"))
					);
				 
				retrievedSequences.add(sequence);
				 
				 
			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		
		return retrievedSequences;
	}
	
	/**
	 * Deletes the table in the database and re-creates it
	 * @return
	 * @throws SQLException
	 */
	public boolean clearTable() throws SQLException {
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
			return false;
		}
		
		return true;
	}
	
}
