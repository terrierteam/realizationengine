package eu.bigdatastack.gdt.openshift;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;

import eu.bigdatastack.gdt.structures.openshift.IJob;


/**
 * This is a service class that performs operations that connect to the Openshift API
 * to recieve data about the cluster state.
 * @author richardm
 *
 */
public class OpenshiftStatusClient {

	String host;
	String username;
	String password;

	IClient client;
	
	ObjectMapper mapper = new ObjectMapper(); 
	
	
	/**
	 * Create a new client using host, username and password
	 * @param host
	 * @param username
	 * @param password
	 */
	public OpenshiftStatusClient(String host, String username, String password) {

		this.host = host;
		this.username = username;
		this.password = password;

		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	public OpenshiftStatusClient(String host, int port, String username, String password) {

		this.host = host+":"+port+"/";
		this.username = username;
		this.password = password;
		
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	/**
	 * Create a new client using an existing connection
	 * @param client
	 */
	public OpenshiftStatusClient(IClient client) {

		this.client = client;

		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Connects to the Openshift API using the Java Client 
	 * @return connection was successful;
	 */
	public boolean connectToOpenshift() {

		if (host==null) return false;
		
		try {
			
			ClientBuilder clientBuilder = new ClientBuilder(host)
					.withUserName(username)
					.withPassword(password);
			
			client = clientBuilder.build();

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} 

		return true;
	}

	/**
	 * Get a project from those visible to the current user. Returns null if no project found.
	 * @param projectName
	 * @return IProject
	 */
	public IProject getProject(String projectName) {

		try {
			
			IResource projectObj = client.get("Project", projectName, projectName);
			
			IResource selectedProject = projectObj;
			
			if (projectObj instanceof com.openshift.internal.restclient.model.List ) {
				com.openshift.internal.restclient.model.List projectList = (com.openshift.internal.restclient.model.List)projectObj;
			
				Iterator<IResource> resourceI = projectList.getItems().iterator();
				while (resourceI.hasNext()) {
					IResource resource = resourceI.next();
					if (resource.getName().equalsIgnoreCase(projectName)) {
						selectedProject = resource;
					}
				}
			}
			
			IProject project =  (IProject)selectedProject;
			return project;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * Returns all pods for a specified project. Returns null if the request fails.
	 * @param project
	 * @param active
	 * @param ended
	 * @return List<IPod>
	 */
	public List<IPod> getPods(IProject project, boolean active, boolean ended) {


		try {
			List<IPod> pods = client.list("pod", project.getName());

			List<IPod> selectedPods = new ArrayList<IPod>(pods.size());
			for (IPod pod : pods) {

				String status = pod.getStatus();
				if (active && (status.equalsIgnoreCase("Running") || status.equalsIgnoreCase("Pending") || status.equalsIgnoreCase("CrashLoopBackOff"))) selectedPods.add(pod);
				if (ended && (status.equalsIgnoreCase("Terminating") || status.equalsIgnoreCase("Completed") || status.equalsIgnoreCase("Failed"))) selectedPods.add(pod);

			}
			return selectedPods;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}


	}

	/**
	 * Returns all pods for a specified project. Returns null if the request fails.
	 * @param project
	 * @param active
	 * @param ended
	 * @return List<IPod>
	 */
	public List<IPod> getPods(IProject project, boolean active, boolean ended, String labelselector) {


		try {
			List<IPod> pods = client.list("pod", project.getName(), labelselector);

			List<IPod> selectedPods = new ArrayList<IPod>(pods.size());
			for (IPod pod : pods) {

				String status = pod.getStatus();
				if (active && (status.equalsIgnoreCase("Running") || status.equalsIgnoreCase("Pending") || status.equalsIgnoreCase("CrashLoopBackOff"))) selectedPods.add(pod);
				if (ended && (status.equalsIgnoreCase("Terminating") || status.equalsIgnoreCase("Completed") || status.equalsIgnoreCase("Failed"))) selectedPods.add(pod);

			}
			return selectedPods;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}


	}

	
	/**
	 * Returns all deployment configs for a specified project. Returns null if the request fails.
	 * @param project
	 * @return List<IPod>
	 */
	public List<IDeploymentConfig> getDeploymentConfigs(IProject project) {

		List<IDeploymentConfig> dcs;
		try {
			dcs = client.list("deploymentconfig", project.getName());
			return dcs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}


	}
	
	/**
	 * Returns all deployment configs for a specified project and name. Returns null if the request fails.
	 * @param project
	 * @return List<IPod>
	 */
	public IDeploymentConfig getDeploymentConfig(IProject project, String deploymentConfigName) {
		try {
			
			IDeploymentConfig dcObject = client.get("deploymentconfig", deploymentConfigName, project.getName());
			return dcObject;
		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Returns a specified resource
	 * @param project
	 * @return List<IPod>
	 */
	public IResource getResource(IProject project, String name, String kind) {
		try {
			
			IResource dcObject = client.get(kind, name, project.getName());
			return dcObject;
		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Returns all replication controllers for a specified project and name. Returns null if the request fails.
	 * @param project
	 * @return List<IResource>
	 */
	public List<IResource> getReplicationControllers(IProject project, String deploymentConfigName) {
		try {
			return client.list("replicationcontroller", project.getName(), "openshift.io/deployment-config.name="+deploymentConfigName);
		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns all Jobs for a specified project. Returns null if the request fails.
	 * @param project
	 * @return List<IResource> - represents a Job, there is not internal representation of a Job for some reason, so these need to be parsed manually.
	 */
	public List<IJob> getJobs(IProject project, boolean active, boolean ended) {

		try {

			List<IResource> jobs = client.list("job", project.getName());

			List<IJob> formattedJobs = new ArrayList<IJob>(jobs.size());
			for (IResource job : jobs) {

				IJob formatedJob = mapper.readValue(job.toJson(), IJob.class);
				
				boolean isDone = false;
				if (formatedJob.getStatus().has("succeeded")) isDone = true;
				
				if (active && !isDone) formattedJobs.add(formatedJob);
				if (ended && isDone) formattedJobs.add(formatedJob);
				
			}

			return formattedJobs;
		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		}


	}
	
	/**
	 * Returns a Job object with a particular name
	 * @param project
	 * @param name
	 * @return
	 */
	public IJob getJob(IProject project, String name) {
		IResource resource = client.get("job", name, project.getName());
		try {
			IJob formatedJob = mapper.readValue(resource.toJson(), IJob.class);
			return formatedJob;
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Returns all pods for a specified project with a jobName. Returns null if the request fails.
	 * @param project
	 * @param jobName
	 * @return List<IPod>
	 */
	public List<IPod> getPodsForJob(IProject project, String jobName) {


		try {
			List<IPod> pods = client.list("pod", project.getName(), "job-name="+jobName);

			return pods;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}


	}
	
	/**
	 * Returns all pods for a specified project with a deployment config name. Returns null if the request fails.
	 * @param project
	 * @param deploymentConfigName
	 * @return List<IPod>
	 */
	public List<IPod> getPodsForDeploymentConfig(IProject project, String deploymentConfigName) {


		try {
			List<IPod> pods = client.list("pod", project.getName(), "deploymentconfig="+deploymentConfigName);

			return pods;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}


	}
	

	public void close() {
		client.close();
	}

	public static void main(String[] args) throws IOException {
		OpenshiftStatusClient actor = new OpenshiftStatusClient("https://idagpu-head.dcs.gla.ac.uk:8443/", "admin", "IDAAdmin2019");

		actor.connectToOpenshift();
		IProject project = actor.getProject("zaiqiaoproject");

		ObjectMapper mapper = new ObjectMapper();

		System.out.println(project.getName()+" "+project.getDescription()+" "+project.getStatus());
		System.out.println(mapper.writeValueAsString(project.getLabels()));
		System.out.println(mapper.writeValueAsString(project.getMetadata()));


		List<IPod> pods = actor.getPods(project, true, false);

		for (IPod pod : pods) {
			System.err.println(pod.getName());
		}

		List<IDeploymentConfig> deploymentConfigs = actor.getDeploymentConfigs(project);

		for (IDeploymentConfig dc : deploymentConfigs) {
			System.err.println(dc.getName());
		}

		List<IJob> jobs = actor.getJobs(project, true, false);

		for (IJob job : jobs) {
			System.err.println(job.getMetadata().getName()+" "+job.getStatus().toString());
		}
		
		actor.close();
	}


}
