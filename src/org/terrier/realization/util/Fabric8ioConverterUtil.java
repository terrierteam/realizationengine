package org.terrier.realization.util;

import java.util.HashSet;
import java.util.Set;

import org.terrier.realization.openshift.OpenshiftObject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobCondition;
import io.fabric8.openshift.api.model.DeploymentCondition;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.Role;
import io.fabric8.openshift.api.model.RoleBinding;
import io.fabric8.openshift.api.model.Route;

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
 * This class performs the conversion between Fabric8io objects and generic OpenshiftObject
 * objects. This is to enable abstraction from the Openshift client being used.
 *
 */
public class Fabric8ioConverterUtil {

	public static final String clientName = "fabric8io";
	
	public static OpenshiftObject convertPod(Pod pod) {
		
		OpenshiftObject object = new OpenshiftObject();
		object.setClient(clientName);
		object.setName(pod.getMetadata().getName());
		object.setType(pod.getKind());
		object.setLabels(pod.getMetadata().getLabels());
		object.setUnderlyingClientObject(pod);
		
		Set<String> statuses = new HashSet<String>();
		/*for (ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
			statuses.add(containerStatus.getName());
		}*/
		statuses.add(pod.getStatus().getPhase());
		object.setStatuses(statuses);
		
		return object;
		
	}
	
	public static OpenshiftObject convertProject(Project project) {
		OpenshiftObject object = new OpenshiftObject();
		object.setName(project.getMetadata().getName());
		object.setClient(clientName);
		object.setType(project.getKind());
		object.setUnderlyingClientObject(project);
		return object;
	}
	
	public static OpenshiftObject convertDeploymentConfig(DeploymentConfig dc) {
		OpenshiftObject object = new OpenshiftObject();
		object.setClient(clientName);
		object.setName(dc.getMetadata().getName());
		object.setType(dc.getKind());
		object.setLabels(dc.getMetadata().getLabels());
		object.setUnderlyingClientObject(dc);
		
		Set<String> statuses = new HashSet<String>();
		for (DeploymentCondition condition : dc.getStatus().getConditions()) {
			if (condition.getStatus().equalsIgnoreCase("True")) statuses.add(condition.getType());
		}
		object.setStatuses(statuses);
		
		return object;
	}
	
	public static OpenshiftObject convertJob(Job job) {
		OpenshiftObject object = new OpenshiftObject();
		object.setClient(clientName);
		object.setName(job.getMetadata().getName());
		object.setType(job.getKind());
		object.setLabels(job.getMetadata().getLabels());
		object.setUnderlyingClientObject(job);
		
		Set<String> statuses = new HashSet<String>();
		for (JobCondition condition : job.getStatus().getConditions()) {
			if (condition.getStatus().equalsIgnoreCase("True")) statuses.add(condition.getType());
		}
		
		if (statuses.size()==0) {
			if (job.getStatus()!=null) {
				if (job.getStatus().getActive()>0) statuses.add("In Progress");
			} 
		}
		
		if (statuses.size()==0) statuses.add("Unknown");
		
		object.setStatuses(statuses);
		
		return object;
	}
	
	public static OpenshiftObject convertReplicationController(ReplicationController rc) {
		OpenshiftObject object = new OpenshiftObject();
		object.setName(rc.getMetadata().getName());
		object.setClient(clientName);
		object.setType(rc.getKind());
		object.setLabels(rc.getMetadata().getLabels());
		object.setUnderlyingClientObject(rc);
		return object;
	}
	
	public static OpenshiftObject convertConfigMap(ConfigMap fabric8ioObject) {
		OpenshiftObject object = new OpenshiftObject();
		object.setName(fabric8ioObject.getMetadata().getName());
		object.setClient(clientName);
		object.setType(fabric8ioObject.getKind());
		object.setLabels(fabric8ioObject.getMetadata().getLabels());
		object.setUnderlyingClientObject(fabric8ioObject);
		return object;
	}
	
	public static OpenshiftObject convertService(Service fabric8ioObject) {
		OpenshiftObject object = new OpenshiftObject();
		object.setName(fabric8ioObject.getMetadata().getName());
		object.setClient(clientName);
		object.setType(fabric8ioObject.getKind());
		object.setLabels(fabric8ioObject.getMetadata().getLabels());
		object.setUnderlyingClientObject(fabric8ioObject);
		return object;
	}
	
	public static OpenshiftObject convertSecret(Secret fabric8ioObject) {
		OpenshiftObject object = new OpenshiftObject();
		object.setName(fabric8ioObject.getMetadata().getName());
		object.setClient(clientName);
		object.setType(fabric8ioObject.getKind());
		object.setLabels(fabric8ioObject.getMetadata().getLabels());
		object.setUnderlyingClientObject(fabric8ioObject);
		return object;
	}
	
	public static OpenshiftObject convertServiceAccount(ServiceAccount fabric8ioObject) {
		OpenshiftObject object = new OpenshiftObject();
		object.setName(fabric8ioObject.getMetadata().getName());
		object.setClient(clientName);
		object.setType(fabric8ioObject.getKind());
		object.setLabels(fabric8ioObject.getMetadata().getLabels());
		object.setUnderlyingClientObject(fabric8ioObject);
		return object;
	}
	
	public static OpenshiftObject convertRoleBinding(RoleBinding fabric8ioObject) {
		OpenshiftObject object = new OpenshiftObject();
		object.setName(fabric8ioObject.getMetadata().getName());
		object.setClient(clientName);
		object.setType(fabric8ioObject.getKind());
		object.setLabels(fabric8ioObject.getMetadata().getLabels());
		object.setUnderlyingClientObject(fabric8ioObject);
		return object;
	}
	
	public static OpenshiftObject convertRoute(Route fabric8ioObject) {
		OpenshiftObject object = new OpenshiftObject();
		object.setName(fabric8ioObject.getMetadata().getName());
		object.setClient(clientName);
		object.setType(fabric8ioObject.getKind());
		object.setLabels(fabric8ioObject.getMetadata().getLabels());
		object.setUnderlyingClientObject(fabric8ioObject);
		return object;
	}
	
	public static OpenshiftObject convertRole(Role fabric8ioObject) {
		OpenshiftObject object = new OpenshiftObject();
		object.setName(fabric8ioObject.getMetadata().getName());
		object.setClient(clientName);
		object.setType(fabric8ioObject.getKind());
		object.setLabels(fabric8ioObject.getMetadata().getLabels());
		object.setUnderlyingClientObject(fabric8ioObject);
		return object;
	}
	
}
