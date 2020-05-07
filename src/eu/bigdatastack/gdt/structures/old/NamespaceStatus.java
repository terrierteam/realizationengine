package eu.bigdatastack.gdt.structures.old;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of the status of a user namespace 
 * @author richardm
 *
 */
public class NamespaceStatus {

	List<ApplicationStatus> userApplications;
	
	public NamespaceStatus() {
		userApplications = new ArrayList<ApplicationStatus>(1);
	}

	public List<ApplicationStatus> getUserApplications() {
		return userApplications;
	}

	public void setUserApplications(List<ApplicationStatus> userApplications) {
		this.userApplications = userApplications;
	}
	
	
	
}
