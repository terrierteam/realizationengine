package eu.bigdatastack.gdt.structures.reports;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RealizationStatus {

	Map<String,Set<String>> apiInstance2Status;
	Map<String,Set<String>> monitorInstance2Status;
	Map<String,Set<String>> costestimatorInstance2Status;
	Map<String,Set<String>> prometheusInstance2Status;
	Map<String,Set<String>> dbInstance2Status;
	
	public RealizationStatus() {
		apiInstance2Status = new HashMap<String,Set<String>>();
		monitorInstance2Status = new HashMap<String,Set<String>>();
		costestimatorInstance2Status = new HashMap<String,Set<String>>();
		prometheusInstance2Status = new HashMap<String,Set<String>>();
		dbInstance2Status = new HashMap<String,Set<String>>();
	}

	public Map<String, Set<String>> getApiInstance2Status() {
		return apiInstance2Status;
	}

	public void setApiInstance2Status(Map<String, Set<String>> apiInstance2Status) {
		this.apiInstance2Status = apiInstance2Status;
	}

	public Map<String, Set<String>> getMonitorInstance2Status() {
		return monitorInstance2Status;
	}

	public void setMonitorInstance2Status(Map<String, Set<String>> monitorInstance2Status) {
		this.monitorInstance2Status = monitorInstance2Status;
	}

	public Map<String, Set<String>> getCostestimatorInstance2Status() {
		return costestimatorInstance2Status;
	}

	public void setCostestimatorInstance2Status(Map<String, Set<String>> costestimatorInstance2Status) {
		this.costestimatorInstance2Status = costestimatorInstance2Status;
	}

	public Map<String, Set<String>> getPrometheusInstance2Status() {
		return prometheusInstance2Status;
	}

	public void setPrometheusInstance2Status(Map<String, Set<String>> prometheusInstance2Status) {
		this.prometheusInstance2Status = prometheusInstance2Status;
	}

	public Map<String, Set<String>> getDbInstance2Status() {
		return dbInstance2Status;
	}

	public void setDbInstance2Status(Map<String, Set<String>> dbInstance2Status) {
		this.dbInstance2Status = dbInstance2Status;
	}
	
	
	
}
