package eu.bigdatastack.gdt.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import eu.bigdatastack.gdt.structures.data.BigDataStackEvent;

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
