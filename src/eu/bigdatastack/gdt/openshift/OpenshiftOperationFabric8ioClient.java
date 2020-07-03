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
			Config config = new ConfigBuilder()
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
			OpenshiftObject project = statusClient.getProject(object.getNamespace());
			if (project==null) return false;

			switch (object.getType()) {
			case DeploymentConfig:
				DeploymentConfig dc = (DeploymentConfig)osClient.load(new ByteArrayInputStream(object.getYamlSource().getBytes()));
				osClient.deploymentConfigs().create(dc);

				return true;
			case Service:
				Service service = (Service)osClient.load(new ByteArrayInputStream(object.getYamlSource().getBytes()));
				osClient.services().create(service);
				return true;
			case Job:
				Job job = (Job)osClient.load(new ByteArrayInputStream(object.getYamlSource().getBytes()));
				osClient.batch().jobs().create(job);
				return true;
			case Route:
				Route route = (Route)osClient.load(new ByteArrayInputStream(object.getYamlSource().getBytes()));
				osClient.routes().create(route);
				return true;
			case Pod:
				Pod pod = (Pod)osClient.load(new ByteArrayInputStream(object.getYamlSource().getBytes()));
				osClient.pods().create(pod);
				return true;
			case Secret:
				Secret secret = (Secret)osClient.load(new ByteArrayInputStream(object.getYamlSource().getBytes()));
				osClient.secrets().create(secret);
				return true;
			case ConfigMap:
				ConfigMap cm = (ConfigMap)osClient.load(new ByteArrayInputStream(object.getYamlSource().getBytes()));
				osClient.configMaps().create(cm);
				return true;
			case ServiceAccount:
				ServiceAccount sa = (ServiceAccount)osClient.load(new ByteArrayInputStream(object.getYamlSource().getBytes()));
				osClient.serviceAccounts().create(sa);
				return true;
			case RoleBinding:
				RoleBinding rb = (RoleBinding)osClient.load(new ByteArrayInputStream(object.getYamlSource().getBytes()));
				osClient.roleBindings().create(rb);
				return true;
			case Role:
				Role r = (Role)osClient.load(new ByteArrayInputStream(object.getYamlSource().getBytes()));
				osClient.roles().create(r);
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
			OpenshiftObject project = statusClient.getProject(object.getNamespace());
			if (project==null) return false;

			switch (object.getType()) {
			case DeploymentConfig:

				OpenshiftObject openshiftObject = statusClient.getDeploymentConfig(project.getName(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.deploymentConfigs().delete((DeploymentConfig)openshiftObject.getUnderlyingClientObject());

				// deleting a deployment config does not delete the underlying replication controller, so delete that too
				OpenshiftObject controller = statusClient.getReplicationController(project.getName(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.replicationControllers().delete((ReplicationController)controller.getUnderlyingClientObject());

				List<OpenshiftObject> pods1 = statusClient.getPods(project.getName(), true, true, "deploymentconfig="+object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				for (OpenshiftObject pod : pods1) {
					osClient.pods().delete((Pod)pod.getUnderlyingClientObject());
				}

				return true;
			case Service:
				OpenshiftObject openshiftObjectService = statusClient.getService(project.getName(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.services().delete((Service)openshiftObjectService.getUnderlyingClientObject());
				return true;
			case Job:
				OpenshiftObject openshiftObjectJob = statusClient.getJob(project.getName(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());

				List<OpenshiftObject> pods = statusClient.getPodsForJob(project.getName(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());

				osClient.batch().jobs().delete((Job)openshiftObjectJob.getUnderlyingClientObject());
				for (OpenshiftObject pod : pods) {
					osClient.pods().delete((Pod)pod.getUnderlyingClientObject());
				}
				return true;
			case Route:
				OpenshiftObject openshiftObjectRoute = statusClient.getRoute(project.getName(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.routes().delete((Route)openshiftObjectRoute.getUnderlyingClientObject());
				return true;
			case Pod:
				OpenshiftObject openshiftObjectPod = statusClient.getPod(project.getName(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.pods().delete((Pod)openshiftObjectPod.getUnderlyingClientObject());
				return true;
			case Secret:
				OpenshiftObject openshiftObjectSecret = statusClient.getSecret(project.getName(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.secrets().delete((Secret)openshiftObjectSecret.getUnderlyingClientObject());
				return true;
			case ConfigMap:
				OpenshiftObject openshiftObjectConfigMap = statusClient.getConfigMap(project.getName(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.configMaps().delete((ConfigMap)openshiftObjectConfigMap.getUnderlyingClientObject());
				return true;
			case ServiceAccount:
				OpenshiftObject openshiftObjectServiceAccount = statusClient.getServiceAccount(project.getName(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.serviceAccounts().delete((ServiceAccount)openshiftObjectServiceAccount.getUnderlyingClientObject());
				return true;
			case RoleBinding:
				OpenshiftObject openshiftObjectRoleBinding = statusClient.getRoleBinding(project.getName(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
				osClient.roleBindings().delete((RoleBinding)openshiftObjectRoleBinding.getUnderlyingClientObject());
				return true;
			case Role:
				OpenshiftObject openshiftObjectRole = statusClient.getRole(project.getName(), object.getAppID()+"-"+object.getObjectID()+"-"+object.getInstance());
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

}
