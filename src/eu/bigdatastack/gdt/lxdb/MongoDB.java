package eu.bigdatastack.gdt.lxdb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MongoDB implements JDBCDB {
	protected final String driver = "com.mongodb.jdbc.MongoDriver"; 

	String host;
	int port;
	String databasename;
	String username;
	String password = null;

	public MongoDB(String host, int port, String databasename, String username, String password) {
		super();
		this.host = host;
		this.port = port;
		this.databasename = databasename;
		this.username = username;
		this.password = password;


		try {
			Class.forName("com.mongodb.jdbc.MongoDriver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}


	public MongoDB(String host, int port, String databasename, String username) {
		super();
		this.host = host;
		this.port = port;
		this.databasename = databasename;
		this.username = username;
	}


	public Connection openConnection() throws SQLException {
		if (password==null) {
			return DriverManager.getConnection("jdbc:mongodb://"+host+":"+port+"/"+databasename+";user="+username);
		} else {
			return DriverManager.getConnection("jdbc:mongodb://"+host+":"+port+"/"+databasename, username, password);
		}
	}
	
	@Override
	public String getUsername() {
		return username;
	}
}
