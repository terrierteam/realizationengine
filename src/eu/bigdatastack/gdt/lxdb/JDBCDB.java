package eu.bigdatastack.gdt.lxdb;

import java.sql.Connection;
import java.sql.SQLException;

public interface JDBCDB {

	/**
	 * Opens a JDBC connection to the database
	 * @return
	 * @throws SQLException
	 */
	public Connection openConnection() throws SQLException;
	
	/**
	 * Gets the name of the current user
	 * @return
	 */
	public String getUsername();
}
