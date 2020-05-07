package eu.bigdatastack.gdt.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openshift.restclient.model.IDeploymentConfig;

/**
 * Static utility methods for parsing json responses from the openshift client
 * @author EbonBlade
 *
 */
public class OpenshiftUtil {

	/**
	 * Returns a list of the status strings for a deployment config
	 * @param deploymentConfig
	 * @return
	 */
	public static Set<String> getDeploymentConfigStatuses(IDeploymentConfig deploymentConfig) {
		
		ObjectMapper mapper = new ObjectMapper();
		
		Set<String> statuses = new HashSet<String>();
		try {
			JsonNode root = mapper.readTree(deploymentConfig.toJson());
			if (root.has("status")) {
				JsonNode status = root.get("status");
				
				if (status!=null && status.has("conditions")) {
					Iterator<JsonNode> conditionIterator = status.get("conditions").iterator();
					while (conditionIterator.hasNext()) {
						JsonNode condition = conditionIterator.next();
						statuses.add(condition.get("type").asText());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		if (statuses.size()==0) statuses.add("Unknown");
		
		return statuses;
	}
	
}
