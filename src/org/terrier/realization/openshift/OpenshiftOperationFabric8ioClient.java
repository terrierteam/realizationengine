package org.terrier.realization.openshift;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackObjectType;
import org.terrier.realization.structures.data.BigDataStackPodStatus;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.client.Callback;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.utils.InputStreamPumper;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.Role;
import io.fabric8.openshift.api.model.RoleBinding;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import okhttp3.Response;

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

@SuppressWarnings("deprecation")
public class OpenshiftOperationFabric8ioClient implements OpenshiftOperationClient{

	final String client = "fabric8io";

	String host;
	int port;
	String username;
	String password;
	String namespace;

	OpenShiftClient osClient;
	OpenshiftStatusFabric8ioClient statusClient;

	public OpenshiftOperationFabric8ioClient(String host, int port, String username, String password, String namespace) {
		super();
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.namespace= namespace;
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
			statusClient = new OpenshiftStatusFabric8ioClient(osClient);
			statusClient.host = host;
			statusClient.port = port;
			statusClient.username = username;
			statusClient.password = password;

			Thread.sleep(3000);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}


	@Override
	public boolean applyOperation(BigDataStackObjectDefinition object) {

		try {
			//OpenshiftObject project = statusClient.getProject(object.getNamespace());
			//if (project==null) return false;

			switch (object.getType()) {
			case DeploymentConfig:
			{ByteArrayInputStream stream = new ByteArrayInputStream(object.getYamlSource().getBytes());
			osClient.load(stream).createOrReplace();
			stream.close();}

			osClient.deploymentConfigs().inNamespace(object.getNamespace()).withName(object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance()).deployLatest(true);

			return true;
			case Service:
			{ByteArrayInputStream stream = new ByteArrayInputStream(object.getYamlSource().getBytes());
			osClient.load(stream).createOrReplace();
			stream.close();}
			return true;
			case Job:
			{ByteArrayInputStream stream = new ByteArrayInputStream(object.getYamlSource().getBytes());
			osClient.load(stream).createOrReplace();
			stream.close();}
			return true;
			case Route:
			{ByteArrayInputStream stream = new ByteArrayInputStream(object.getYamlSource().getBytes());
			osClient.load(stream).createOrReplace();
			stream.close();}
			return true;
			case Pod:
			{ByteArrayInputStream stream = new ByteArrayInputStream(object.getYamlSource().getBytes());
			osClient.load(stream).createOrReplace();
			stream.close();}
			return true;
			case Secret:
			{ByteArrayInputStream stream = new ByteArrayInputStream(object.getYamlSource().getBytes());
			osClient.load(stream).createOrReplace();
			stream.close();}
			return true;
			case ConfigMap:
			{ByteArrayInputStream stream = new ByteArrayInputStream(object.getYamlSource().getBytes());
			osClient.load(stream).createOrReplace();
			stream.close();}
			return true;
			case ServiceAccount:
			{ByteArrayInputStream stream = new ByteArrayInputStream(object.getYamlSource().getBytes());
			osClient.load(stream).createOrReplace();
			stream.close();}
			return true;
			case RoleBinding:
			{ByteArrayInputStream stream = new ByteArrayInputStream(object.getYamlSource().getBytes());
			osClient.load(stream).createOrReplace();
			stream.close();}
			return true;
			case Role:
			{ByteArrayInputStream stream = new ByteArrayInputStream(object.getYamlSource().getBytes());
			osClient.load(stream).createOrReplace();
			stream.close();}
			return true;
			case Volume:
			{ByteArrayInputStream stream = new ByteArrayInputStream(object.getYamlSource().getBytes());
			osClient.load(stream).createOrReplace();
			stream.close();}
			return true;
			case VolumeClaim:
			{ByteArrayInputStream stream = new ByteArrayInputStream(object.getYamlSource().getBytes());
			osClient.load(stream).createOrReplace();
			stream.close();}
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

	@Override
	public boolean deleteOperation(BigDataStackObjectDefinition object) {
		try {
			//OpenshiftObject project = statusClient.getProject(object.getNamespace());
			//if (project==null) return false;

			switch (object.getType()) {
			case DeploymentConfig:

				OpenshiftObject openshiftObject = statusClient.getDeploymentConfig(object.getNamespace(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.deploymentConfigs().delete((DeploymentConfig)openshiftObject.getUnderlyingClientObject());

				Thread.sleep(3000);
				
				// deleting a deployment config does not delete the underlying replication controller, so delete that too
				/*OpenshiftObject controller = statusClient.getReplicationController(object.getNamespace(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.replicationControllers().delete((ReplicationController)controller.getUnderlyingClientObject());*/

				List<OpenshiftObject> pods1 = statusClient.getPods(object.getNamespace(), true, true, "deploymentconfig="+object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				for (OpenshiftObject pod : pods1) {
					osClient.pods().delete((Pod)pod.getUnderlyingClientObject());
				}

				return true;
			case Service:
				OpenshiftObject openshiftObjectService = statusClient.getService(object.getNamespace(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.services().delete((Service)openshiftObjectService.getUnderlyingClientObject());
				return true;
			case Job:
				OpenshiftObject openshiftObjectJob = statusClient.getJob(object.getNamespace(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());

				List<OpenshiftObject> pods = statusClient.getPodsForJob(object.getNamespace(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());

				osClient.batch().jobs().delete((Job)openshiftObjectJob.getUnderlyingClientObject());
				for (OpenshiftObject pod : pods) {
					osClient.pods().delete((Pod)pod.getUnderlyingClientObject());
				}
				return true;
			case Route:
				OpenshiftObject openshiftObjectRoute = statusClient.getRoute(object.getNamespace(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.routes().delete((Route)openshiftObjectRoute.getUnderlyingClientObject());
				return true;
			case Pod:
				OpenshiftObject openshiftObjectPod = statusClient.getPod(object.getNamespace(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.pods().delete((Pod)openshiftObjectPod.getUnderlyingClientObject());
				return true;
			case Secret:
				OpenshiftObject openshiftObjectSecret = statusClient.getSecret(object.getNamespace(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.secrets().delete((Secret)openshiftObjectSecret.getUnderlyingClientObject());
				return true;
			case ConfigMap:
				OpenshiftObject openshiftObjectConfigMap = statusClient.getConfigMap(object.getNamespace(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.configMaps().delete((ConfigMap)openshiftObjectConfigMap.getUnderlyingClientObject());
				return true;
			case ServiceAccount:
				OpenshiftObject openshiftObjectServiceAccount = statusClient.getServiceAccount(object.getNamespace(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.serviceAccounts().delete((ServiceAccount)openshiftObjectServiceAccount.getUnderlyingClientObject());
				return true;
			case RoleBinding:
				OpenshiftObject openshiftObjectRoleBinding = statusClient.getRoleBinding(object.getNamespace(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.roleBindings().delete((RoleBinding)openshiftObjectRoleBinding.getUnderlyingClientObject());
				return true;
			case Role:
				OpenshiftObject openshiftObjectRole = statusClient.getRole(object.getNamespace(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.roles().delete((Role)openshiftObjectRole.getUnderlyingClientObject());
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
	
	
	public Map<String,Map<String,String>> getLogs(BigDataStackObjectDefinition object) {
		
		Map<String,Map<String,String>> logs = new HashMap<String,Map<String,String>>();
		
		if (object.getType()==BigDataStackObjectType.DeploymentConfig) {
			List<OpenshiftObject> pods1 = statusClient.getPods(object.getNamespace(), true, true, "deploymentconfig="+object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
			if (pods1.size()==0) {
				pods1 = statusClient.getPods(object.getNamespace(), true, true, "jobconfig="+object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
			}
			System.out.println(pods1.size());
			for (OpenshiftObject pod : pods1) {
				System.out.println(pod.getName());
				
				if (!logs.containsKey(pod.getName())) logs.put(pod.getName(), new HashMap<String,String>());
				
				List<OpenshiftContainer> containers = pod.ifPodGetContainers();
				for (OpenshiftContainer container : containers) {
					System.out.println(container.getName());
					String log = osClient.pods().inNamespace(namespace).withName(pod.getName()).inContainer(container.getName()).withPrettyOutput().getLog();
					
					logs.get(pod.getName()).put(container.getName(), log);
				}
				
			}
		} else if (object.getType()==BigDataStackObjectType.Pod) {
			OpenshiftObject pod = statusClient.getPod(namespace, object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
			
			System.out.println(pod.getName());
			
			if (!logs.containsKey(pod.getName())) logs.put(pod.getName(), new HashMap<String,String>());
			
			List<OpenshiftContainer> containers = pod.ifPodGetContainers();
			for (OpenshiftContainer container : containers) {
				System.out.println(container.getName());
				String log = osClient.pods().inNamespace(namespace).withName(pod.getName()).inContainer(container.getName()).withPrettyOutput().getLog();
				
				logs.get(pod.getName()).put(container.getName(), log);
			}
			
		} else if (object.getType()==BigDataStackObjectType.Job) {
			
			System.err.println("job-name="+object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
			List<OpenshiftObject> pods1 = statusClient.getPods(object.getNamespace(), true, true, "job-name="+object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
			if (pods1.size()==0) {
				pods1 = statusClient.getPods(object.getNamespace(), true, true, "jobconfig="+object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
			}
			System.out.println(pods1.size());
			for (OpenshiftObject pod : pods1) {
				System.out.println(pod.getName());
				
				if (!logs.containsKey(pod.getName())) logs.put(pod.getName(), new HashMap<String,String>());
				
				List<OpenshiftContainer> containers = pod.ifPodGetContainers();
				for (OpenshiftContainer container : containers) {
					System.out.println(container.getName());
					String log = osClient.pods().inNamespace(namespace).withName(pod.getName()).inContainer(container.getName()).withPrettyOutput().getLog();
					
					logs.get(pod.getName()).put(container.getName(), log);
				}
				
			}
		} else {
			return null;
		}
		
		
		
		return logs;
		
		
		
		
	}

	@Override
	public void close() {
		osClient.close();

	}

	public OpenShiftClient getOsClient() {
		return osClient;
	}

	@Override
	public List<String> execCommands(BigDataStackPodStatus pod, String[][] commands) {

		return execCommands(pod.getPodID(), commands);
	}
	
	
	public List<String> execCommands(String podName, String[][] commands) {
		
		List<String> commandOutput = new ArrayList<String>();
		
		ScheduledExecutorService executorService = Executors.newScheduledThreadPool(20);

		try {
			int i =0;
			for (String[] command : commands) {
				ExecWatch watch = null;
				InputStreamPumper pump = null;
				InputStreamPumper pumpErr = null;
				final CountDownLatch latch = new CountDownLatch(1);
				watch = osClient.pods().withName(podName).redirectingOutput().usingListener(new ExecListener() {
					@Override
					public void onOpen(Response response) {
					}

					@Override
					public void onFailure(Throwable t, Response response) {
						latch.countDown();
					}

					@Override
					public void onClose(int code, String reason) {
						latch.countDown();
					}
				}).exec(command);
				SystemOutCallback callback = new SystemOutCallback();
				pump = new InputStreamPumper(watch.getOutput(), callback);
				pumpErr = new InputStreamPumper(watch.getError(), callback);
				executorService.submit(pump);
				executorService.submit(pumpErr);
				Future<String> outPumpFuture = executorService.submit(pump, "Done");
				executorService.scheduleAtFixedRate(new FutureChecker("Command " + (i + 1), outPumpFuture), 0, 2, TimeUnit.SECONDS);

				latch.await(10, TimeUnit.SECONDS);
				//We need to wait or the pumper (ws -> System.out) will not be able to print the message.
				//Thread.sleep(1000);
				commandOutput.add(callback.getOutput());
				
				watch.close();
				pump.close();
				pumpErr.close();
				i++;

			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}


		executorService.shutdown();
		//System.out.println("Done.");
		return commandOutput;
	}


	public OpenshiftStatusFabric8ioClient getStatusClient() {
		return statusClient;
	}


	private static class SystemOutCallback implements Callback<byte[]> {
		
		StringBuilder builder = new StringBuilder();
		
		@Override
		public void call(byte[] data) {
			builder.append(new String(data));
			builder.append('\n');
		}
		
		public String getOutput() {
			return builder.toString();
		}
	}

	private static class FutureChecker implements Runnable {
		private final String name;
		private final Future<String> future;

		private FutureChecker(String name, Future<String> future) {
			this.name = name;
			this.future = future;
		}

		@Override
		public void run() {
			if(!future.isDone()) {
				//
			System.out.println("Future:[" + name + "] is not done yet");
			}
		}
	}



}
