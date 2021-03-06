package eu.bigdatastack.gdt.structures.config;

public class DatabaseConf {

	String type;
	String host;
	int port;
	String name;
	String username;
	String password;
	
	public DatabaseConf() {};
	
	public DatabaseConf(String type, String host, int port, String name, String username, String password) {
		super();
		this.type = type;
		this.host = host;
		this.port = port;
		this.name = name;
		this.username = username;
		this.password = password;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	
}
