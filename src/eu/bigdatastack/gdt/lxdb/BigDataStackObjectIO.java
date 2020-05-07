package eu.bigdatastack.gdt.lxdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectType;

public class BigDataStackObjectIO {

	protected String tableName = "BigDataStackObjectDefinitions";
	protected ObjectMapper mapper = new ObjectMapper();
	
	LXDB client;
	
	public BigDataStackObjectIO(LXDB client, boolean template) throws SQLException {
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
			"objectID VARCHAR(100), "+
			"owner VARCHAR(140), "+
			"type VARCHAR(100), "+
			"status VARCHAR(1000), "+
			"yamlSource VARCHAR(65535), "+
			"instance INT, "+
			"PRIMARY KEY (objectID,owner,instance)"+
			")");
			
			conn.commit();
		}
		
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
		
		if (object.getYamlSource().length()>=65535) return false;
		
		if (getObject(object.getObjectID(), object.getOwner(), object.getInstance())!=null) return false;
		
		Connection conn = client.openConnection();
		
		try {
			PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO "+tableName+" (objectID, owner, type, status, yamlSource, instance)"+
				" VALUES ( ?, ?, ?, ?, ?, ? )");

		
			statement.setString(1, SQLUtils.prepareTextNoQuote(object.getObjectID(),100));
			statement.setString(2, SQLUtils.prepareTextNoQuote(object.getOwner(),140));
			statement.setString(3, SQLUtils.prepareTextNoQuote(object.getType().name(),100));
			statement.setString(4, SQLUtils.prepareTextNoQuote(mapper.writeValueAsString(object.getStatus()),1000));
			statement.setString(5, object.getYamlSource());
			statement.setInt(6, object.getInstance());
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
	 * Returns a previously stored BigDataStack Application with a particular appID. 
	 * @return
	 * @throws SQLException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public BigDataStackObjectDefinition getObject(String objectID, String owner, int instance) throws SQLException {
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
						 results.getInt("instance"));

			 }
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		
		
		conn.close();
		
		return object;
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
		
		if (object.getYamlSource().length()>=65535) return false;
		
		Connection conn = client.openConnection();
		
		try {
			
			PreparedStatement statement = conn.prepareStatement("UPDATE "+tableName+" SET type=?, status=?, yamlSource=?"+
					" WHERE owner="+SQLUtils.prepareText(object.getOwner(),140)+
					" AND objectID="+SQLUtils.prepareText(object.getObjectID(),100)+
					" AND instance="+object.getInstance());
			
			statement.setNString(1, SQLUtils.prepareTextNoQuote(object.getType().name(),100));
			statement.setNString(2, SQLUtils.prepareTextNoQuote(mapper.writeValueAsString(object.getStatus()),1000));
			statement.setNString(3, object.getYamlSource());
			
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
	 * Returns the number of instances of an object. 
	 * @param objectID
	 * @param owner
	 * @return
	 * @throws SQLException
	 */
	public int getObjectCount(String objectID, String owner) throws SQLException {
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

		return count;
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
