import java.io.File;

import eu.bigdatastack.gdt.application.GDTManager;
import eu.bigdatastack.gdt.structures.config.GDTConfig;
import eu.bigdatastack.gdt.util.GDTFileUtil;

public class TestPlaybook {

	public static void main(String[] args) throws Exception {
		
		GDTConfig config = new GDTConfig(new File("gdt.config.json"));
		//TestUtil.clearDatabase(config.getDatabase());

		// Manager
		GDTManager manager = new GDTManager(config);
		
		manager.loadPlaybook(GDTFileUtil.file2String(new File("resources/dynamicorchestrator/bigdatastackdo.playbook.yaml"), "UTF-8"), "richardm", "richardmproject");
		
	}
	
}
