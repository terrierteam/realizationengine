package org.terrier.realization.structures.data;

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

/**
 * This represents the generic definition of a metric that can be tracked within BigDataStack.
 * A metric is defined for a user. Note that a metric may have a range that defines the maximum
 * and minimum values. Constraints/threholds are not set my metrics, but instead are stored separately.
 *
 */
public class BigDataStackMetric {

	private String owner;
	
	// Metric Descriptors
	private String name;
	private String summary;
	private String displayUnit;
	
	// Bound Data
	private double maximumValue;
	private double minimumValue;
	
	// Interpretation
	private boolean higherIsBetter;
	
	// Source
	private BigDataStackMetricSource source;
	
	// Aggregation
	private BigDataStackMetricAggregation aggregation;
	
	public BigDataStackMetric() {}
	
	public BigDataStackMetric(String owner, String name, String summary, String displayUnit, double maximumValue,
			double minimumValue, boolean higherIsBetter, BigDataStackMetricSource source,
			BigDataStackMetricAggregation aggregation) {
		super();
		this.owner = owner;
		this.name = name;
		this.summary = summary;
		this.displayUnit = displayUnit;
		this.maximumValue = maximumValue;
		this.minimumValue = minimumValue;
		this.higherIsBetter = higherIsBetter;
		this.source = source;
		this.aggregation = aggregation;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getDisplayUnit() {
		return displayUnit;
	}

	public void setDisplayUnit(String displayUnit) {
		this.displayUnit = displayUnit;
	}

	public double getMaximumValue() {
		return maximumValue;
	}

	public void setMaximumValue(double maximumValue) {
		this.maximumValue = maximumValue;
	}

	public double getMinimumValue() {
		return minimumValue;
	}

	public void setMinimumValue(double minimumValue) {
		this.minimumValue = minimumValue;
	}

	public boolean isHigherIsBetter() {
		return higherIsBetter;
	}

	public void setHigherIsBetter(boolean higherIsBetter) {
		this.higherIsBetter = higherIsBetter;
	}

	public BigDataStackMetricSource getSource() {
		return source;
	}

	public void setSource(BigDataStackMetricSource source) {
		this.source = source;
	}

	public BigDataStackMetricAggregation getAggregation() {
		return aggregation;
	}

	public void setAggregation(BigDataStackMetricAggregation aggregation) {
		this.aggregation = aggregation;
	}
	
	
      	
        	
	
}
