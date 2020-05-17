package eu.bigdatastack.gdt.prometheus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bigdatastack.gdt.structures.data.BigDataStackMetricValue;

public class PrometheusDataClient {

	ObjectMapper mapper = new ObjectMapper();
	
	
	/**
	 * Updates the value and lastUpdated fields of the provided metric value based on the current
	 * prometheus server data. (does not update the database however)
	 * @param existingValue
	 */
	public boolean update(BigDataStackMetricValue existingValue) {
		
		JsonNode response = basicQuery(existingValue);
		if (response==null) return false;

		if (response.has("status") && !response.get("status").asText().equalsIgnoreCase("success")) return false;
		
		JsonNode data = response.get("data");
		if (data.has("resultType") && !data.get("resultType").asText().equalsIgnoreCase("vector")) return false;
		
		Iterator<JsonNode> resultIterator = data.get("result").iterator();
		while (resultIterator.hasNext()) {
			JsonNode result = resultIterator.next();
			
			JsonNode metricData = result.get("metric");
			Iterator<String> fieldNames = metricData.fieldNames();
			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				String fieldvalue = metricData.get(fieldName).asText();
				existingValue.getLabels().put(fieldName, fieldvalue);
			}
			
			JsonNode value = result.get("value");
			double time = value.get(0).asDouble();
			String valueString = value.get(1).asText();
			
			existingValue.setLastUpdated(Double.valueOf(time).longValue()*1000);
			existingValue.setValue(valueString);
			
		}
		
		return true;
		
	}


	protected JsonNode basicQuery(BigDataStackMetricValue existingValue) {

		StringBuffer content = new StringBuffer();
		try {
			URL url = new URL("http://prometheus-gdt-"+existingValue.getNamespace()+".ida.dcs.gla.ac.uk/api/v1/query?query="+existingValue.getMetricname());
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

}
