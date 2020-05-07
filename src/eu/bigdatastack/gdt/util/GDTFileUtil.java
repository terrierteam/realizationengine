package eu.bigdatastack.gdt.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectType;

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
			String yamlSource = node.get("yamlSource").toPrettyString();
			
			BigDataStackObjectDefinition object = new BigDataStackObjectDefinition(objectID, owner, type,
					yamlSource, new HashSet<String>());
			
			return object;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
}
