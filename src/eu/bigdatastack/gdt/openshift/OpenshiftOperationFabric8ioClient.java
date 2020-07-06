package eu.bigdatastack.gdt.openshift;

import java.io.ByteArrayInputStream;
import java.util.List;


import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.Role;
import io.fabric8.openshift.api.model.RoleBinding;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

public class OpenshiftOperationFabric8ioClient implements OpenshiftOperationClient{

	final String client = "fabric8io";

	String host;
	int port;
	String username;
	String password;

	Config config;
	
	OpenShiftClient osClient;
	OpenshiftStatusFabric8ioClient statusClient;

	public OpenshiftOperationFabric8ioClient(String host, int port, String username, String password) {
		super();
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	@Override
	public boolean connectToOpenshift() {
		try {
			config = new ConfigBuilder()
					.withMasterUrl(host+":"+port)
					.withUsername(username)
					.withPassword(password)
					.withTrustCerts(true)
					.build();

			osClient = new DefaultOpenShiftClient(config);
			statusClient = new OpenshiftStatusFabric8ioClient(osClient);
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
				osClient.load(stream).createOrReplaceAnd();
				stream.close();}
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

				// deleting a deployment config does not delete the underlying replication controller, so delete that too
				OpenshiftObject controller = statusClient.getReplicationController(object.getNamespace(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.replicationControllers().delete((ReplicationController)controller.getUnderlyingClientObject());

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

	@Override
	public void close() {
		osClient.close();

	}

	public OpenShiftClient getOsClient() {
		return osClient;
	}

	

}
