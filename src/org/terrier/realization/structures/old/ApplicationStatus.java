package org.terrier.realization.structures.old;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

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
 * The status of a user application, described by a playbook
 * @deprecated
 *
 */
public class ApplicationStatus {
	
	String identifier;
	String playbook;
	String status;
	DeploymentStatus currentDeployment;
	List<DeploymentStatus> previousDeployments;
	
	public ApplicationStatus() {}
	
	public ApplicationStatus(String identifier, String playbook) {
		super();
		this.playbook = playbook;
		status = "New application, deployment is not yet configured.";
		currentDeployment = null;
		previousDeployments = new ArrayList<DeploymentStatus>(3);
		this.identifier = identifier;
	}

	public void registerNewDeployment(DeploymentStatus deployment) {
		if (currentDeployment!=null) {
			//previousDeployments.add(currentDeployment);
		}
		currentDeployment = deployment;
		status = "Deployment "+previousDeployments.size();
	}
	
	public String getPlaybook() {
		return playbook;
	}

	public void setPlaybook(String playbook) {
		this.playbook = playbook;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public DeploymentStatus getCurrentDeployment() {
		return currentDeployment;
	}

	public void setCurrentDeployment(DeploymentStatus currentDeployment) {
		this.currentDeployment = currentDeployment;
	}

	public List<DeploymentStatus> getPreviousDeployments() {
		return previousDeployments;
	}

	public void setPreviousDeployments(List<DeploymentStatus> previousDeployments) {
		this.previousDeployments = previousDeployments;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	
	
}
