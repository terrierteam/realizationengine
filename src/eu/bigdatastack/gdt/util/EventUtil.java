package eu.bigdatastack.gdt.util;

import java.sql.SQLException;

import eu.bigdatastack.gdt.lxdb.BigDataStackEventIO;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackEvent;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;

public class EventUtil {

	BigDataStackEventIO eventIO;
	RabbitMQClient mailboxClient;
	
	public EventUtil(BigDataStackEventIO eventIO, RabbitMQClient mailboxClient) throws SQLException {
		this.mailboxClient= mailboxClient;
		this.eventIO = eventIO;
	}
	
	public boolean registerEvent(String appID, String owner, String namespace,
			BigDataStackEventType type, BigDataStackEventSeverity severity, String title, String description,
			String objectID, int instance) throws SQLException {
		
		int failures = 0;
		boolean eventRegistered = false;
		
		while (!eventRegistered) {
			int previousEvents = eventIO.getEventCount(appID, owner);
			
			BigDataStackEvent newEvent = new BigDataStackEvent(
					appID,
					owner,
					previousEvents,
					namespace,
					type,
					severity,
					title,
					description,
					objectID
					);
			eventRegistered = eventIO.addEvent(newEvent);
			if (!eventRegistered) {
				failures++;
				if (failures>=5) {
					System.err.println("Event not Sent");
					newEvent.print();
					return false;
				} else continue;
			}
			
			mailboxClient.publishEvent(newEvent);
			
			newEvent.print();
			
			//System.err.println(newEvent.getAppID()+" "+newEvent.getOwner()+" "+newEvent.getEventNo()+" "+newEvent.getTitle());
		}
		
		return true;
		
	}
	
}
