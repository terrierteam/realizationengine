package org.terrier.realization.structures.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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

/**
 * Represents a human-readable event that has occurred during system operation. Events are
 * associated to an owner, application and optionally an object.
 * @author EbonBlade
 *
 */
public class BigDataStackEvent implements Comparable<BigDataStackEvent> {

	private String appID;
	private String owner;
	private int eventNo;
	
	private String namepace;
	private long eventTime;
	private BigDataStackEventType type;
	private BigDataStackEventSeverity severity;
	private String title;
	private String description;
	private String objectID;
	private int instance;
	
	public BigDataStackEvent() {}
	
	public BigDataStackEvent(String appID, String owner, int eventNo, String namepace, long eventTime,
			BigDataStackEventType type, BigDataStackEventSeverity severity, String title, String description,
			String objectID) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.eventNo = eventNo;
		this.namepace = namepace;
		this.eventTime = eventTime;
		this.type = type;
		this.severity = severity;
		this.title = title;
		this.description = description;
		this.objectID = objectID;
		this.instance = 0;
	}
	
	public BigDataStackEvent(String appID, String owner, int eventNo, String namepace,
			BigDataStackEventType type, BigDataStackEventSeverity severity, String title, String description,
			String objectID) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.eventNo = eventNo;
		this.namepace = namepace;
		this.eventTime = System.currentTimeMillis();
		this.type = type;
		this.severity = severity;
		this.title = title;
		this.description = description;
		this.objectID = objectID;
		this.instance = 0;
	}
	
	public BigDataStackEvent(String appID, String owner, int eventNo, String namepace,
			BigDataStackEventType type, BigDataStackEventSeverity severity, String title, String description,
			String objectID, int instance) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.eventNo = eventNo;
		this.namepace = namepace;
		this.eventTime = System.currentTimeMillis();
		this.type = type;
		this.severity = severity;
		this.title = title;
		this.description = description;
		this.objectID = objectID;
		this.instance = instance;
	}

	public BigDataStackEvent(String appID, String owner, int eventNo, String namepace, long eventTime,
			BigDataStackEventType type, BigDataStackEventSeverity severity, String title, String description,
			String objectID, int instance) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.eventNo = eventNo;
		this.namepace = namepace;
		this.eventTime = eventTime;
		this.type = type;
		this.severity = severity;
		this.title = title;
		this.description = description;
		this.objectID = objectID;
		this.instance = instance;
	}

	public String getAppID() {
		return appID;
	}

	public void setAppID(String appID) {
		this.appID = appID;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public int getEventNo() {
		return eventNo;
	}

	public void setEventNo(int eventNo) {
		this.eventNo = eventNo;
	}

	public String getNamepace() {
		return namepace;
	}

	public void setNamepace(String namepace) {
		this.namepace = namepace;
	}


	public BigDataStackEventType getType() {
		return type;
	}

	public void setType(BigDataStackEventType type) {
		this.type = type;
	}

	public BigDataStackEventSeverity getSeverity() {
		return severity;
	}

	public void setSeverity(BigDataStackEventSeverity severity) {
		this.severity = severity;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getObjectID() {
		return objectID;
	}

	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}

	public int getInstance() {
		return instance;
	}

	public void setInstance(int instance) {
		this.instance = instance;
	}

	@Override
	public int compareTo(BigDataStackEvent o) {
		return Long.valueOf(eventTime).compareTo(o.eventTime);
	}

	public long getEventTime() {
		return eventTime;
	}

	public void setEventTime(long eventTime) {
		this.eventTime = eventTime;
	}
	
	public void print() {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(eventTime);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");  
		String strDate = dateFormat.format(c.getTime()); 
		
		System.out.println(strDate+" ["+namepace+"/"+appID+"/"+objectID+"] ["+severity+"]: "+title);
	}
	
	
	
}
