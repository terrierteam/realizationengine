package org.terrier.realization.structures.data;

import java.util.Map;

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

public class BigDataStackResourceTemplateAdv extends BigDataStackResourceTemplate implements Comparable<BigDataStackResourceTemplateAdv>{

	Map<String,String> qualityOfServiceFactors;
	Map<String,String> qualityOfServiceFactorImages;
	double cost;
	double score;
	int numStars;
	public BigDataStackResourceTemplateAdv(Map<String, String> requests, 
			Map<String, String> limits, 
			Map<String, String> qualityOfServiceFactors, 
			Map<String, String> qualityOfServiceFactorImages, 
			double cost, 
			double score,
			int numStars) {
		super();
		this.requests = requests;
		this.limits = limits;
		this.qualityOfServiceFactors = qualityOfServiceFactors;
		this.qualityOfServiceFactorImages = qualityOfServiceFactorImages;
		this.cost = cost;
		this.score = score;
		this.numStars = numStars;
	}
	public Map<String, String> getQualityOfServiceFactors() {
		return qualityOfServiceFactors;
	}
	public void setQualityOfServiceFactors(Map<String, String> qualityOfServiceFactors) {
		this.qualityOfServiceFactors = qualityOfServiceFactors;
	}
	public Map<String, String> getQualityOfServiceFactorImages() {
		return qualityOfServiceFactorImages;
	}
	public void setQualityOfServiceFactorImages(Map<String, String> qualityOfServiceFactorImages) {
		this.qualityOfServiceFactorImages = qualityOfServiceFactorImages;
	}
	public double getCost() {
		return cost;
	}
	public void setCost(double cost) {
		this.cost = cost;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public int getNumStars() {
		return numStars;
	}
	public void setNumStars(int numStars) {
		this.numStars = numStars;
	}
	@Override
	public int compareTo(BigDataStackResourceTemplateAdv o) {
		return new Double(score).compareTo(o.score);
	}
	
	
	
	
	
}
