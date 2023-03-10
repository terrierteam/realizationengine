package org.terrier.realization.structures.reports;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
