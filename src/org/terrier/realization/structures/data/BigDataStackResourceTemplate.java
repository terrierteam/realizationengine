package org.terrier.realization.structures.data;

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
