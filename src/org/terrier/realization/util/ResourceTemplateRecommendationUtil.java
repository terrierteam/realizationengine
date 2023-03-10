package org.terrier.realization.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.terrier.realization.structures.data.BigDataStackResourceTemplateAdv;

import com.fasterxml.jackson.core.JsonProcessingException;
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
public class ResourceTemplateRecommendationUtil {

	public static  List<BigDataStackResourceTemplateAdv> getDefaultRecommendations() {

		List<BigDataStackResourceTemplateAdv> recs = new ArrayList<BigDataStackResourceTemplateAdv>();
		
		{Map<String, String> qualityOfServiceFactors = new HashMap<String, String>(); qualityOfServiceFactors.put("Est Response Time", "353ms");
		Map<String, String> qualityOfServiceFactorImages = new HashMap<String, String>(); qualityOfServiceFactorImages.put("Est Response Time", "remixicon/icons/System/timer-line.svg");
		recs.add(makeResourceTemplate(100, 100, 1, 1, 0.2440, qualityOfServiceFactors, qualityOfServiceFactorImages));}
		
		{Map<String, String> qualityOfServiceFactors = new HashMap<String, String>(); qualityOfServiceFactors.put("Est Response Time", "91ms");
		Map<String, String> qualityOfServiceFactorImages = new HashMap<String, String>(); qualityOfServiceFactorImages.put("Est Response Time", "remixicon/icons/System/timer-line.svg");
		recs.add(makeResourceTemplate(200, 200, 1, 1, 0.7218, qualityOfServiceFactors, qualityOfServiceFactorImages));}
		
		{Map<String, String> qualityOfServiceFactors = new HashMap<String, String>(); qualityOfServiceFactors.put("Est Response Time", "103ms");
		Map<String, String> qualityOfServiceFactorImages = new HashMap<String, String>(); qualityOfServiceFactorImages.put("Est Response Time", "remixicon/icons/System/timer-line.svg");
		recs.add(makeResourceTemplate(300, 300, 1, 1, 0.6114, qualityOfServiceFactors, qualityOfServiceFactorImages));}

		{Map<String, String> qualityOfServiceFactors = new HashMap<String, String>(); qualityOfServiceFactors.put("Est Response Time", "71ms");
		Map<String, String> qualityOfServiceFactorImages = new HashMap<String, String>(); qualityOfServiceFactorImages.put("Est Response Time", "remixicon/icons/System/timer-line.svg");
		recs.add(makeResourceTemplate(500, 500, 1, 1, 0.6013, qualityOfServiceFactors, qualityOfServiceFactorImages));}
		
		{Map<String, String> qualityOfServiceFactors = new HashMap<String, String>(); qualityOfServiceFactors.put("Est Response Time", "70ms");
		Map<String, String> qualityOfServiceFactorImages = new HashMap<String, String>(); qualityOfServiceFactorImages.put("Est Response Time", "remixicon/icons/System/timer-line.svg");
		recs.add(makeResourceTemplate(1000, 1000, 1, 1, 0.4640, qualityOfServiceFactors, qualityOfServiceFactorImages));}
		
		Collections.sort(recs);
		Collections.reverse(recs);
		
		
		double maxScore = 0.0;
		double minScore = 0.0;
		for (BigDataStackResourceTemplateAdv rec : recs) {
			if (rec.getScore()>maxScore) maxScore = rec.getScore();
		}
		for (BigDataStackResourceTemplateAdv rec : recs) {
			double normScore = (rec.getScore()-minScore)/(maxScore-minScore);
			if (normScore>=0.8) rec.setNumStars(5);
			else if (normScore>=0.6) rec.setNumStars(4);
			else if (normScore>=0.4) rec.setNumStars(3);
			else if (normScore>=0.2) rec.setNumStars(2);
			else rec.setNumStars(1);
		}
		
		
		return recs;
	}
	
	
	public static BigDataStackResourceTemplateAdv makeResourceTemplate(int cpurequest, int cpulimit, int memrequest, int memlimit, double score, Map<String, String> qualityOfServiceFactors, Map<String, String> qualityOfServiceFactorImages) {
		Map<String, String> requests = new HashMap<String, String>();
		requests.put("cpu", cpurequest+"m");
		requests.put("memory", memrequest+"Gi");
		Map<String, String> limits = new HashMap<String, String>();
		limits.put("cpu", cpulimit+"m");
		limits.put("memory", memlimit+"Gi");
		
		double cost =  calculateCPUCost(cpulimit) + calculateMemCost(memlimit*1024);
		
		return new BigDataStackResourceTemplateAdv(requests, limits, qualityOfServiceFactors, qualityOfServiceFactorImages,  cost, score, 0);
				
	}
	
	
	public static double calculateCPUCost(int millicores) {
		// 4000m = 0.048
		return ((0.048/4000)/2)*millicores;
	}
	
	public static double calculateMemCost(int mb) {
		// 16,384‬ = 0.048
		return (((0.048*mb)/16384)/2);
	}

}
