package org.terrier.realization.rabbitmq;

import org.terrier.realization.structures.data.BigDataStackEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

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

public class RabbitMQClient {

	String rabbitMQExchangeIP;
	String username;
	String password;
	int port = -1;
	
	ObjectMapper mapper = new ObjectMapper();
	
	public RabbitMQClient(String rabbitMQExchangeIP, String username, String password) {
		this.rabbitMQExchangeIP = rabbitMQExchangeIP;
		this.username = username;
		this.password = password;
	}
	
	public RabbitMQClient(String rabbitMQExchangeIP, int port, String username, String password) {
		this.rabbitMQExchangeIP = rabbitMQExchangeIP;
		this.username = username;
		this.password = password;
		this.port = port;
	}
	
	public void publishEvent(BigDataStackEvent event) {
		
		if (rabbitMQExchangeIP!=null && rabbitMQExchangeIP.length()>0) {
			try {
				ConnectionFactory factory = new ConnectionFactory();
				if (port==-1) factory.setUri("amqp://"+username+":"+password+"@"+rabbitMQExchangeIP+":5672");
				else factory.setUri("amqp://"+username+":"+password+"@"+rabbitMQExchangeIP+":"+port);
				Connection conn = factory.newConnection();
				
				Channel channel = conn.createChannel();

				channel.basicPublish("BigDataStack-Orchestration", event.getNamepace(), null, mapper.writeValueAsString(event).getBytes());
				
				channel.close();
				conn.close();

				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
}
