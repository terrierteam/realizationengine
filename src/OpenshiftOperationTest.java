import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import eu.bigdatastack.gdt.openshift.OpenshiftOperationClientv3;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectType;
import eu.bigdatastack.gdt.util.GDTFileUtil;

public class OpenshiftOperationTest {

	public static void main(String[] args) throws Exception {
		OpenshiftOperationClientv3 operationClient = new OpenshiftOperationClientv3("https://idagpu-head.dcs.gla.ac.uk:8443/", "admin", "IDAAdmin2019");
		operationClient.connectToOpenshift();
		
		String yaml = GDTFileUtil.file2String(new File("resources/openshift/example-service.yaml"), "UTF-8");
		Set<String> oStatus = new HashSet<String>();
		oStatus.add("Starting");
		
		BigDataStackObjectDefinition myObject = new BigDataStackObjectDefinition(
				"example",
				"richardm",
				BigDataStackObjectType.Service,
				yaml,
				oStatus);
		
		operationClient.applyOperation(myObject);
		
		operationClient.close();
	}
	
}
