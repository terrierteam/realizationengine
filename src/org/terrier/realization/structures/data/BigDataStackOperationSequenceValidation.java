package org.terrier.realization.structures.data;

import java.util.List;
import java.util.Map;

import org.terrier.realization.operations.BigDataStackOperation;

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
 * This object represents return information regarding a pre-execution validation of a specified operation sequence,
 * i.e. it provides details for the user to check prior to executing the operation sequence.
 *
 */
public class BigDataStackOperationSequenceValidation {

	// Objects
	List<String> missingObjects;
	
	// Parameters
	Map<String,String> parametersSetWithDefaults;
	Map<String,BigDataStackOperation> parametersSetAtRuntime;
	List<String> parametersNotSet;
	Map<String,List<String>> objects2parameters;
	
	
	// Resources
	Map<String, List<BigDataStackResourceTemplate>> objectsWithValidTemplates;
	Map<String, List<BigDataStackResourceTemplate>> objectsWithIncompleteTemplates;
	List<String> objectsWithTemplatesSetAtRuntime;
	
	public BigDataStackOperationSequenceValidation() {}

	public BigDataStackOperationSequenceValidation(List<String> missingObjects,
			Map<String, String> parametersSetWithDefaults, Map<String, BigDataStackOperation> parametersSetAtRuntime,
			List<String> parametersNotSet, Map<String, List<String>> objects2parameters,
			Map<String, List<BigDataStackResourceTemplate>> objectsWithValidTemplates,
			Map<String, List<BigDataStackResourceTemplate>> objectsWithIncompleteTemplates,
			List<String> objectsWithTemplatesSetAtRuntime) {
		super();
		this.missingObjects = missingObjects;
		this.parametersSetWithDefaults = parametersSetWithDefaults;
		this.parametersSetAtRuntime = parametersSetAtRuntime;
		this.parametersNotSet = parametersNotSet;
		this.objects2parameters = objects2parameters;
		this.objectsWithValidTemplates = objectsWithValidTemplates;
		this.objectsWithIncompleteTemplates = objectsWithIncompleteTemplates;
		this.objectsWithTemplatesSetAtRuntime = objectsWithTemplatesSetAtRuntime;
	}

	public List<String> getMissingObjects() {
		return missingObjects;
	}

	public void setMissingObjects(List<String> missingObjects) {
		this.missingObjects = missingObjects;
	}

	public Map<String, String> getParametersSetWithDefaults() {
		return parametersSetWithDefaults;
	}

	public void setParametersSetWithDefaults(Map<String, String> parametersSetWithDefaults) {
		this.parametersSetWithDefaults = parametersSetWithDefaults;
	}

	public Map<String, BigDataStackOperation> getParametersSetAtRuntime() {
		return parametersSetAtRuntime;
	}

	public void setParametersSetAtRuntime(Map<String, BigDataStackOperation> parametersSetAtRuntime) {
		this.parametersSetAtRuntime = parametersSetAtRuntime;
	}

	public List<String> getParametersNotSet() {
		return parametersNotSet;
	}

	public void setParametersNotSet(List<String> parametersNotSet) {
		this.parametersNotSet = parametersNotSet;
	}

	public Map<String, List<String>> getObjects2parameters() {
		return objects2parameters;
	}

	public void setObjects2parameters(Map<String, List<String>> objects2parameters) {
		this.objects2parameters = objects2parameters;
	}

	public Map<String, List<BigDataStackResourceTemplate>> getObjectsWithValidTemplates() {
		return objectsWithValidTemplates;
	}

	public void setObjectsWithValidTemplates(Map<String, List<BigDataStackResourceTemplate>> objectsWithValidTemplates) {
		this.objectsWithValidTemplates = objectsWithValidTemplates;
	}

	public Map<String, List<BigDataStackResourceTemplate>> getObjectsWithIncompleteTemplates() {
		return objectsWithIncompleteTemplates;
	}

	public void setObjectsWithIncompleteTemplates(
			Map<String, List<BigDataStackResourceTemplate>> objectsWithIncompleteTemplates) {
		this.objectsWithIncompleteTemplates = objectsWithIncompleteTemplates;
	}

	public List<String> getObjectsWithTemplatesSetAtRuntime() {
		return objectsWithTemplatesSetAtRuntime;
	}

	public void setObjectsWithTemplatesSetAtRuntime(List<String> objectsWithTemplatesSetAtRuntime) {
		this.objectsWithTemplatesSetAtRuntime = objectsWithTemplatesSetAtRuntime;
	}

	

	

	
}
