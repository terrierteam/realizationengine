
import java.util.List;
import java.util.Set;

import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;

import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClientv3;
import eu.bigdatastack.gdt.structures.openshift.IJob;
import eu.bigdatastack.gdt.util.OpenshiftUtil;

public class OpenshiftStatusTest {

	public static void main(String[] args) {
		OpenshiftStatusClientv3 statusClient = new OpenshiftStatusClientv3("https://api.moc.bigdatastack.com:6443", "bigdatastack", "Bi6_d4ta_5tacK");
		statusClient.connectToOpenshift();
		
		IProject project = statusClient.getProject("realization");	
		
		IDeploymentConfig dc = statusClient.getDeploymentConfig(project, "realizationstatedb");
		
		System.err.println(dc.toJson().toString());
		
		/*IProject project2 = statusClient.getProject("carolproject");
		
		List<IPod> jobPods = statusClient.getPodsForJob(project2, "autofocus-test-9");
		for (IPod pod : jobPods) {
			System.err.println(pod.getName());
		}
		
		IDeploymentConfig dc = statusClient.getDeploymentConfig(project, "dc-gdt-lx-store");
		Set<String> statuses = OpenshiftUtil.getDeploymentConfigStatuses(dc);
		for (String s : statuses) {
			System.err.println(s);
		}
		
		statusClient.close();*/
	}
	
	

}
