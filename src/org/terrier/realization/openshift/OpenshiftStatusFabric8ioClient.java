package org.terrier.realization.openshift;

import java.util.ArrayList;
import java.util.List;

import org.terrier.realization.util.Fabric8ioConverterUtil;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.DoneableReplicationController;
import io.fabric8.kubernetes.api.model.DoneableSecret;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.DoneableServiceAccount;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountList;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.batch.DoneableJob;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigList;
import io.fabric8.openshift.api.model.DoneableDeploymentConfig;
import io.fabric8.openshift.api.model.DoneableProject;
import io.fabric8.openshift.api.model.DoneableRole;
import io.fabric8.openshift.api.model.DoneableRoleBinding;
import io.fabric8.openshift.api.model.DoneableRoute;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.Role;
import io.fabric8.openshift.api.model.RoleBinding;
import io.fabric8.openshift.api.model.RoleBindingList;
import io.fabric8.openshift.api.model.RoleList;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.dsl.DeployableScalableResource;

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
 * This class provides the underlying implementation for getting the status of objects on the
 * Openshift cluster using the fabric8io kubernetes/openshift client
 * https://github.com/fabric8io/kubernetes-client
 *
 */
public class OpenshiftStatusFabric8ioClient implements OpenshiftStatusClient {

	final String client = "fabric8io";
	
	String host;
	int port;
	String username;
	String password;
	String namespace;
	
	OpenShiftClient osClient;
	
	public OpenshiftStatusFabric8ioClient(String host, int port, String username, String password, String namespace) {
		super();
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.namespace = namespace;
	}
	
	public OpenshiftStatusFabric8ioClient (OpenShiftClient osClient) {
		this.osClient = osClient;
	}

	@Override
	public boolean connectToOpenshift() {
		try {
			Config config = new ConfigBuilder()
					.withMasterUrl(host+":"+port)
					.withUsername(username)
					.withPassword(password)
					.withTrustCerts(true)
					.withNamespace(namespace)
					.build();
			
			osClient = new DefaultOpenShiftClient(config);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	
	//-----------------------------------------------
	// Get Methods for single objects
	//-----------------------------------------------
	
	@Override
	public OpenshiftObject getProject(String projectName) {
		Resource<Project,DoneableProject> project = osClient.projects().withName(projectName);
		
		if (project==null) return null;
		if (project.get()==null) return null;
		
		return Fabric8ioConverterUtil.convertProject(project.get());
	}
	
	@Override
	public OpenshiftObject getPod(String namespace, String name) {
		PodResource<Pod, DoneablePod> pod = osClient.pods().inNamespace(namespace).withName(name);
		if (pod==null) return null;
		if (pod.get()==null) return null;
		
		return Fabric8ioConverterUtil.convertPod(pod.get());
	}
	
	@Override
	public OpenshiftObject getDeploymentConfig(String projectName, String deploymentConfigName) {
		DeployableScalableResource<DeploymentConfig, DoneableDeploymentConfig> dc = osClient.deploymentConfigs().inNamespace(projectName).withName(deploymentConfigName);
		if (dc==null) {
			System.err.println("Failed to retrieve "+deploymentConfigName+" in "+projectName);
			return null;
		}
		if (dc.get()==null) {
			System.err.println("Failed to get "+deploymentConfigName+" in "+projectName);
			return null;
		}
		
		return Fabric8ioConverterUtil.convertDeploymentConfig(dc.get());
	}
	
	@Override
	public OpenshiftObject getJob(String projectName, String name) {
		ScalableResource<Job,DoneableJob> job = osClient.batch().jobs().inNamespace(projectName).withName(name);
		if (job==null) return null;
		if (job.get()==null) return null;
		
		return Fabric8ioConverterUtil.convertJob(job.get());
	}
	
	@Override
	public OpenshiftObject getReplicationController(String projectName, String controllerName) {
		RollableScalableResource<ReplicationController, DoneableReplicationController> rc = osClient.replicationControllers().inNamespace(projectName).withName(controllerName);
		if (rc==null) return null;
		if (rc.get()==null) return null;
		
		return Fabric8ioConverterUtil.convertReplicationController(rc.get());
	}
	
	@Override
	public OpenshiftObject getConfigMap(String projectName, String name) {
		Resource<ConfigMap, DoneableConfigMap> fabric8ioObject = osClient.configMaps().inNamespace(projectName).withName(name);
		if (fabric8ioObject==null) return null;
		if (fabric8ioObject.get()==null) return null;
		
		return Fabric8ioConverterUtil.convertConfigMap(fabric8ioObject.get());
	}
	
	@Override
	public OpenshiftObject getService(String projectName, String name) {
		ServiceResource<Service,DoneableService> fabric8ioObject = osClient.services().inNamespace(projectName).withName(name);
		if (fabric8ioObject==null) return null;
		if (fabric8ioObject.get()==null) return null;
		
		return Fabric8ioConverterUtil.convertService(fabric8ioObject.get());
	}
	
	@Override
	public OpenshiftObject getSecret(String projectName, String name) {
		Resource<Secret,DoneableSecret> fabric8ioObject = osClient.secrets().inNamespace(projectName).withName(name);
		if (fabric8ioObject==null) return null;
		if (fabric8ioObject.get()==null) return null;
		
		return Fabric8ioConverterUtil.convertSecret(fabric8ioObject.get());
	}
	
	@Override
	public OpenshiftObject getServiceAccount(String projectName, String name) {
		Resource<ServiceAccount,DoneableServiceAccount> fabric8ioObject = osClient.serviceAccounts().inNamespace(projectName).withName(name);
		if (fabric8ioObject==null) return null;
		if (fabric8ioObject.get()==null) return null;
		
		return Fabric8ioConverterUtil.convertServiceAccount(fabric8ioObject.get());
	}
	
	@Override
	public OpenshiftObject getRoleBinding(String projectName, String name) {
		Resource<RoleBinding,DoneableRoleBinding> fabric8ioObject = osClient.roleBindings().inNamespace(projectName).withName(name);
		if (fabric8ioObject==null) return null;
		if (fabric8ioObject.get()==null) return null;
		
		return Fabric8ioConverterUtil.convertRoleBinding(fabric8ioObject.get());
	}
	
	@Override
	public OpenshiftObject getRoute(String projectName, String name) {
		Resource<Route,DoneableRoute> fabric8ioObject = osClient.routes().inNamespace(projectName).withName(name);
		if (fabric8ioObject==null) return null;
		if (fabric8ioObject.get()==null) return null;
		
		return Fabric8ioConverterUtil.convertRoute(fabric8ioObject.get());
	}
	
	@Override
	public OpenshiftObject getRole(String projectName, String name) {
		Resource<Role,DoneableRole> fabric8ioObject = osClient.roles().inNamespace(projectName).withName(name);
		if (fabric8ioObject==null) return null;
		if (fabric8ioObject.get()==null) return null;
		
		return Fabric8ioConverterUtil.convertRole(fabric8ioObject.get());
	}
	

	
	
	//-----------------------------------------------
	// Generic single object accessor
	//-----------------------------------------------

	@Override
	public OpenshiftObject getResource(String projectName, String name, String kind) {
		switch (kind) {
		case "Project":
			return getProject(projectName);
		case "Pod":
			return getPod(projectName, name);
		case "DeploymentConfig":
			return getDeploymentConfig(projectName, name);
		case "Job":
			return getJob(projectName, name);
		case "ReplicationController":
			return getReplicationController(projectName, name);
		case "ConfigMap":
			return getConfigMap(projectName, name);
		case "Service":
			return getService(projectName, name);
		case "Secret":
			return getSecret(projectName, name);
		case "ServiceAccount":
			return getServiceAccount(projectName, name);
		case "RoleBinding":
			return getRoleBinding(projectName, name);
		case "Route":
			return getRoute(projectName, name);
		case "Role":
			return getRole(projectName, name);
		case "project":
			return getProject(projectName);
		case "pod":
			return getPod(projectName, name);
		case "deploymentconfig":
			return getDeploymentConfig(projectName, name);
		case "job":
			return getJob(projectName, name);
		case "replicationcontroller":
			return getReplicationController(projectName, name);
		case "configmap":
			return getConfigMap(projectName, name);
		case "service":
			return getService(projectName, name);
		case "secret":
			return getSecret(projectName, name);
		case "serviceaccount":
			return getServiceAccount(projectName, name);
		case "rolebinding":
			return getRoleBinding(projectName, name);
		case "route":
			return getRoute(projectName, name);
		case "role":
			return getRole(projectName, name);
		default:
			throw new UnsupportedOperationException("Requested Openshift Resource of type '"+kind+"', but this was not supported by OpenshiftStatusFabric8ioClient");
		}
	}
	
	//--------------------------------------------------------
	// Get methods for lists of objects
	//--------------------------------------------------------
	
	@Override
	public List<OpenshiftObject> getPods(String projectName) {
		PodList pods = osClient.pods().inNamespace(projectName).list();
		
		List<OpenshiftObject> selectedPods = new ArrayList<OpenshiftObject>(pods.getItems().size());
		for (Pod pod : pods.getItems()) {
			OpenshiftObject podAsObject = Fabric8ioConverterUtil.convertPod(pod);
			selectedPods.add(podAsObject);
		}
		
		return selectedPods;
	}
	
	@Override
	public List<OpenshiftObject> getPods(String projectName, boolean active, boolean ended) {
		PodList pods = osClient.pods().inNamespace(projectName).list();
		
		List<OpenshiftObject> selectedPods = new ArrayList<OpenshiftObject>(pods.getItems().size());
		for (Pod pod : pods.getItems()) {
			OpenshiftObject podAsObject = Fabric8ioConverterUtil.convertPod(pod);
			if (active && (podAsObject.getStatuses().contains("Running") || podAsObject.getStatuses().contains("Pending") || podAsObject.getStatuses().contains("CrashLoopBackOff"))) selectedPods.add(podAsObject);
			if (ended && (podAsObject.getStatuses().contains("Terminating") || podAsObject.getStatuses().contains("Completed") || podAsObject.getStatuses().contains("Failed"))) selectedPods.add(podAsObject);
		}
		
		return selectedPods;
	}

	@Override
	public List<OpenshiftObject> getPods(String projectName, boolean active, boolean ended, String labelselector) {
		ListOptions query = new ListOptions();
		query.setLabelSelector(labelselector);
		PodList pods = osClient.pods().inNamespace(projectName).list(query);
		
		List<OpenshiftObject> selectedPods = new ArrayList<OpenshiftObject>(pods.getItems().size());
		for (Pod pod : pods.getItems()) {
			OpenshiftObject podAsObject = Fabric8ioConverterUtil.convertPod(pod);
			if (active && ended) selectedPods.add(podAsObject);
			else {
				if (active && (podAsObject.getStatuses().contains("Running") || podAsObject.getStatuses().contains("Pending") || podAsObject.getStatuses().contains("CrashLoopBackOff"))) selectedPods.add(podAsObject);
				if (ended && (podAsObject.getStatuses().contains("Terminating") || podAsObject.getStatuses().contains("Completed") || podAsObject.getStatuses().contains("Succeeded") || podAsObject.getStatuses().contains("Failed") || podAsObject.getStatuses().contains("Failed"))) selectedPods.add(podAsObject);
			}
			
			
		}
		
		return selectedPods;
	}

	
	@Override
	public List<OpenshiftObject> getDeploymentConfigs(String projectName) {
		DeploymentConfigList dcs = osClient.deploymentConfigs().inNamespace(projectName).list();
		
		List<OpenshiftObject> convertedObjects = new ArrayList<OpenshiftObject>(dcs.getItems().size());
		for (DeploymentConfig dc : dcs.getItems()) {
			OpenshiftObject dcAsObject = Fabric8ioConverterUtil.convertDeploymentConfig(dc);
			convertedObjects.add(dcAsObject);
		}
		return convertedObjects;
	}


	@Override
	public List<OpenshiftObject> getJobs(String projectName, boolean active, boolean ended) {
		JobList fabric8ioList = osClient.batch().jobs().inNamespace(projectName).list();
		
		List<OpenshiftObject> convertedObjects = new ArrayList<OpenshiftObject>(fabric8ioList.getItems().size());
		for (Job dc : fabric8ioList.getItems()) {
			OpenshiftObject dcAsObject = Fabric8ioConverterUtil.convertJob(dc);
			
			boolean complete = dcAsObject.getStatuses().contains("Complete");
			if (ended && complete) convertedObjects.add(dcAsObject);
			else if (active) convertedObjects.add(dcAsObject);
		}
		return convertedObjects;
	}
	
	
	@Override
	public List<OpenshiftObject> getJobs(String projectName) {
		JobList fabric8ioList = osClient.batch().jobs().inNamespace(projectName).list();
		
		List<OpenshiftObject> convertedObjects = new ArrayList<OpenshiftObject>(fabric8ioList.getItems().size());
		for (Job dc : fabric8ioList.getItems()) {
			OpenshiftObject dcAsObject = Fabric8ioConverterUtil.convertJob(dc);
			convertedObjects.add(dcAsObject);
		}
		return convertedObjects;
	}
	
	@Override
	public List<OpenshiftObject> getConfigMaps(String projectName) {
		ConfigMapList fabric8ioList = osClient.configMaps().inNamespace(projectName).list();
		
		List<OpenshiftObject> convertedObjects = new ArrayList<OpenshiftObject>(fabric8ioList.getItems().size());
		for (ConfigMap fabricio8Object : fabric8ioList.getItems()) {
			OpenshiftObject object = Fabric8ioConverterUtil.convertConfigMap(fabricio8Object);
			convertedObjects.add(object);
		}
		return convertedObjects;
	}
	
	@Override
	public List<OpenshiftObject> getReplicationControllers(String projectName) {
		ReplicationControllerList fabric8ioList = osClient.replicationControllers().inNamespace(projectName).list();
		
		List<OpenshiftObject> convertedObjects = new ArrayList<OpenshiftObject>(fabric8ioList.getItems().size());
		for (ReplicationController fabricio8Object : fabric8ioList.getItems()) {
			OpenshiftObject object = Fabric8ioConverterUtil.convertReplicationController(fabricio8Object);
			convertedObjects.add(object);
		}
		return convertedObjects;
	}
	
	@Override
	public List<OpenshiftObject> getRoleBindings(String projectName) {
		RoleBindingList fabric8ioList = osClient.roleBindings().inNamespace(projectName).list();
		
		List<OpenshiftObject> convertedObjects = new ArrayList<OpenshiftObject>(fabric8ioList.getItems().size());
		for (RoleBinding fabricio8Object : fabric8ioList.getItems()) {
			OpenshiftObject object = Fabric8ioConverterUtil.convertRoleBinding(fabricio8Object);
			convertedObjects.add(object);
		}
		return convertedObjects;
	}
	
	@Override
	public List<OpenshiftObject> getSecrets(String projectName) {
		SecretList fabric8ioList = osClient.secrets().inNamespace(projectName).list();
		
		List<OpenshiftObject> convertedObjects = new ArrayList<OpenshiftObject>(fabric8ioList.getItems().size());
		for (Secret fabricio8Object : fabric8ioList.getItems()) {
			OpenshiftObject object = Fabric8ioConverterUtil.convertSecret(fabricio8Object);
			convertedObjects.add(object);
		}
		return convertedObjects;
	}

	@Override
	public List<OpenshiftObject> getServices(String projectName) {
		ServiceList fabric8ioList = osClient.services().inNamespace(projectName).list();
		
		List<OpenshiftObject> convertedObjects = new ArrayList<OpenshiftObject>(fabric8ioList.getItems().size());
		for (Service fabricio8Object : fabric8ioList.getItems()) {
			OpenshiftObject object = Fabric8ioConverterUtil.convertService(fabricio8Object);
			convertedObjects.add(object);
		}
		return convertedObjects;
	}
	
	@Override
	public List<OpenshiftObject> getServiceAccounts(String projectName) {
		ServiceAccountList fabric8ioList = osClient.serviceAccounts().inNamespace(projectName).list();
		
		List<OpenshiftObject> convertedObjects = new ArrayList<OpenshiftObject>(fabric8ioList.getItems().size());
		for (ServiceAccount fabricio8Object : fabric8ioList.getItems()) {
			OpenshiftObject object = Fabric8ioConverterUtil.convertServiceAccount(fabricio8Object);
			convertedObjects.add(object);
		}
		return convertedObjects;
	}
	
	@Override
	public List<OpenshiftObject> getRoutes(String projectName) {
		RouteList fabric8ioList = osClient.routes().inNamespace(projectName).list();
		
		List<OpenshiftObject> convertedObjects = new ArrayList<OpenshiftObject>(fabric8ioList.getItems().size());
		for (Route fabricio8Object : fabric8ioList.getItems()) {
			OpenshiftObject object = Fabric8ioConverterUtil.convertRoute(fabricio8Object);
			convertedObjects.add(object);
		}
		return convertedObjects;
	}
	
	@Override
	public List<OpenshiftObject> getRoles(String projectName) {
		RoleList fabric8ioList = osClient.roles().inNamespace(projectName).list();
		
		List<OpenshiftObject> convertedObjects = new ArrayList<OpenshiftObject>(fabric8ioList.getItems().size());
		for (Role fabricio8Object : fabric8ioList.getItems()) {
			OpenshiftObject object = Fabric8ioConverterUtil.convertRole(fabricio8Object);
			convertedObjects.add(object);
		}
		return convertedObjects;
	}
	
	//-----------------------------------------------
	// Generic object list accessor
	//-----------------------------------------------
	
	@Override
	public List<OpenshiftObject> getResources(String projectName, String kind) {
		switch (kind) {
		case "Pod":
			return getPods(projectName);
		case "DeploymentConfig":
			return getDeploymentConfigs(projectName);
		case "Job":
			return getJobs(projectName);
		case "ReplicationController":
			return getReplicationControllers(projectName);
		case "ConfigMap":
			return getConfigMaps(projectName);
		case "Service":
			return getServices(projectName);
		case "Secret":
			return getSecrets(projectName);
		case "ServiceAccount":
			return getServiceAccounts(projectName);
		case "RoleBinding":
			return getRoleBindings(projectName);
		case "Route":
			return getRoutes(projectName);
		case "Role":
			return getRoles(projectName);
		case "pod":
			return getPods(projectName);
		case "deploymentconfig":
			return getDeploymentConfigs(projectName);
		case "job":
			return getJobs(projectName);
		case "replicationcontroller":
			return getReplicationControllers(projectName);
		case "configmap":
			return getConfigMaps(projectName);
		case "service":
			return getServices(projectName);
		case "secret":
			return getSecrets(projectName);
		case "serviceaccount":
			return getServiceAccounts(projectName);
		case "rolebinding":
			return getRoleBindings(projectName);
		case "route":
			return getRoutes(projectName);
		case "role":
			return getRoles(projectName);
		default:
			throw new UnsupportedOperationException("Requested Openshift Resources of type '"+kind+"', but this was not supported by OpenshiftStatusFabric8ioClient");
		}
	}

	//-------------------------------------------------------------------
    // Get methods for lists of objects connected to other objects
	//-------------------------------------------------------------------

	@Override
	public List<OpenshiftObject> getPodsForJob(String projectName, String jobName) {
		return getPods(projectName, true, true, "job-name="+jobName);
	}

	@Override
	public List<OpenshiftObject> getPodsForDeploymentConfig(String projectName, String deploymentConfigName) {
		return getPods(projectName, true, true, "deploymentconfig="+deploymentConfigName);
	}

	

	@Override
	public void close() {
		osClient.close();
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
