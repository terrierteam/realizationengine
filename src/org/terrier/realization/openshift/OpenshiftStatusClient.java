package org.terrier.realization.openshift;

import java.util.List;

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
 * This is an interface that enables fronting of different connection libraries to Openshift/Kubernetes.
 * This was originally implemented as there is no official java client for Openshift 4, so we need to use
 * a third party library to support Openshift 4.x. The manager class will switch inderlying client based on
 * the config it is provided ('client' field in the Openshift part of the config). 
 *
 */
public interface OpenshiftStatusClient {

	/**
	 * Attempt to generate a connection to Openshift. Operations can be
	 * performed after this method is called (assuming it returns true.
	 * Under the hood this will authenticate with openshift and get a
	 * token,
	 * @return was successful?
	 */
	public boolean connectToOpenshift();


	/**
	 * Gets a Project object based on its name, note that this may not be functional for all
	 * clients, if the client assumes that the user it is impersonating is 'logged-in' to a
	 * particular namespace. 
	 * @param projectName
	 * @return
	 */
	public OpenshiftObject getProject(String projectName);

	/**
	 * Returns a list of Pod objects for a namespace, where they have been pre-filtered based
	 * on their current statuses
	 * @param projectName
	 * @param active
	 * @param ended
	 * @return
	 */
	public List<OpenshiftObject> getPods(String projectName, boolean active, boolean ended);


	/**
	 * Returns a list of Pod objects for a namespace, where the results have been filtered by
	 * status and by labels
	 * @param projectName
	 * @param active
	 * @param ended
	 * @param labelselector
	 * @return
	 */
	public List<OpenshiftObject> getPods(String projectName, boolean active, boolean ended, String labelselector);


	/**
	 * Returns a Pod object in a namespace with a particular name
	 * @param namespace
	 * @param name
	 * @return
	 */
	public OpenshiftObject getPod(String namespace, String name);

	/**
	 * Get all pods for a namespace
	 * @param projectName
	 * @return
	 */
	public List<OpenshiftObject> getPods(String projectName);



	/**
	 * Returns a list of DeploymentConfig objects from within a specified project
	 * @param project
	 * @return
	 */
	public List<OpenshiftObject> getDeploymentConfigs(String projectName);

	/**
	 * Returns a DeploymentConfig object from within a specified namespace with a given name
	 * @param projectName
	 * @param deploymentConfigName
	 * @return
	 */
	public OpenshiftObject getDeploymentConfig(String projectName, String deploymentConfigName);


	/**
	 * Get an object from a namespace with a specified name and kind
	 * @param project
	 * @param name
	 * @param kind
	 * @return
	 */
	public OpenshiftObject getResource(String projectName, String name, String kind);


	/**
	 * Get a list of objects from a namespace with a specified kind
	 * @param project
	 * @param kind
	 * @return
	 */
	public List<OpenshiftObject> getResources(String projectName, String kind);


	/**
	 * Gets a list of replication controllers for a project with a specified name
	 * @param projectName
	 * @param controllerName
	 * @return
	 */
	public OpenshiftObject getReplicationController(String projectName, String controllerName);

	/**
	 * Returns a list of Job objects for a namespace, where they have been pre-filtered based
	 * on their current statuses
	 * @param projectName
	 * @param active
	 * @param ended
	 * @return
	 */
	public List<OpenshiftObject> getJobs(String projectName, boolean active, boolean ended);

	/**
	 * Returns a Job object for a namsepace with a specified name
	 * @param project
	 * @param name
	 * @return
	 */
	public OpenshiftObject getJob(String projectName, String name);

	/**
	 * Returns a list of all jobs within a namespace
	 * @param projectName
	 * @return
	 */
	public List<OpenshiftObject> getJobs(String projectName);


	/**
	 * Get a list of Pods in a namespace that are associated to a particular Job
	 * @param project
	 * @param jobName
	 * @return
	 */
	public List<OpenshiftObject> getPodsForJob(String projectName, String jobName);


	/**
	 * Gets a list of Pods in a namespace that are associated to a particular DeploymentConfig
	 * @param projectName
	 * @param deploymentConfigName
	 * @return
	 */
	public List<OpenshiftObject> getPodsForDeploymentConfig(String projectName, String deploymentConfigName);


	/**
	 * Gets a ConfigMap with a specified name.
	 * @param projectName
	 * @param name
	 * @return
	 */
	public OpenshiftObject getConfigMap(String projectName, String name);

	/**
	 * Returns a Service from a namespace with a specified name.
	 * @param projectName
	 * @param name
	 * @return
	 */
	public OpenshiftObject getService(String projectName, String name);

	/**
	 * Gets a Secret from an namespace with a specified name
	 * @param projectName
	 * @param name
	 * @return
	 */
	public OpenshiftObject getSecret(String projectName, String name);

	/**
	 * Gets a ServiceAccount for a namespace with a specified name
	 * @param projectName
	 * @param name
	 * @return
	 */
	public OpenshiftObject getServiceAccount(String projectName, String name);

	/**
	 * Gets a RoleBinding (note! not a cluster role binding) for a namespace and with a specified name
	 * @param projectName
	 * @param name
	 * @return
	 */
	public OpenshiftObject getRoleBinding(String projectName, String name);

	/**
	 * Gets a Route within a namespace with a specified name
	 * @param projectName
	 * @param name
	 * @return
	 */
	public OpenshiftObject getRoute(String projectName, String name);


	/**
	 * Gets a Role from within a project with a specified name
	 * @param projectName
	 * @param name
	 * @return
	 */
	public OpenshiftObject getRole(String projectName, String name);

	/**
	 * Get all ConfigMaps in a namespace
	 * @param projectName
	 * @return
	 */
	public List<OpenshiftObject> getConfigMaps(String projectName);

	/**
	 * Get all ReplicationControllers in a namespace
	 * @param projectName
	 * @return
	 */
	public List<OpenshiftObject> getReplicationControllers(String projectName);

	/**
	 * Gets all RoleBindings for a particular namespace
	 * @param projectName
	 * @return
	 */
	public List<OpenshiftObject> getRoleBindings(String projectName);


	/**
	 * Gets all Secrets from a namespace
	 * @param projectName
	 * @return
	 */
	public List<OpenshiftObject> getSecrets(String projectName);


	/**
	 * Gets all Services for a namespace
	 * @param projectName
	 * @return
	 */
	public List<OpenshiftObject> getServices(String projectName);

	/**
	 * Gets all ServiceAccounts for a namespace
	 * @param projectName
	 * @return
	 */
	public List<OpenshiftObject> getServiceAccounts(String projectName);

	/**
	 * Gets all Routes for a namespace
	 * @param projectName
	 * @return
	 */
	public List<OpenshiftObject> getRoutes(String projectName);


	/**
	 * Gets all of the Roles for a namespace
	 * @param projectName
	 * @return
	 */
	public List<OpenshiftObject> getRoles(String projectName);

	/**
	 * Closes the connection to Openshift
	 */
	public void close();
}
