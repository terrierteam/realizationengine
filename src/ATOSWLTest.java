import java.io.File;

import eu.bigdatastack.gdt.application.GDTManager;
import eu.bigdatastack.gdt.structures.config.GDTConfig;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetric;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;
import test.TestUtil;

public class ATOSWLTest {

	public static void main(String[] args) throws Exception {

		// Config
		GDTConfig config = new GDTConfig(new File("gdt.config.json"));
		//TestUtil.clearDatabase(config.getDatabase());

		// Manager
		GDTManager manager = new GDTManager(config);
		
		
		/*BigDataStackApplication app = manager.registerApplication(new File("resources/atoswl/atoswl.app.yaml"));
		if (app==null) return;
		
		BigDataStackMetric ndcg_at_k_valid = manager.registerMetric(new File("resources/atoswl/ndcg_at_k_valid.metric.yaml"));
		if (ndcg_at_k_valid==null) return;
		
		
		BigDataStackObjectDefinition object = manager.registerObject(new File("resources/atoswl/atoswl-train.job.yaml"));
		if (object==null) return;*/
		
		BigDataStackOperationSequence seq = manager.registerOperationSequence(new File("resources/atoswl/atoswl-trainmodel.seq.yaml"));
		if (seq==null) return;
		
		manager.executeSequenceFromTemplateSync(seq);
	}

}
