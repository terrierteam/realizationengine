package eu.bigdatastack.gdt.structures.data;

import java.util.List;
import java.util.Map;

import eu.bigdatastack.gdt.operations.BigDataStackOperation;

/**
 * This object represents return information regarding a pre-execution validation of a specified operation sequence,
 * i.e. it provides details for the user to check prior to executing the operation sequence.
 * @author EbonBlade
 *
 */
public class BigDataStackOperationSequenceValidation {

	// Objects
	List<String> missingObjects;
	
	// Parameters
	Map<String,String> parametersSetWithDefaults;
	Map<String,BigDataStackOperation> parametersSetAtRuntime;
	List<String> parametersNotSet;
	
	// Resources
	Map<BigDataStackObjectDefinition, List<BigDataStackResourceTemplate>> objectsWithValidTemplates;
	Map<BigDataStackObjectDefinition, List<BigDataStackResourceTemplate>> objectsWithIncompleteTemplates;
	List<BigDataStackObjectDefinition> objectsWithTemplatesSetAtRuntime;
	
	public BigDataStackOperationSequenceValidation() {}

	public BigDataStackOperationSequenceValidation(List<String> missingObjects,
			Map<String, String> parametersSetWithDefaults, Map<String, BigDataStackOperation> parametersSetAtRuntime,
			List<String> parametersNotSet,
			Map<BigDataStackObjectDefinition, List<BigDataStackResourceTemplate>> objectsWithValidTemplates,
			Map<BigDataStackObjectDefinition, List<BigDataStackResourceTemplate>> objectsWithIncompleteTemplates,
			List<BigDataStackObjectDefinition> objectsWithTemplatesSetAtRuntime) {
		super();
		this.missingObjects = missingObjects;
		this.parametersSetWithDefaults = parametersSetWithDefaults;
		this.parametersSetAtRuntime = parametersSetAtRuntime;
		this.parametersNotSet = parametersNotSet;
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

	public Map<BigDataStackObjectDefinition, List<BigDataStackResourceTemplate>> getObjectsWithValidTemplates() {
		return objectsWithValidTemplates;
	}

	public void setObjectsWithValidTemplates(
			Map<BigDataStackObjectDefinition, List<BigDataStackResourceTemplate>> objectsWithValidTemplates) {
		this.objectsWithValidTemplates = objectsWithValidTemplates;
	}

	public Map<BigDataStackObjectDefinition, List<BigDataStackResourceTemplate>> getObjectsWithIncompleteTemplates() {
		return objectsWithIncompleteTemplates;
	}

	public void setObjectsWithIncompleteTemplates(
			Map<BigDataStackObjectDefinition, List<BigDataStackResourceTemplate>> objectsWithIncompleteTemplates) {
		this.objectsWithIncompleteTemplates = objectsWithIncompleteTemplates;
	}

	public List<BigDataStackObjectDefinition> getObjectsWithTemplatesSetAtRuntime() {
		return objectsWithTemplatesSetAtRuntime;
	}

	public void setObjectsWithTemplatesSetAtRuntime(List<BigDataStackObjectDefinition> objectsWithTemplatesSetAtRuntime) {
		this.objectsWithTemplatesSetAtRuntime = objectsWithTemplatesSetAtRuntime;
	}

	
}
