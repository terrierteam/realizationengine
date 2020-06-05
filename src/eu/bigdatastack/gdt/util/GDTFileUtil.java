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
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import eu.bigdatastack.gdt.application.GDTManager;
import eu.bigdatastack.gdt.operations.BigDataStackOperation;
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
	public static BigDataStackObjectDefinition readObjectFromString(String yaml) {
		
		try {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			JsonNode node = mapper.readTree(yaml);
			
			
			String objectID = node.get("objectID").asText();
			String owner = node.get("owner").asText();
			BigDataStackObjectType type = BigDataStackObjectType.valueOf(node.get("type").asText());
			String yamlSource = mapper.writeValueAsString(node.get("yamlSource"));
			
			BigDataStackObjectDefinition object = new BigDataStackObjectDefinition(objectID, owner, type,
					yamlSource, new HashSet<String>());
			
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
		
		return readSequenceFromString(yaml, null);
	}
	
	/**
	 * Reads in a BigDataStackOperationSequence from a yaml format string. Note that this relies on implementations of
	 * initalizeFromJson within each Operation class to parse the contents of the operation definitions
	 * @param yaml
	 * @return
	 */
	public static BigDataStackOperationSequence readSequenceFromString(String yaml, String namespace) {
		try {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			JsonNode node = mapper.readTree(yaml);
			
			
			String appID = node.get("appID").asText();
			String owner = node.get("owner").asText();
			if (namespace==null && node.has("namespace")) namespace = node.get("namespace").asText();
			String sequenceID = node.get("sequenceID").asText();
			
			
			yaml = yaml.replaceAll("\\$appID\\$", appID);
			yaml = yaml.replaceAll("\\$owner\\$", owner);
			yaml = yaml.replaceAll("\\$namespace\\$", namespace);
			yaml = yaml.replaceAll("\\$sequenceID\\$", sequenceID);
			
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
	
	
	
	
}
