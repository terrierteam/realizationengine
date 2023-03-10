package org.terrier.realization.structures.config;

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
public class OpenshiftConfig {

	String client;
	String host;
	int port;
	String username;
	String password;
	String hostExtension;
	String imageRepositoryHost;
	String namespace;
	String openshiftPrometheus;
	
	public OpenshiftConfig() {}

	public OpenshiftConfig(String client, String host, int port, String username, String password, String hostExtension,
			String imageRepositoryHost, String namespace) {
		super();
		this.client = client;
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.hostExtension = hostExtension;
		this.imageRepositoryHost = imageRepositoryHost;
		this.namespace = namespace;
		this.openshiftPrometheus = null;
	}
	
	

	public OpenshiftConfig(String client, String host, int port, String username, String password, String hostExtension,
			String imageRepositoryHost, String namespace, String openshiftPrometheus) {
		super();
		this.client = client;
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.hostExtension = hostExtension;
		this.imageRepositoryHost = imageRepositoryHost;
		this.namespace = namespace;
		this.openshiftPrometheus = openshiftPrometheus;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
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

	public String getHostExtension() {
		return hostExtension;
	}

	public void setHostExtension(String hostExtension) {
		this.hostExtension = hostExtension;
	}

	public String getImageRepositoryHost() {
		return imageRepositoryHost;
	}

	public void setImageRepositoryHost(String imageRepositoryHost) {
		this.imageRepositoryHost = imageRepositoryHost;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getOpenshiftPrometheus() {
		return openshiftPrometheus;
	}

	public void setOpenshiftPrometheus(String openshiftPrometheus) {
		this.openshiftPrometheus = openshiftPrometheus;
	}

	

	
	
	
	
}
