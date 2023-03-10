package org.terrier.realization.prometheus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.terrier.realization.structures.data.BigDataStackMetricValue;
import org.terrier.realization.structures.reports.PerHourTimeSeries;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

 /*
 * Realization Engine 
 * Webpage: https://github.com/terrierteam/realizationengine
 * Contact: richard.mccreadie@glasgow.ac.uk
 * University of Glasgow - School of Computing Science
 * http://www.gla.ac.uk/
 * 
 * The contents of this file are subject to the Apache License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 *
 * The Original Code is Copyright (C) to the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richard.mccreadie@glasgow.ac.uk> (original author)
 */

public class PrometheusDataClient {

	String hostExtension;
	
	public PrometheusDataClient(String hostExtension) {
		this.hostExtension = hostExtension;
	}
	
	public static ObjectMapper mapper = new ObjectMapper();
	
	
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
		return basicQuery( owner,  namespace,  appID,  objectID,  instance,  metricname, "5m");
	}
	
	public BigDataStackMetricValue basicQuery(String owner, String namespace, String appID, String objectID, String instance, String metricname, String timeExpression) {

		StringBuffer content = new StringBuffer();
		try {
			
			StringBuilder queryBuilder = new StringBuilder();
			
			if (owner==null && appID==null && objectID==null && instance==null) {
				queryBuilder.append(metricname);
				if (timeExpression!=null) queryBuilder.append("["+timeExpression+"]");
			} else {
				queryBuilder.append(metricname+"{");
				boolean first = true;
				if (owner!=null) { if (first) { first=false; } else { queryBuilder.append(","); };  queryBuilder.append("owner%3D\""+owner+"\""); };
				if (namespace!=null) { if (first) { first=false; } else { queryBuilder.append(","); };  queryBuilder.append("namespace%3D\""+namespace+"\""); }
				if (appID!=null) { if (first) { first=false; } else { queryBuilder.append(","); }; queryBuilder.append("appID%3D\""+appID+"\""); }
				if (objectID!=null) { if (first) { first=false; } else { queryBuilder.append(","); }; queryBuilder.append("objectID%3D\""+objectID+"\""); }
				if (instance!=null) { if (first) { first=false; } else { queryBuilder.append(","); }; queryBuilder.append("instance%3D\""+instance+"\""); }
				if (timeExpression!=null) queryBuilder.append("}["+timeExpression+"]");
				else queryBuilder.append("}");
			}
			
			
			
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
			
			List<String> values = new ArrayList<String>();
			List<Long> times = new ArrayList<Long>();
			List<Map<String,String>> labelSets = new ArrayList<Map<String,String>>();
			
			JsonNode data = node.get("data");
			if (data.has("resultType") && data.get("resultType").asText().equalsIgnoreCase("vector")) {
				Iterator<JsonNode> resultIterator = data.get("result").iterator();
				
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
			}
			
			if (data.has("resultType") && data.get("resultType").asText().equalsIgnoreCase("matrix")) {
				Iterator<JsonNode> resultIterator = data.get("result").iterator();
				
				
				
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
					
					String valueString = null;
					double time = 0.0;
					JsonNode valueList = result.get("values");
					Iterator<JsonNode> valueIterator = valueList.iterator();
					while (valueIterator.hasNext()) {
						JsonNode value = valueIterator.next();
						time = value.get(0).asDouble();
						valueString = value.get(1).asText();
					}
					
					values.add(valueString);
					times.add(Double.valueOf(time).longValue()*1000);
					labelSets.add(map);
					
				}
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
	
	/**
	 * Static query method for use with any prometheus instance. Does not assume data parameterization like the instantiated methods of this class.
	 * @param prometheusHost
	 * @param metricname
	 * @param matchCriteria
	 * @param timeExpression
	 * @param owner
	 * @param namespace
	 * @param appID
	 * @param objectID
	 * @return
	 */
	public static BigDataStackMetricValue basicQuery(String prometheusHost, String metricname, Map<String,String> matchCriteria, String timeExpression, String owner, String namespace, String appID, String objectID) {

		StringBuffer content = new StringBuffer();
		try {
			
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append(metricname+"{");
			boolean first = true;
			for (String key : matchCriteria.keySet()) {
				if (key!=null) { if (first) { first=false; } else { queryBuilder.append(","); };  queryBuilder.append(key+"%3D\""+matchCriteria.get(key)+"\""); };
			}
			if (timeExpression!=null) queryBuilder.append("}["+timeExpression+"]");
			else  queryBuilder.append("}");
			
			//System.err.println(queryBuilder.toString());
			
			URL url = new URL(prometheusHost+"/api/v1/query?query="+queryBuilder.toString());
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
			
			List<String> values = new ArrayList<String>();
			List<Long> times = new ArrayList<Long>();
			List<Map<String,String>> labelSets = new ArrayList<Map<String,String>>();
			
			JsonNode data = node.get("data");
			if (data.has("resultType") && data.get("resultType").asText().equalsIgnoreCase("vector")) {
				Iterator<JsonNode> resultIterator = data.get("result").iterator();
				
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
			}
			
			if (data.has("resultType") && data.get("resultType").asText().equalsIgnoreCase("matrix")) {
				Iterator<JsonNode> resultIterator = data.get("result").iterator();
				
				
				
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
					
					String valueString = null;
					double time = 0.0;
					JsonNode valueList = result.get("values");
					Iterator<JsonNode> valueIterator = valueList.iterator();
					while (valueIterator.hasNext()) {
						JsonNode value = valueIterator.next();
						time = value.get(0).asDouble();
						valueString = value.get(1).asText();
					}
					
					values.add(valueString);
					times.add(Double.valueOf(time).longValue()*1000);
					labelSets.add(map);
					
				}
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
	
	
	
	public PerHourTimeSeries perHourAvg(String owner, String namespace, String appID, String objectID, String instance, String metricname, String timeExpression) {

		StringBuilder content = new StringBuilder();
		try {
			
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append(metricname+"{");
			boolean first = true;
			if (owner!=null) { if (first) { first=false; } else { queryBuilder.append(","); };  queryBuilder.append("owner%3D\""+owner+"\""); };
			if (namespace!=null) { if (first) { first=false; } else { queryBuilder.append(","); };  queryBuilder.append("namespace%3D\""+namespace+"\""); }
			if (appID!=null) { if (first) { first=false; } else { queryBuilder.append(","); }; queryBuilder.append("appID%3D\""+appID+"\""); }
			if (objectID!=null) { if (first) { first=false; } else { queryBuilder.append(","); }; queryBuilder.append("objectID%3D\""+objectID+"\""); }
			if (instance!=null) { if (first) { first=false; } else { queryBuilder.append(","); }; queryBuilder.append("instance%3D\""+instance+"\""); }
			queryBuilder.append("}["+timeExpression+"]");
			
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
			
			// Calculate the earliest time in any series to act as a common start point
			long earliestTime = Long.MAX_VALUE;
			
			if (data.has("resultType") && data.get("resultType").asText().equalsIgnoreCase("vector")) {
				Iterator<JsonNode> resultIterator = data.get("result").iterator();
				while (resultIterator.hasNext()) {
					JsonNode result = resultIterator.next();
					JsonNode value = result.get("value");
					long time = Double.valueOf(value.get(0).asDouble()).longValue()*1000;
					if (time<earliestTime) earliestTime = time;
				}
			}
			
			if (data.has("resultType") && data.get("resultType").asText().equalsIgnoreCase("matrix")) {
				Iterator<JsonNode> resultIterator = data.get("result").iterator();
				while (resultIterator.hasNext()) {
					JsonNode result = resultIterator.next();
					JsonNode valueList = result.get("values");
					Iterator<JsonNode> valueIterator = valueList.iterator();
					while (valueIterator.hasNext()) {
						JsonNode value = valueIterator.next();
						long time = Double.valueOf(value.get(0).asDouble()).longValue()*1000;
						if (time<earliestTime) earliestTime = time;
					}
				}
			}
			
			// Now generate the per hour bins
			List<List<Double>> binsPerTimeSeries = new ArrayList<List<Double>>();
			
			
			if (data.has("resultType") && data.get("resultType").asText().equalsIgnoreCase("vector")) {
				Iterator<JsonNode> resultIterator = data.get("result").iterator();
				
				while (resultIterator.hasNext()) {
					
					long startOfHour = earliestTime;
					long oneHour = 1000*60*60;
					
					double sumInHour = 0.0;
					
					// We only have a single data point, so assume its for the containing hour
					List<Double> timeSeries = new ArrayList<Double>(1);
					
					JsonNode result = resultIterator.next();
					
					JsonNode value = result.get("value");
					long time = Double.valueOf(value.get(0).asDouble()).longValue()*1000;
					double valueString = Double.parseDouble(value.get(1).asText());
					
					while (time>(startOfHour+oneHour)) {
						// new hour
						timeSeries.add(sumInHour);
						
						sumInHour = 0.0;
						
						startOfHour = (startOfHour+oneHour);
					}
					
					sumInHour = sumInHour+valueString;
					timeSeries.add(sumInHour);
					while (startOfHour<System.currentTimeMillis()) {
						startOfHour = (startOfHour+oneHour);
						timeSeries.add(0.0);
					}
					
					binsPerTimeSeries.add(timeSeries);
					
				}
				
			}
			
			if (data.has("resultType") && data.get("resultType").asText().equalsIgnoreCase("matrix")) {
				Iterator<JsonNode> resultIterator = data.get("result").iterator();
				
				while (resultIterator.hasNext()) {
					JsonNode result = resultIterator.next();
					JsonNode valueList = result.get("values");
					Iterator<JsonNode> valueIterator = valueList.iterator();
					
					long startOfHour = earliestTime;
					long oneHour = 1000*60*60;
					
					double sumInHour = 0.0;
					int countInHour = 0;
					
					List<Double> timeSeries = new ArrayList<Double>(1);
					
					while (valueIterator.hasNext()) {
						JsonNode value = valueIterator.next();
						long time = Double.valueOf(value.get(0).asDouble()).longValue()*1000;
						double valueString = Double.parseDouble(value.get(1).asText());
						
						while (time>(startOfHour+oneHour)) {
							// new hour
							
							if (countInHour>0) timeSeries.add(sumInHour/countInHour);
							else timeSeries.add(sumInHour);
							
							
							sumInHour = 0.0;
							countInHour=0;
							
							startOfHour = (startOfHour+oneHour);
						}
						sumInHour = sumInHour+valueString;
						countInHour=countInHour+1;
						
					}
					if (countInHour>0) timeSeries.add(sumInHour/countInHour);
					else timeSeries.add(sumInHour);
					binsPerTimeSeries.add(timeSeries);
					
				}
			}
			

			
			
			int maxIndex =0;
			for (List<Double> series : binsPerTimeSeries) {
				//System.err.println(series.size());
				if (maxIndex<series.size()) maxIndex = series.size();
			}
			
			double[] sum = new double[maxIndex];
			for (List<Double> series : binsPerTimeSeries) {
				for (int i=0; i<series.size(); i++) {
					sum[i] = sum[i]+series.get(i);
				}
			}
			
			PerHourTimeSeries timeSeries = new PerHourTimeSeries(earliestTime, sum);
					
			return timeSeries;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

}
