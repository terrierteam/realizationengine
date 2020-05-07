package eu.bigdatastack.gdt.structures.openshift;

import java.util.Map;

public class ISelector {

	Map<String,String> matchLabels;
	
	public ISelector() {}

	public Map<String, String> getMatchLabels() {
		return matchLabels;
	}

	public void setMatchLabels(Map<String, String> matchLabels) {
		this.matchLabels = matchLabels;
	};
	
	
	
}
