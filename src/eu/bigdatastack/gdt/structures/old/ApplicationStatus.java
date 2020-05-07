package eu.bigdatastack.gdt.structures.old;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The status of a user application, described by a playbook
 * @author richardm
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
