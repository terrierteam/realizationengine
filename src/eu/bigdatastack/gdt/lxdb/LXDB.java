package eu.bigdatastack.gdt.lxdb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LXDB {

	protected final String driver = "com.leanxcale.client.Driver"; 
	
	String host;
	int port;
	String databasename;
	String username;
	String password = null;
	
	public LXDB(String host, int port, String databasename, String username, String password) {
		super();
		this.host = host;
		this.port = port;
		this.databasename = databasename;
		this.username = username;
		this.password = password;
		
		
		try {
			Class.forName("com.leanxcale.client.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	
	public LXDB(String host, int port, String databasename, String username) {
		super();
		this.host = host;
		this.port = port;
		this.databasename = databasename;
		this.username = username;
	}
	

	public Connection openConnection() throws SQLException {
		if (password==null) {
			return DriverManager.getConnection("jdbc:leanxcale://"+host+":"+port+"/"+databasename+";user="+username);
		} else {
			return DriverManager.getConnection("jdbc:leanxcale://"+host+":"+port+"/"+databasename, username, password);
		}
	}
	
}
