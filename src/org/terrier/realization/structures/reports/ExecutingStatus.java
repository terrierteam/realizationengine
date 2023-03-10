package org.terrier.realization.structures.reports;

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

public class ExecutingStatus {

	int sequencesActive;
	int deploymentsActive;
	int jobsActive;
	int podsActive;
	int servicesActive;
	public ExecutingStatus(int sequencesActive, int deploymentsActive, int jobsActive, int podsActive,
			int servicesActive) {
		super();
		this.sequencesActive = sequencesActive;
		this.deploymentsActive = deploymentsActive;
		this.jobsActive = jobsActive;
		this.podsActive = podsActive;
		this.servicesActive = servicesActive;
	}
	public int getSequencesActive() {
		return sequencesActive;
	}
	public void setSequencesActive(int sequencesActive) {
		this.sequencesActive = sequencesActive;
	}
	public int getDeploymentsActive() {
		return deploymentsActive;
	}
	public void setDeploymentsActive(int deploymentsActive) {
		this.deploymentsActive = deploymentsActive;
	}
	public int getJobsActive() {
		return jobsActive;
	}
	public void setJobsActive(int jobsActive) {
		this.jobsActive = jobsActive;
	}
	public int getPodsActive() {
		return podsActive;
	}
	public void setPodsActive(int podsActive) {
		this.podsActive = podsActive;
	}
	public int getServicesActive() {
		return servicesActive;
	}
	public void setServicesActive(int servicesActive) {
		this.servicesActive = servicesActive;
	}
	
	
	
}
