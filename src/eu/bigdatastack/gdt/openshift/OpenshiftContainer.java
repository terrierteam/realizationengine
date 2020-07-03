package eu.bigdatastack.gdt.openshift;

import java.util.Map;

public class OpenshiftContainer {

	String name;
	Map<String,String> requests;
	
	
	
	public OpenshiftContainer(String name, Map<String,String> requests) {
		super();
		this.name = name;
		this.requests = requests;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getRequests() {
		return requests;
	}

	public void setRequests(Map<String, String> requests) {
		this.requests = requests;
	}
	
	

	public String getRequestCPU() {
		if (!requests.containsKey("cpu")) return "0m";
		else return requests.get("cpu");
	}
	
	public String getRequestMemory() {
		if (!requests.containsKey("memory")) return "0Mi";
		else return requests.get("memory");
		
	}
	
	public int getRequestGPU() {
		if (!requests.containsKey("nvidia.com/gpu")) return 0;
		else return Integer.parseInt(requests.get("nvidia.com/gpu"));
	}
	
}
