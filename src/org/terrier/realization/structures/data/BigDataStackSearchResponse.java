package org.terrier.realization.structures.data;

import java.util.List;
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

public class BigDataStackSearchResponse {

	String query;
	Map<String,Map<String, List<BigDataStackSearchItem>>> results;
	String searchProvider;
	long searchLatency;
	public BigDataStackSearchResponse(String query, Map<String, Map<String, List<BigDataStackSearchItem>>> results,
			String searchProvider, long searchLatency) {
		super();
		this.query = query;
		this.results = results;
		this.searchProvider = searchProvider;
		this.searchLatency = searchLatency;
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	public Map<String, Map<String, List<BigDataStackSearchItem>>> getResults() {
		return results;
	}
	public void setResults(Map<String, Map<String, List<BigDataStackSearchItem>>> results) {
		this.results = results;
	}
	public String getSearchProvider() {
		return searchProvider;
	}
	public void setSearchProvider(String searchProvider) {
		this.searchProvider = searchProvider;
	}
	public long getSearchLatency() {
		return searchLatency;
	}
	public void setSearchLatency(long searchLatency) {
		this.searchLatency = searchLatency;
	}
	

	
	
	
}
