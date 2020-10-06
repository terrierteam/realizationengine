package eu.bigdatastack.gdt.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import eu.bigdatastack.gdt.operations.BigDataStackOperation;
import eu.bigdatastack.gdt.structures.data.BigDataStackAppState;
import eu.bigdatastack.gdt.structures.data.BigDataStackAppStateCondition;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplicationType;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectType;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequenceMode;

/**
 * Utility class that provides file operations to avoid redundant code
 * @author EbonBlade
 *
 */
public class GDTFileUtil {

	/**
	 * Reads in a file to a string with a given encoding
	 * @param file
	 * @param encoding
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static String file2String(File file, String encoding) throws FileNotFoundException, IOException {

		BufferedReader reader = null;

		if (file.getName().endsWith(".gz") || file.getName().endsWith(".GZ")) {
			reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)),encoding));
		} else {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),encoding));
		}

		StringBuilder builder = new StringBuilder();
		String line;
		while ((line = reader.readLine())!=null) {
			builder.append(line);
			builder.append('\n');
		}

		reader.close();

		return builder.toString();
	}

	/**
	 * Reads a BigDataStack Object Definition from a string. This is needed because we
	 * want to convert yamlSource to a string, for storage purposes.
	 * @param yaml
	 * @return
	 */
	public static BigDataStackObjectDefinition readObjectFromString(String yaml, BigDataStackApplication app) {

		boolean inferMissingValues = false;
		boolean overrideValues = false;
		boolean setObjectMetadata = false;
		if (app!=null) {
			if (app.getTypes().contains(BigDataStackApplicationType.inferMissingValues)) inferMissingValues = true;
			if (app.getTypes().contains(BigDataStackApplicationType.overrideValues)) overrideValues = true;
			if (app.getTypes().contains(BigDataStackApplicationType.setObjectMetadata)) setObjectMetadata = true;
		}

		try {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			JsonNode node = mapper.readTree(yaml);

			BigDataStackObjectDefinition object;

			String appID = null;
			if ((inferMissingValues && !node.has("appID")) || overrideValues) appID = app.getAppID();
			else if (node.has("appID")) appID = node.get("appID").asText();

			String namespace = null;
			if ((inferMissingValues && !node.has("namespace")) || overrideValues) namespace = app.getNamespace();
			else if (node.has("namespace")) namespace = node.get("namespace").asText();

			String owner = null;
			if ((inferMissingValues && !node.has("owner")) || overrideValues) owner = app.getOwner();
			else if (node.has("owner")) owner = node.get("owner").asText();

			String objectID = node.get("objectID").asText();

			BigDataStackObjectType type = null;
			if (!node.has("type")) {
				String typeOfYaml = node.get("yamlSource").get("kind").asText();
				type = BigDataStackObjectType.valueOf(typeOfYaml);
			} else type = BigDataStackObjectType.valueOf(node.get("type").asText());


			if (setObjectMetadata) {
				ObjectNode yamlSource = (ObjectNode)node.get("yamlSource");
				ObjectNode metadata = null;
				
				if (yamlSource.has("metadata") && !yamlSource.get("metadata").isNull()) metadata = (ObjectNode)yamlSource.get("metadata");
				else metadata = mapper.createObjectNode();
				
				if (node.has("instance")) metadata.put("name", appID+"-"+objectID+"-"+node.get("instance").asInt());
				else metadata.put("name", appID+"-"+objectID+"-$instance$");
				metadata.put("namespace", namespace);
				yamlSource.put("metadata", metadata);
			}
			
			
			String yamlSource = mapper.writeValueAsString(node.get("yamlSource"));
			
			object = new BigDataStackObjectDefinition(objectID, owner, type,
					yamlSource, new HashSet<String>(), namespace, appID);


			
			
			if (node.has("instance")) object.setInstance(node.get("instance").asInt());


			return object;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * Reads in a BigDataStackOperationSequence from a yaml format string. Note that this relies on implementations of
	 * initalizeFromJson within each Operation class to parse the contents of the operation definitions
	 * @param yaml
	 * @return
	 */
	public static BigDataStackOperationSequence readSequenceFromString(String yaml) {

		return readSequenceFromString(yaml, null, null);
	}

	/**
	 * Reads in a BigDataStackOperationSequence from a yaml format string. Note that this relies on implementations of
	 * initalizeFromJson within each Operation class to parse the contents of the operation definitions
	 * @param yaml
	 * @return
	 */
	public static BigDataStackOperationSequence readSequenceFromString(String yaml, String namespace, String owner) {
		try {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			JsonNode node = mapper.readTree(yaml);


			String appID = node.get("appID").asText();
			if (owner==null && node.has("owner")) owner = node.get("owner").asText();
			if (namespace==null && node.has("namespace")) namespace = node.get("namespace").asText();
			String sequenceID = node.get("sequenceID").asText();


			yaml = yaml.replaceAll("\\$appID\\$", appID);
			yaml = yaml.replaceAll("\\$owner\\$", owner);
			yaml = yaml.replaceAll("\\$namespace\\$", namespace);
			yaml = yaml.replaceAll("\\$sequenceID\\$", sequenceID);
			
			Random r = new Random();
			long randomLong = r.nextLong();
			yaml = yaml.replaceAll("\\$random\\$", String.valueOf(randomLong));
			yaml = yaml.replaceAll("\\$RANDOM\\$", String.valueOf(randomLong));

			node = mapper.readTree(yaml);


			String name = "";
			if (node.has("name")) name = node.get("name").asText();
			String description = "";
			if (node.has("description")) description = node.get("description").asText();
			Map<String,String> parameters = new HashMap<String,String>();
			if (node.has("parameters")) {
				Iterator<String> paramI = node.get("parameters").fieldNames();
				while (paramI.hasNext()) {
					String fieldName = paramI.next();
					parameters.put(fieldName, node.get("parameters").get(fieldName).asText());

				}
			}

			if (!node.has("operations") || !node.get("operations").isArray()) return null;
			Iterator<JsonNode> operationI = node.get("operations").iterator();
			List<BigDataStackOperation> operations = new ArrayList<BigDataStackOperation>(5);
			while (operationI.hasNext()) {
				JsonNode operationJson = operationI.next();
				String className = operationJson.get("className").asText();


				@SuppressWarnings("deprecation")
				BigDataStackOperation operation = (BigDataStackOperation) Class.forName("eu.bigdatastack.gdt.operations."+className).newInstance();
				operation.setAppID(appID);
				operation.setNamespace(namespace);
				operation.setOwner(owner);
				operation.initalizeFromJson(operationJson);
				operations.add(operation);
			}

			BigDataStackOperationSequenceMode mode = BigDataStackOperationSequenceMode.valueOf(node.get("mode").asText());

			BigDataStackOperationSequence sequence = new BigDataStackOperationSequence(appID, owner, namespace, sequenceID, name,
					description, parameters, operations, mode);


			return sequence;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * Reads in an application state from yaml. The provided application is used to fill in the missing information
	 * about appID, namespace and owner.
	 * @param yaml
	 * @param app
	 * @return
	 */
	public static BigDataStackAppState readApplicationStateFromString(String yaml, BigDataStackApplication app) {
		boolean inferMissingValues = false;
		boolean overrideValues = false;
		if (app!=null) {
			if (app.getTypes().contains(BigDataStackApplicationType.inferMissingValues)) inferMissingValues = true;
			if (app.getTypes().contains(BigDataStackApplicationType.overrideValues)) overrideValues = true;
		}
		
		try {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			JsonNode node = mapper.readTree(yaml);
			
			String appID = null;
			if ((inferMissingValues && !node.has("appID")) || overrideValues) appID = app.getAppID();
			else if (node.has("appID")) appID = node.get("appID").asText();

			String namespace = null;
			if ((inferMissingValues && !node.has("namespace")) || overrideValues) namespace = app.getNamespace();
			else if (node.has("namespace")) namespace = node.get("namespace").asText();

			String owner = null;
			if ((inferMissingValues && !node.has("owner")) || overrideValues) owner = app.getOwner();
			else if (node.has("owner")) owner = node.get("owner").asText();
			
			List<String> notInStates = new ArrayList<String>();
			if (node.has("notInStates")) {
				JsonNode notInStatesA = node.get("notInStates");
				Iterator<JsonNode> notInStatesAI = notInStatesA.elements();
				while (notInStatesAI.hasNext()) notInStates.add(notInStatesAI.next().asText());
			}
			
			List<String> sequences = new ArrayList<String>();
			if (node.has("sequences")) {
				JsonNode sequencesA = node.get("sequences");
				Iterator<JsonNode> sequencesAI = sequencesA.elements();
				while (sequencesAI.hasNext()) sequences.add(sequencesAI.next().asText());
			}
			
			List<BigDataStackAppStateCondition> conditions = new ArrayList<BigDataStackAppStateCondition>();
			if (node.has("conditions")) {
				JsonNode conditionsA = node.get("conditions");
				Iterator<JsonNode> conditionsAI = conditionsA.elements();
				while (conditionsAI.hasNext()) {
					JsonNode conditionJSON = conditionsAI.next();
					List<String> objectIDs = new ArrayList<String>();
					if (conditionJSON.has("objectIDs")) {
						JsonNode objectIDsA = conditionJSON.get("objectIDs");
						Iterator<JsonNode> objectIDsAI = objectIDsA.elements();
						while (objectIDsAI.hasNext()) objectIDs.add(objectIDsAI.next().asText());
					}
					
					
					List<String> notInState = new ArrayList<String>();
					if (conditionJSON.has("notInState")) {
						JsonNode notInStateA = conditionJSON.get("notInState");
						Iterator<JsonNode> notInStateAIC = notInStateA.elements();
						while (notInStateAIC.hasNext()) notInState.add(notInStateAIC.next().asText());
					}
					
					String instances = null; if (conditionJSON.has("instances")) instances = conditionJSON.get("instances").asText();
					String state = null; if (conditionJSON.has("state")) state = conditionJSON.get("state").asText();
					String sequenceID = null; if (conditionJSON.has("sequenceID")) sequenceID = conditionJSON.get("sequenceID").asText();
					
					BigDataStackAppStateCondition condition = new BigDataStackAppStateCondition(objectIDs, instances, state, sequenceID, notInState);
					
					conditions.add(condition);
				}
					
			}
			
			String appStateID = null; if (node.has("appStateID")) appStateID = node.get("appStateID").asText();
			String name = null; if (node.has("name")) name = node.get("name").asText();
			
			BigDataStackAppState appState = new BigDataStackAppState(appID, owner, namespace, appStateID, name,
					notInStates, sequences, conditions);
			
			return appState;
		
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
