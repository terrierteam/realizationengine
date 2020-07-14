package eu.bigdatastack.gdt.prometheus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bigdatastack.gdt.structures.data.BigDataStackMetricValue;

public class PrometheusDataClient {

	String hostExtension;
	
	public PrometheusDataClient(String hostExtension) {
		this.hostExtension = hostExtension;
	}
	
	ObjectMapper mapper = new ObjectMapper();
	
	
	/**
	 * Updates the value and lastUpdated fields of the provided metric value based on the current
	 * prometheus server data. (does not update the database however)
	 * @param existingValue
	 */
	public boolean update(BigDataStackMetricValue existingValue) {
		
		JsonNode response = basicQuery(existingValue);
		//System.err.println(response.toString());
		if (response==null) return false;

		if (response.has("status") && !response.get("status").asText().equalsIgnoreCase("success")) return false;
		
		JsonNode data = response.get("data");
		if (data.has("resultType") && !data.get("resultType").asText().equalsIgnoreCase("vector")) return false;
		
		Iterator<JsonNode> resultIterator = data.get("result").iterator();
		
		List<String> values = new ArrayList<String>();
		List<Long> times = new ArrayList<Long>();
		List<Map<String,String>> labelSets = new ArrayList<Map<String,String>>();
		
		while (resultIterator.hasNext()) {
			JsonNode result = resultIterator.next();
			
			JsonNode metricData = result.get("metric");
			Iterator<String> fieldNames = metricData.fieldNames();
			Map<String,String> map = new HashMap<String,String>();
			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				String fieldvalue = metricData.get(fieldName).asText();
				map.put(fieldName, fieldvalue);
			}
			
			JsonNode value = result.get("value");
			double time = value.get(0).asDouble();
			String valueString = value.get(1).asText();
			
			values.add(valueString);
			times.add(Double.valueOf(time).longValue()*1000);
			labelSets.add(map);
		}
		
		List<String> newValues = new ArrayList<String>();
		List<Long> newTimes = new ArrayList<Long>();
		List<Map<String,String>> newLabelSets = new ArrayList<Map<String,String>>();
		
		int eIndex = 0;
		for (Map<String,String> existingSet : existingValue.getLabels()) {
			boolean matchFound = false;
			int matchIndex = -1;
			int index = 0;
			for (Map<String,String> currentSet : labelSets) {
				if (labelMatch(existingSet, currentSet)) {
					matchFound = true;
					matchIndex = index;
					break;
				}
				index++;
			}
			
			if (!matchFound) {
				newValues.add(existingValue.getValue().get(eIndex));
				newTimes.add(existingValue.getLastUpdated().get(eIndex));
				newLabelSets.add(existingValue.getLabels().get(eIndex));
			} else {
				newValues.add(values.get(matchIndex));
				newTimes.add(times.get(matchIndex));
				newLabelSets.add(labelSets.get(matchIndex));
			}
			
			eIndex++;
		}
		
		int index = 0;
		for (Map<String,String> currentSet : labelSets) {
			boolean labelMatch = false;
			for (Map<String,String> existingSet : existingValue.getLabels()) {
				if (labelMatch(existingSet, currentSet)) {
					labelMatch = true;
				}
			}
			
			if (labelMatch) continue;
			
			newValues.add(values.get(index));
			newTimes.add(times.get(index));
			newLabelSets.add(labelSets.get(index));
			
			index++;
		}
		
		existingValue.setLabels(newLabelSets);
		existingValue.setValue(newValues);
		existingValue.setLastUpdated(newTimes);
		
		
		return true;
		
	}
	
	protected boolean labelMatch(Map<String,String> map1, Map<String,String> map2) {
		if (map1.size()!=map2.size()) return false;
		for(String k : map1.keySet()) {
			if (!map2.containsKey(k)) return false;
			if (!map1.get(k).equalsIgnoreCase(map2.get(k))) return false;
		}
		return true;
		
	}


	protected JsonNode basicQuery(BigDataStackMetricValue existingValue) {

		StringBuffer content = new StringBuffer();
		try {
			URL url = new URL("http://prometheus-gdt-"+existingValue.getNamespace()+"."+hostExtension+"/api/v1/query?query="+existingValue.getMetricname());
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		if (content.length()==0) return null;
		
		try {
			JsonNode node = mapper.readTree(content.toString());
			return node;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}
	
	public BigDataStackMetricValue basicQuery(String owner, String namespace, String appID, String objectID, String instance, String metricname) {

		StringBuffer content = new StringBuffer();
		try {
			
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append(metricname+"{");
			boolean first = true;
			if (owner!=null) { if (first) { first=false; } else { queryBuilder.append(","); };  queryBuilder.append("owner%3D\""+owner+"\""); };
			if (namespace!=null) { if (first) { first=false; } else { queryBuilder.append(","); };  queryBuilder.append("namespace%3D\""+namespace+"\""); }
			if (appID!=null) { if (first) { first=false; } else { queryBuilder.append(","); }; queryBuilder.append("appID%3D\""+appID+"\""); }
			if (objectID!=null) { if (first) { first=false; } else { queryBuilder.append(","); }; queryBuilder.append("objectID%3D\""+objectID+"\""); }
			if (instance!=null) { if (first) { first=false; } else { queryBuilder.append(","); }; queryBuilder.append("instance%3D\""+instance+"\""); }
			queryBuilder.append("}");
			
			//System.err.println(queryBuilder.toString());
			
			URL url = new URL("http://prometheus-gdt-"+namespace+"."+hostExtension+"/api/v1/query?query="+queryBuilder.toString());
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		if (content.length()==0) return null;
		
		try {
			JsonNode node = mapper.readTree(content.toString());
			
			if (node.has("status") && !node.get("status").asText().equalsIgnoreCase("success")) return null;
			
			JsonNode data = node.get("data");
			if (data.has("resultType") && !data.get("resultType").asText().equalsIgnoreCase("vector")) return null;
			
			Iterator<JsonNode> resultIterator = data.get("result").iterator();
			
			List<String> values = new ArrayList<String>();
			List<Long> times = new ArrayList<Long>();
			List<Map<String,String>> labelSets = new ArrayList<Map<String,String>>();
			
			while (resultIterator.hasNext()) {
				JsonNode result = resultIterator.next();
				
				JsonNode metricData = result.get("metric");
				Iterator<String> fieldNames = metricData.fieldNames();
				Map<String,String> map = new HashMap<String,String>();
				while (fieldNames.hasNext()) {
					String fieldName = fieldNames.next();
					String fieldvalue = metricData.get(fieldName).asText();
					map.put(fieldName, fieldvalue);
				}
				
				JsonNode value = result.get("value");
				double time = value.get(0).asDouble();
				String valueString = value.get(1).asText();
				
				values.add(valueString);
				times.add(Double.valueOf(time).longValue()*1000);
				labelSets.add(map);
			}
			

			BigDataStackMetricValue metricValue = new BigDataStackMetricValue();
			metricValue.setOwner(owner);
			metricValue.setNamespace(namespace);
			metricValue.setObjectID(objectID);
			metricValue.setAppID(appID);
			metricValue.setLabels(labelSets);
			metricValue.setValue(values);
			metricValue.setLastUpdated(times);
			metricValue.setMetricname(metricname);
			
			return metricValue;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

}
