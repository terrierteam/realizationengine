package org.terrier.realization.structures.old;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class DeploymentStatus {

	DeploymentStates status;
	String cdp;
	List<String> eventList;
	Map<String,List<String>> alternativePodCDPs;
	
	public DeploymentStatus(String cdp) {
		super();
		this.status = DeploymentStates.DeployReady;
		this.cdp = cdp;
		this.eventList = new ArrayList<String>(10);
		this.alternativePodCDPs = new HashMap<String,List<String>>();
	}
	
	public DeploymentStatus() {
		super();
		this.status = DeploymentStates.NotReady;
		this.cdp = cdp;
		this.eventList = new ArrayList<String>(10);
		this.alternativePodCDPs = new HashMap<String,List<String>>();
	}

	public void addEvent(String event) {
		eventList.add(event);
	}
	
	public String getStatus() {
		return status.name();
	}

	public void setStatus(DeploymentStates status) {
		this.status = status;
	}

	public String getCdp() {
		return cdp;
	}

	public void setCdp(String cdp) {
		this.cdp = cdp;
	}

	public List<String> getEventList() {
		return eventList;
	}

	public void setEventList(List<String> eventList) {
		this.eventList = eventList;
	}

	public Map<String, List<String>> getAlternativePodCDPs() {
		return alternativePodCDPs;
	}

	public void setAlternativePodCDPs(Map<String, List<String>> alternativePodCDPs) {
		this.alternativePodCDPs = alternativePodCDPs;
	}

	
	
	
	
}
