package eu.bigdatastack.gdt.lxdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplicationType;

/**
 * Provides IO capabilities for a BigDataStackApplication
 * @author EbonBlade
 *
 */
public class BigDataStackApplicationIO {

	protected final String tableName = "BigDataStackApplications";
	protected ObjectMapper mapper = new ObjectMapper();

	LXDB client;

	public BigDataStackApplicationIO(LXDB client) throws SQLException {
		this.client = client;

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
					"name VARCHAR(140), "+
					"description VARCHAR(65535), "+
					"owner VARCHAR(140), "+
					"namepace VARCHAR(140), "+
					"types VARCHAR(1000), "+
					"PRIMARY KEY (appID,owner)"+
					")");

			conn.commit();
		}

		conn.close();
	}

	/**
	 * Add a new BigDataStack application to the database. The application will only be added if the appID is unique.
	 * Use updateApp to change the content for an existing app
	 * @param app
	 * @return whether the insert was successful
	 * @throws SQLException
	 */
	public boolean addApplication(BigDataStackApplication app) throws SQLException {

		Connection conn = client.openConnection();

		Statement statement = conn.createStatement();
		try {
			statement.executeUpdate("INSERT INTO "+tableName+" (appID, name, description, owner, namepace, types)"+
					" VALUES ( "+
					SQLUtils.prepareText(app.getAppID(),100)+", "+
					SQLUtils.prepareText(app.getName(),140)+", "+
					SQLUtils.prepareText(app.getDescription(),65535)+", "+
					SQLUtils.prepareText(app.getOwner(),140)+", "+
					SQLUtils.prepareText(app.getNamespace(),140)+", "+
					SQLUtils.prepareText(mapper.writeValueAsString(app.getTypes()),1000)+
					" )");
		} catch (Exception e) {
			//e.printStackTrace();
			conn.close();
			return false;
		}

		conn.commit();
		conn.close();

		return true;
	}

	/**
	 * Returns a previously stored BigDataStack Application with a particular appID. 
	 * @param appID
	 * @return
	 * @throws SQLException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public BigDataStackApplication getApp(String appID, String owner) throws SQLException {
		Connection conn = client.openConnection();

		Statement statement = conn.createStatement();
		statement.execute("SELECT DISTINCT * FROM "+tableName+" WHERE appID='"+appID+"' AND owner='"+owner+"'");
		ResultSet results = statement.getResultSet();

		BigDataStackApplication app = null;

		try {
			while (results.next()) {

				List<String> enums = mapper.readValue(results.getString("types"), List.class);
				List<BigDataStackApplicationType> types = new ArrayList<BigDataStackApplicationType>(enums.size());
				for (String type : enums) types.add(BigDataStackApplicationType.valueOf(type));

				app = new BigDataStackApplication(
						results.getString("appID"),
						results.getString("name"),
						results.getString("description"),
						results.getString("owner"),
						results.getString("namepace"),
						types);


			}
		} catch (Exception e) {
			e.printStackTrace();
		} 



		conn.close();

		return app;
	}

	/**
	 * Update the data for an existing BigDataStack application. This cannot change the appID.
	 * @param app
	 * @return
	 * @throws SQLException
	 */
	public boolean updateApp(BigDataStackApplication app) throws SQLException {
		Connection conn = client.openConnection();

		try {
			Statement statement = conn.createStatement();
			statement.executeUpdate("UPDATE "+tableName+" SET "+
					"name="+SQLUtils.prepareText(app.getName(),140)+", "+
					"description="+SQLUtils.prepareText(app.getDescription(),65535)+", "+
					"namepace="+SQLUtils.prepareText(app.getNamespace(),140)+", "+
					"types="+SQLUtils.prepareText(mapper.writeValueAsString(app.getTypes()),1000)+
					" WHERE appID="+SQLUtils.prepareText(app.getAppID(),100)+" AND owner="+SQLUtils.prepareText(app.getOwner(),140));
		} catch (Exception e) {
			e.printStackTrace();
			conn.close();
			return false;
		}



		conn.commit();
		conn.close();

		return true;
	}

	/**
	 * Returns all applications. 
	 * @param owner
	 * @return
	 * @throws SQLException
	 */
	public List<BigDataStackApplication> getApplications(String owner) throws SQLException {
		Connection conn = client.openConnection();

		Statement statement = conn.createStatement();

		StringBuilder baseStatement = new StringBuilder();
		baseStatement.append("SELECT * FROM "+tableName+" WHERE owner='"+owner+"'");

		statement.execute(baseStatement.toString());
		ResultSet results = statement.getResultSet();

		List<BigDataStackApplication> retrievedApplications = new ArrayList<BigDataStackApplication>(5);

		try {
			while (results.next()) {

				@SuppressWarnings("unchecked")
				List<String> enums = mapper.readValue(results.getString("types"), List.class);
				List<BigDataStackApplicationType> types = new ArrayList<BigDataStackApplicationType>(enums.size());
				for (String type : enums) types.add(BigDataStackApplicationType.valueOf(type));

				BigDataStackApplication app = new BigDataStackApplication(
						results.getString("appID"),
						results.getString("name"),
						results.getString("description"),
						results.getString("owner"),
						results.getString("namepace"),
						types);

				retrievedApplications.add(app);


			}
		} catch (Exception e) {
			e.printStackTrace();
		} 

		conn.close();

		return retrievedApplications;
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
