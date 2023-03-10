package org.terrier.realization.structures.reports;

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
