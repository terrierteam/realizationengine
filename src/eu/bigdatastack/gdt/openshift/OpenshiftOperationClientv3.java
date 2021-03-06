package eu.bigdatastack.gdt.openshift;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;


import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackPodStatus;

public class OpenshiftOperationClientv3 implements OpenshiftOperationClient {

	String host;
	String username;
	String password;

	IClient client;

	ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
	ObjectMapper jsonMapper = new ObjectMapper();
	
	/**
	 * Create a new client using host, username and password
	 * @param host
	 * @param username
	 * @param password
	 */
	public OpenshiftOperationClientv3(String host, String username, String password) {

		this.host = host;
		this.username = username;
		this.password = password;

	}
	
	public OpenshiftOperationClientv3(String host, int port, String username, String password) {

		this.host = host+":"+port+"/";
		this.username = username;
		this.password = password;

	}
	
	
	/**
	 * Create a new client using an existing connection
	 * @param client
	 */
	public OpenshiftOperationClientv3(IClient client) {

		this.client = client;

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
	 * Creates an BigDataStack Object on the cluster
	 * @param object
	 * @return
	 */
	public boolean applyOperation(BigDataStackObjectDefinition object) {
		
		try {
			IResource resource;
			switch (object.getType()) {
				case DeploymentConfig:
					//System.err.println(object.getYamlSource());
					resource = client.getResourceFactory().create(yaml2Json(object.getYamlSource()));
					IDeploymentConfig deploymentConfig = (IDeploymentConfig)resource;
					client.create(deploymentConfig);
					return true;
				case Service:
					resource = client.getResourceFactory().create(yaml2Json(object.getYamlSource()));
					client.create(resource);
					return true;
				case Job:
					resource = client.getResourceFactory().create(yaml2Json(object.getYamlSource()));
					client.create(resource);
					return true;
				case Route:
					resource = client.getResourceFactory().create(yaml2Json(object.getYamlSource()));
					client.create(resource);
					return true;
				case Volume:
					resource = client.getResourceFactory().create(yaml2Json(object.getYamlSource()));
					client.create(resource);
					return true;
				case VolumeClaim:
					resource = client.getResourceFactory().create(yaml2Json(object.getYamlSource()));
					client.create(resource);
					return true;
				case Pod:
					resource = client.getResourceFactory().create(yaml2Json(object.getYamlSource()));
					client.create(resource);
					return true;
				case Secret:
					resource = client.getResourceFactory().create(yaml2Json(object.getYamlSource()));
					client.create(resource);
					return true;
				case ConfigMap:
					resource = client.getResourceFactory().create(yaml2Json(object.getYamlSource()));
					client.create(resource);
					return true;
				case ServiceAccount:
					resource = client.getResourceFactory().create(yaml2Json(object.getYamlSource()));
					client.create(resource);
					return true;
				case RoleBinding:
					resource = client.getResourceFactory().create(yaml2Json(object.getYamlSource()));
					client.create(resource);
					return true;
				case Role:
					resource = client.getResourceFactory().create(yaml2Json(object.getYamlSource()));
					client.create(resource);
					return true;
				case Playbook:
					return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
		
	}
	
	/**
	 * Deletes an object from openshift
	 * @param object
	 * @return
	 */
	public boolean deleteOperation(BigDataStackObjectDefinition object) {
		try {
			
			// create a copy of status client so we can more easily get existing objects
			OpenshiftStatusClientv3 statusClient = new OpenshiftStatusClientv3(client);
			
			IProject project = statusClient.getProject(object.getNamespace());
			
			IResource resource;
			switch (object.getType()) {
				case DeploymentConfig:
					resource = statusClient.getDeploymentConfig(project, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
					client.delete(resource);
					
					// deleting a deployment config does not delete the underlying replication controller, so delete that too
					List<IResource> controllers = statusClient.getReplicationControllers(project, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
					for (IResource controller : controllers) {
						client.delete(controller);
					}
					
					List<IPod> pods1 = statusClient.getPods(project, true, true, "deploymentconfig="+object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
					for (IPod pod : pods1) {
						client.delete(pod);
					}
					
					
					return true;
				case Service:
					resource = statusClient.getResource(project, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance(), "service");
					client.delete(resource);
					return true;
				case Job:
					resource = client.get("job", object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance(), project.getName());
					
					List<IPod> pods = statusClient.getPodsForJob(project, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
					
					client.delete(resource);
					for (IPod pod : pods) {
						client.delete(pod);
					}
					return true;
				case Route:
					resource = statusClient.getResource(project, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance(), "route");
					client.delete(resource);
					return true;
				case Volume:
					resource = statusClient.getResource(project, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance(), "volume");
					client.delete(resource);
					return true;
				case VolumeClaim:
					resource = statusClient.getResource(project, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance(), "volumeclaim");
					client.delete(resource);
					return true;
				case Pod:
					resource = statusClient.getResource(project, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance(), "pod");
					client.delete(resource);
					return true;
				case Secret:
					resource = statusClient.getResource(project, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance(), "secret");
					client.delete(resource);
					return true;
				case ConfigMap:
					resource = statusClient.getResource(project, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance(), "configmap");
					client.delete(resource);
					return true;
				case ServiceAccount:
					resource = statusClient.getResource(project, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance(), "serviceaccount");
					client.delete(resource);
					return true;
				case RoleBinding:
					resource = statusClient.getResource(project, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance(), "rolebinding");
					client.delete(resource);
					return true;
				case Role:
					resource = statusClient.getResource(project, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance(), "role");
					client.delete(resource);
					return true;
				case Playbook:
					return false;
				default:
					return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	public String yaml2Json(String yaml) throws JsonMappingException, JsonProcessingException {
		JsonNode node = yamlMapper.readTree(yaml);
		return node.toString();
	}
	
	public void close() {
		client.close();
	}

	public IClient getClient() {
		return client;
	}

	@Override
	public List<String> execCommands(BigDataStackPodStatus pod, String[][] commands) {
		return null;
	}
	
	
	
}
