
import java.util.List;
import java.util.Set;

import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;

import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.structures.openshift.IJob;
import eu.bigdatastack.gdt.util.OpenshiftUtil;

public class OpenshiftStatusTest {

	public static void main(String[] args) {
		OpenshiftStatusClient statusClient = new OpenshiftStatusClient("https://idagpu-head.dcs.gla.ac.uk:8443/", "admin", "IDAAdmin2019");
		statusClient.connectToOpenshift();
		
		IProject project = statusClient.getProject("richardmproject");
		
		IJob job = statusClient.getJob(project, "job-appsimulator-flinksim");
		
		System.err.println(job.getStatus().toString());
		
		IProject project2 = statusClient.getProject("carolproject");
		
		List<IPod> jobPods = statusClient.getPodsForJob(project2, "autofocus-test-9");
		for (IPod pod : jobPods) {
			System.err.println(pod.getName());
		}
		
		IDeploymentConfig dc = statusClient.getDeploymentConfig(project, "dc-gdt-lx-store");
		Set<String> statuses = OpenshiftUtil.getDeploymentConfigStatuses(dc);
		for (String s : statuses) {
			System.err.println(s);
		}
		
		statusClient.close();
	}
	
	

}
