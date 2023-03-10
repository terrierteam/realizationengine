package org.terrier.realization.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.terrier.realization.openshift.OpenshiftOperationFabric8ioClient;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackSearchItem;
import org.terrier.realization.structures.data.BigDataStackSearchResponse;

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
public class SearchUtil {

	public static BigDataStackSearchResponse basicSearch(String query, OpenshiftOperationFabric8ioClient operationClient, BigDataStackObjectDefinition object) {
		
		long start = System.currentTimeMillis();
		
		Map<String,Map<String,String>> logs = operationClient.getLogs(object);
		
		Map<String,Map<String, List<BigDataStackSearchItem>>> results = new HashMap<String,Map<String, List<BigDataStackSearchItem>>>();
		
		for (String podID : logs.keySet()) {
			if (!results.containsKey(podID)) results.put(podID, new HashMap<String, List<BigDataStackSearchItem>>());
			Map<String, List<BigDataStackSearchItem>> podLogs =  results.get(podID);
			for (String containerID : logs.get(podID).keySet()) {
				if (!podLogs.containsKey(containerID)) podLogs.put(containerID, new ArrayList<BigDataStackSearchItem>());
				
				try {
					int lineno = 1;
					BufferedReader br = new BufferedReader(new StringReader(logs.get(podID).get(containerID)));
					String line;
					while ((line = br.readLine())!=null) {
						if (line.contains(query)) {
							BigDataStackSearchItem item = new BigDataStackSearchItem(line, lineno, 1.0);
							podLogs.get(containerID).add(item);
						}
						lineno++;
					}
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		BigDataStackSearchResponse response = new BigDataStackSearchResponse(query, results, "terrier", (System.currentTimeMillis()-start)/1000);
		
		return response;
		
	}
	
}
