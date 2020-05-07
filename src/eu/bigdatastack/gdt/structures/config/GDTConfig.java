package eu.bigdatastack.gdt.structures.config;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GDTConfig {

	DatabaseConf database;
	RabbitMQConf rabbitmq;
	OpenshiftConfig openshift;
	
	public GDTConfig() {};
	
	public GDTConfig(File file) throws Exception {
		
		ObjectMapper mapper = new ObjectMapper();
		
		GDTConfig conf = mapper.readValue(file, GDTConfig.class);
		database = conf.getDatabase();
		rabbitmq = conf.getRabbitmq();
		openshift = conf.getOpenshift();
	}
	
	public GDTConfig(DatabaseConf database, RabbitMQConf rabbitmq, OpenshiftConfig openshift) {
		super();
		this.database = database;
		this.rabbitmq = rabbitmq;
		this.openshift = openshift;
	}
	public DatabaseConf getDatabase() {
		return database;
	}
	public void setDatabase(DatabaseConf database) {
		this.database = database;
	}
	public RabbitMQConf getRabbitmq() {
		return rabbitmq;
	}
	public void setRabbitmq(RabbitMQConf rabbitmq) {
		this.rabbitmq = rabbitmq;
	}
	public OpenshiftConfig getOpenshift() {
		return openshift;
	}
	public void setOpenshift(OpenshiftConfig openshift) {
		this.openshift = openshift;
	}
	
	
	
}
