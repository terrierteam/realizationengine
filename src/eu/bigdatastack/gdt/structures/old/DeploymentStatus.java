package eu.bigdatastack.gdt.structures.old;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
