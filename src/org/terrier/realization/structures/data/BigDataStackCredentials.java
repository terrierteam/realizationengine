package org.terrier.realization.structures.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
