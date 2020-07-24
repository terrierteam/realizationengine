package eu.bigdatastack.gdt.structures.reports;

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
