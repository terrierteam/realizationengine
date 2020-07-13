package eu.bigdatastack.gdt.openshift;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IPod;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;

/**
 * This is an internal representation of an object exposed by an Openshift Client.
 * It is designed to be as generic and minimal as possible, as different clients provide
 * different levels of information access on the objects. 
 * @author EbonBlade
 *
 */
public class OpenshiftObject {
	
	String name;
	String type;
	String client;
	Object underlyingClientObject;
	Map<String,String> labels;
	Set<String> statuses;
	
	public OpenshiftObject() {
		statuses = new HashSet<String>();
		labels = new HashMap<String,String>();
	}
	
	public OpenshiftObject(String name, String type, String client, Object underlyingClientObject,
			Map<String, String> labels, Set<String> statuses) {
		super();
		this.name = name;
		this.type = type;
		this.client = client;
		this.underlyingClientObject = underlyingClientObject;
		this.labels = labels;
		this.statuses = statuses;
	}



	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public Object getUnderlyingClientObject() {
		return underlyingClientObject;
	}

	public void setUnderlyingClientObject(Object underlyingClientObject) {
		this.underlyingClientObject = underlyingClientObject;
	}

	public Set<String> getStatuses() {
		return statuses;
	}

	public void setStatuses(Set<String> statuses) {
		this.statuses = statuses;
	}
	
	public Map<String, String> getLabels() {
		return labels;
	}

	public void setLabels(Map<String, String> labels) {
		this.labels = labels;
	}

	
	
	//--------------------------------------------
	// Pod-Specific Methods
	//--------------------------------------------

	public List<OpenshiftContainer> ifPodGetContainers() {
		List<OpenshiftContainer> containers = new ArrayList<OpenshiftContainer>();
		if (underlyingClientObject instanceof IPod) { // openshiftv3
			IPod pod = (IPod)underlyingClientObject;
			for (IContainer container : pod.getContainers()) {
				try {
					Map<String,String> requests = new HashMap<String,String>();
					if (container.getRequestsCPU()!=null) requests.put("cpu", container.getRequestsCPU());
					if (container.getRequestsMemory()!=null) requests.put("cpu", container.getRequestsMemory());
					OpenshiftContainer containerObject = new OpenshiftContainer(container.getName(), requests);
					containers.add(containerObject);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return containers;
		}
		
		if (underlyingClientObject instanceof Pod) { //fabric8io
			Pod pod = (Pod)underlyingClientObject;
			for (Container container : pod.getSpec().getContainers()) {
				try {
					Map<String,String> requests = new HashMap<String,String>();
					Map<String,Quantity> fabric8ioRequests = container.getResources().getRequests();
					for (String k : fabric8ioRequests.keySet()) {
						System.err.println("-- "+k+" "+fabric8ioRequests.get(k).getAmount());
						requests.put(k, fabric8ioRequests.get(k).getAmount());
					}
					OpenshiftContainer containerObject = new OpenshiftContainer(container.getName(), requests);
					containers.add(containerObject);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return containers;
		}
		
		return null;
	}
	
	public String ifPodGetIP() {
		if (underlyingClientObject instanceof IPod) return ((IPod)underlyingClientObject).getIP(); // openshiftv3
		if (underlyingClientObject instanceof Pod) return  ((Pod)underlyingClientObject).getStatus().getPodIP(); //fabric8io
		return null;
	}
	
	public String ifPodGetHost() {
		if (underlyingClientObject instanceof IPod) return ((IPod)underlyingClientObject).getHost(); // openshiftv3
		if (underlyingClientObject instanceof Pod) return  ((Pod)underlyingClientObject).getStatus().getHostIP(); //fabric8io
		return null;
	}
	
}
