package org.terrier.realization.structures.reports;

import java.util.List;

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

public class EventTimeSeries {

	List<Integer> infoCountPerHour;
	List<Integer> warnCountPerHour;
	List<Integer> errCountPerHour;
	List<Integer> alertCountPerHour;
	
	long firstHourStart;
	long lastHourStart;
	
	public EventTimeSeries(List<Integer> infoCountPerHour, List<Integer> warnCountPerHour,
			List<Integer> errCountPerHour, List<Integer> alertCountPerHour, long firstHourStart, long lastHourStart) {
		super();
		this.infoCountPerHour = infoCountPerHour;
		this.warnCountPerHour = warnCountPerHour;
		this.errCountPerHour = errCountPerHour;
		this.alertCountPerHour = alertCountPerHour;
		this.firstHourStart = firstHourStart;
		this.lastHourStart = lastHourStart;
	}
	public List<Integer> getInfoCountPerHour() {
		return infoCountPerHour;
	}
	public void setInfoCountPerHour(List<Integer> infoCountPerHour) {
		this.infoCountPerHour = infoCountPerHour;
	}
	public List<Integer> getWarnCountPerHour() {
		return warnCountPerHour;
	}
	public void setWarnCountPerHour(List<Integer> warnCountPerHour) {
		this.warnCountPerHour = warnCountPerHour;
	}
	public List<Integer> getErrCountPerHour() {
		return errCountPerHour;
	}
	public void setErrCountPerHour(List<Integer> errCountPerHour) {
		this.errCountPerHour = errCountPerHour;
	}
	public List<Integer> getAlertCountPerHour() {
		return alertCountPerHour;
	}
	public void setAlertCountPerHour(List<Integer> alertCountPerHour) {
		this.alertCountPerHour = alertCountPerHour;
	}
	public long getFirstHourStart() {
		return firstHourStart;
	}
	public void setFirstHourStart(long firstHourStart) {
		this.firstHourStart = firstHourStart;
	}
	public long getLastHourStart() {
		return lastHourStart;
	}
	public void setLastHourStart(long lastHourStart) {
		this.lastHourStart = lastHourStart;
	}
	
	
	
}
