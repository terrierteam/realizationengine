package eu.bigdatastack.gdt.structures.data;

import java.util.Map;

public class BigDataStackResourceTemplate {

	Map<String,String> requests;
	Map<String,String> limits;
	
	public BigDataStackResourceTemplate() {}
	
	public BigDataStackResourceTemplate(Map<String, String> requests, Map<String, String> limits) {
		super();
		this.requests = requests;
		this.limits = limits;
	}

	public Map<String, String> getRequests() {
		return requests;
	}

	public void setRequests(Map<String, String> requests) {
		this.requests = requests;
	}

	public Map<String, String> getLimits() {
		return limits;
	}

	public void setLimits(Map<String, String> limits) {
		this.limits = limits;
	}
	
	
}
