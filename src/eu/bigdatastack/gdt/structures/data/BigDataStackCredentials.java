package eu.bigdatastack.gdt.structures.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BigDataStackCredentials {

	String owner;
	String username;
	String password;
	Map<String,Long> tokens;
	BigDataStackCredentialsType type;
	
	public BigDataStackCredentials() {}
	
	public BigDataStackCredentials(String owner, String username, String password, BigDataStackCredentialsType type) {
		this.owner = owner;
		this.username = username;
		this.password = password;
		tokens = new HashMap<String,Long>();
		this.type = type;
	}

	public BigDataStackCredentials(String owner, String username, String password, Map<String, Long> tokens, BigDataStackCredentialsType type) {
		super();
		this.owner = owner;
		this.username = username;
		this.password = password;
		this.tokens = tokens;
		this.type = type;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Map<String, Long> getTokens() {
		return tokens;
	}

	public void setTokens(Map<String, Long> tokens) {
		this.tokens = tokens;
	}
	
	public BigDataStackCredentialsType getType() {
		return type;
	}

	public void setType(BigDataStackCredentialsType type) {
		this.type = type;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String generateToken(String password) {
		if (password.contentEquals(this.password)) {
			Random r = new Random();
			String token = String.valueOf(r.nextLong());
			tokens.put(token, System.currentTimeMillis()+(1000*60*60));
			return token;
		}
		return null;
	}
	
}
