import java.io.File;

import eu.bigdatastack.gdt.application.GDTManager;
import eu.bigdatastack.gdt.openshift.OpenshiftObject;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationFabric8ioClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusFabric8ioClient;
import eu.bigdatastack.gdt.structures.config.GDTConfig;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.util.GDTFileUtil;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.AutoAdaptableKubernetesClient;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigList;
import io.fabric8.openshift.api.model.DoneableDeploymentConfig;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.dsl.DeployableScalableResource;

public class fabricIOOpenshiftClientTest {

	public static void main(String[] args) throws Exception {
		
		GDTConfig gdtconfig = new GDTConfig(new File("boston.gdt.config.json"));
		
		/*System.err.println("URL: "+gdtconfig.getOpenshift().getHost()+":"+gdtconfig.getOpenshift().getPort());
		System.err.println("Username: "+gdtconfig.getOpenshift().getUsername());
		System.err.println("Password: "+gdtconfig.getOpenshift().getPassword());
		
		Config config = new ConfigBuilder()
				.withMasterUrl(gdtconfig.getOpenshift().getHost()+":"+gdtconfig.getOpenshift().getPort())
				.withUsername(gdtconfig.getOpenshift().getUsername())
				.withPassword(gdtconfig.getOpenshift().getPassword())
				.withNamespace("realization")
				//.withOauthToken("6xiF5Ih7lK68trzeQz8aSB_k9ocOleyy3QAhB-bkYjw")
				.withTrustCerts(true)
				.build();
		
		
		OpenShiftClient osClient = new DefaultOpenShiftClient(config);
		System.err.println(osClient.getApiVersion());
		
		MixedOperation<DeploymentConfig, DeploymentConfigList, DoneableDeploymentConfig, DeployableScalableResource<DeploymentConfig, DoneableDeploymentConfig>> myNs = osClient.deploymentConfigs();
		
		for (DeploymentConfig dc : myNs.list().getItems()) {
			System.err.println(dc.getMetadata().getName());
		}
		
		osClient.close();*/
		
		
		/*OpenshiftStatusClient ocClient = new OpenshiftStatusFabric8ioClient(
				gdtconfig.getOpenshift().getHost(),
				gdtconfig.getOpenshift().getPort(),
				gdtconfig.getOpenshift().getUsername(),
				gdtconfig.getOpenshift().getPassword());
		ocClient.connectToOpenshift();
		
		
		OpenshiftObject object = ocClient.getPod("realization", "realizationcli-1-kcmjm");
		
		object = ocClient.getPod("realization", "realizationcli-1-kcmjm");
		
		System.err.println(object.ifPodGetContainers().get(0).getRequestCPU());
		
		ocClient.close();*/
		
		GDTManager manager = new GDTManager(gdtconfig);
		
		//BigDataStackObjectDefinition pod = GDTFileUtil.readObjectFromString(GDTFileUtil.file2String(new File("resources/boston/test/helloWorld.pod.yaml"), "UTF-8"));
		
		//manager.openshiftOperationClient.applyOperation(pod);
		
		BigDataStackObjectDefinition dc = GDTFileUtil.readObjectFromString(GDTFileUtil.file2String(new File("resources/boston/test/gdtmonitor.dc.yaml"), "UTF-8"), null);
		
		manager.openshiftOperationClient.applyOperation(dc);
		//object = ocClient2.applyOperation(object)
		
		
		manager.shutdown();
		
	}
	
}
