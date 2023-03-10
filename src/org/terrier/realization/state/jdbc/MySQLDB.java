package org.terrier.realization.state.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

 /*
 * Realization Engine 
 * Webpage: https://github.com/terrierteam/realizationengine
 * Contact: richard.mccreadie@glasgow.ac.uk
 * University of Glasgow - School of Computing Science
 * http://www.gla.ac.uk/
 * 
 * The contents of this file are subject to the Apache License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 *
 * The Original Code is Copyright (C) to the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richard.mccreadie@glasgow.ac.uk> (original author)
 */

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
			Connection conn = DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+databasename+";user="+username);
			conn.setAutoCommit(false);
			return conn;
		} else {
			Connection conn = DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+databasename, username, password);
			conn.setAutoCommit(false);
			return conn;
		}
	}
	
	@Override
	public String getUsername() {
		return username;
	}
}
