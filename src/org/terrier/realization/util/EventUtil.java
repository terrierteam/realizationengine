package org.terrier.realization.util;

import java.sql.SQLException;

import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.BigDataStackEventIO;
import org.terrier.realization.structures.data.BigDataStackEvent;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;

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
					objectID,
					instance
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
			
			if (mailboxClient!=null) mailboxClient.publishEvent(newEvent);
			
			newEvent.print();
			
			//System.err.println(newEvent.getAppID()+" "+newEvent.getOwner()+" "+newEvent.getEventNo()+" "+newEvent.getTitle());
		}
		
		return true;
		
	}
	
}
