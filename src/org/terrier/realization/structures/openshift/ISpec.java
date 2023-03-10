package org.terrier.realization.structures.openshift;

import com.fasterxml.jackson.databind.node.ObjectNode;

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

public class ISpec {

	int parallelism;
	int completions;
	int backoffLimit;
	
	ISelector selector;
	ObjectNode template;
	
	
	public ISpec( ) {}


	public int getParallelism() {
		return parallelism;
	}


	public void setParallelism(int parallelism) {
		this.parallelism = parallelism;
	}


	public int getCompletions() {
		return completions;
	}


	public void setCompletions(int completions) {
		this.completions = completions;
	}


	public int getBackoffLimit() {
		return backoffLimit;
	}


	public void setBackoffLimit(int backoffLimit) {
		this.backoffLimit = backoffLimit;
	}


	public ISelector getSelector() {
		return selector;
	}


	public void setSelector(ISelector selector) {
		this.selector = selector;
	}


	public ObjectNode getTemplate() {
		return template;
	}


	public void setTemplate(ObjectNode template) {
		this.template = template;
	}


	
	
	
	
}
