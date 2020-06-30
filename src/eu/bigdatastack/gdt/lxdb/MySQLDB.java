package eu.bigdatastack.gdt.lxdb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLDB implements JDBCDB {
	protected final String driver = "com.mysql.cj.jdbc.Driver"; 

	String host;
	int port;
	String databasename;
	String username;
	String password = null;

	public MySQLDB(String host, int port, String databasename, String username, String password) {
		super();
		this.host = host;
		this.port = port;
		this.databasename = databasename;
		this.username = username;
		this.password = password;


		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}


	public MySQLDB(String host, int port, String databasename, String username) {
		super();
		this.host = host;
		this.port = port;
		this.databasename = databasename;
		this.username = username;
	}


	public Connection openConnection() throws SQLException {
		if (password==null) {
			return DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+databasename+";user="+username);
		} else {
			return DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+databasename, username, password);
		}
	}
	
	@Override
	public String getUsername() {
		return username;
	}
}
