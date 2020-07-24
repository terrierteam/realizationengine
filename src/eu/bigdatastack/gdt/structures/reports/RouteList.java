package eu.bigdatastack.gdt.structures.reports;

import java.util.Map;

public class RouteList {

	Map<String,Map<String,String>> url2Desc;

	public RouteList(Map<String, Map<String, String>> url2Desc) {
		super();
		this.url2Desc = url2Desc;
	}

	public Map<String, Map<String, String>> getUrl2Desc() {
		return url2Desc;
	}

	public void setUrl2Desc(Map<String, Map<String, String>> url2Desc) {
		this.url2Desc = url2Desc;
	}

	
	
	
}
