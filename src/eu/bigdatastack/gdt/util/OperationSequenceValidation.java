package eu.bigdatastack.gdt.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.bigdatastack.gdt.application.GDTManager;
import eu.bigdatastack.gdt.operations.BigDataStackOperation;
import eu.bigdatastack.gdt.operations.Instantiate;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequenceValidation;
import eu.bigdatastack.gdt.structures.data.BigDataStackResourceTemplate;

public class OperationSequenceValidation {

	public static BigDataStackOperationSequenceValidation validate(GDTManager manager, BigDataStackOperationSequence sequence) throws SQLException {
		
		List<String> missingObjects = new ArrayList<String>();
		
		// Parameters
		Map<String,String> parametersSetWithDefaults = new HashMap<String,String>();
		Map<String,BigDataStackOperation> parametersSetAtRuntime = new HashMap<String,BigDataStackOperation>();
		List<String> parametersNotSet = new ArrayList<String>();
		
		// Check objects and get their parameters
		Map<String,String> objectIDsAndRefs = getObjectIDsAndRefsForSequence(sequence);
		Map<String,BigDataStackObjectDefinition> objectID2Object = new HashMap<String,BigDataStackObjectDefinition>(); 
		List<String> allParameters = new ArrayList<String>();
		for (String objectID : objectIDsAndRefs.keySet()) {
			BigDataStackObjectDefinition object = manager.getObjectTemplateClient().getObject(objectID, sequence.getOwner());
			if (object==null) {
				missingObjects.add(objectID);
				objectIDsAndRefs.remove(objectID);
			}
			else {
				objectID2Object.put(objectID, object);
				List<String> placeholders = getConfigurableParametersForObjectID(object);
				for (String placeholder : placeholders) allParameters.add(placeholder);
			}
		}
		
		// Get a list of parameters that are set at run-time
		// TODO
		
		
		// Resources
		Map<BigDataStackObjectDefinition, BigDataStackResourceTemplate> objectsWithValidTemplates = new HashMap<BigDataStackObjectDefinition, BigDataStackResourceTemplate>();
		Map<BigDataStackObjectDefinition, BigDataStackResourceTemplate> objectsWithIncompleteTemplates = new HashMap<BigDataStackObjectDefinition, BigDataStackResourceTemplate>();
		List<BigDataStackObjectDefinition> objectsWithTemplatesSetAtRuntime = new ArrayList<BigDataStackObjectDefinition>();
		List<BigDataStackObjectDefinition> objectsWithNoTemplates = new ArrayList<BigDataStackObjectDefinition>();
		
		
		
		
		
		
		
		
		
		return new BigDataStackOperationSequenceValidation(missingObjects, parametersSetWithDefaults, parametersSetAtRuntime, parametersNotSet, objectsWithValidTemplates, objectsWithIncompleteTemplates, objectsWithTemplatesSetAtRuntime, objectsWithNoTemplates);
	}
	
	/**
	 * Gets a list of the target objectIDs created by this operation sequence
	 * @param sequence
	 * @return
	 */
	public static Map<String,String> getObjectIDsAndRefsForSequence(BigDataStackOperationSequence sequence) {
		Map<String,String> objectIDs = new HashMap<String,String>();
		List<BigDataStackOperation> operations = sequence.getOperations();
		for (BigDataStackOperation operation : operations) {
			
			if (operation instanceof Instantiate) {
				String objectID = operation.getObjectID();
				if (!objectIDs.containsKey(objectID)) objectIDs.put(objectID, ((Instantiate)operation).getSeqInstanceRef());
			}
			
		}
		return objectIDs;
	}
	
	
	public static List<String> getConfigurableParametersForObjectID(BigDataStackObjectDefinition object) {
		List<String> placeholders = new ArrayList<String>();
		
		String yaml = object.getYamlSource();
		boolean inplaceholder = false;
		StringBuilder placeholder = new StringBuilder();
		for (int i = 0; i<yaml.length(); i++) {
			char c = yaml.charAt(i);
			
			if (c=='$') {
				if (!inplaceholder) {
					inplaceholder=true;
					placeholder = new StringBuilder();
				}
				else {
					inplaceholder=false;
					if (placeholder.length()>0 && !placeholders.contains(placeholder.toString())) placeholders.add(placeholder.toString()); 
				}
			} else if (inplaceholder) placeholder.append(c);
			
		}
		
		
		return placeholders;
	}
	
	public static void main(String[] args) {
		String text = "we are going to do some replacement pof $A$ and $something$ with additional $text$.";
		
		List<String> placeholders = new ArrayList<String>();
		
		String yaml = text;
		boolean inplaceholder = false;
		StringBuilder placeholder = new StringBuilder();
		for (int i = 0; i<yaml.length(); i++) {
			char c = yaml.charAt(i);
			
			if (c=='$') {
				if (!inplaceholder) {
					inplaceholder=true;
					placeholder = new StringBuilder();
				}
				else {
					inplaceholder=false;
					if (placeholder.length()>0 && !placeholders.contains(placeholder.toString())) placeholders.add(placeholder.toString()); 
				}
			} else if (inplaceholder) placeholder.append(c);
			
		}
		
		
		for (String s: placeholders) {
			System.out.println(s);
		}
	}
	
}
