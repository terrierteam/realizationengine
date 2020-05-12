import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bigdatastack.gdt.lxdb.BigDataStackApplicationIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackEventIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackMetricIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackOperationSequenceIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackPodStatusIO;
import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.operations.Apply;
import eu.bigdatastack.gdt.operations.BigDataStackOperation;
import eu.bigdatastack.gdt.operations.BigDataStackOperationState;
import eu.bigdatastack.gdt.operations.Build;
import eu.bigdatastack.gdt.operations.WaitFor;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplicationType;
import eu.bigdatastack.gdt.structures.data.BigDataStackEvent;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetric;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetricClassname;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectType;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequenceMode;
import eu.bigdatastack.gdt.structures.data.BigDataStackPodStatus;

public class GDTSetupAndTest {

	public static void main(String[] args) throws SQLException, IOException {
		
		ObjectMapper mapper = new ObjectMapper();
		
		LXDB database = new LXDB("gdtdb-richardmproject.ida.dcs.gla.ac.uk", 80, "BigDataStackGDTDB", "GDT");
		
		BigDataStackApplicationIO appClient = new BigDataStackApplicationIO(database);
		
		
		List<BigDataStackApplicationType> appTypes = new ArrayList<BigDataStackApplicationType>();
		appTypes.add(BigDataStackApplicationType.playbook);
		
		BigDataStackApplication app = new BigDataStackApplication(
				"app-01",
				"my app",
				"this is my app",
				"richardm",
				"richardmproject",
				appTypes);
		
		System.err.println(appClient.addApplication(app));
		
		System.err.println(mapper.writeValueAsString(appClient.getApp("app-01", "richardm", "richardmproject")));
		
		app.setName("my updated app");
		System.err.println(appClient.updateApp(app));
		
		System.err.println(mapper.writeValueAsString(appClient.getApp("app-01", "richardm", "richardmproject")));
		
		Set<String> oStatus = new HashSet<String>();
		oStatus.add("Starting");
		
		BigDataStackObjectDefinition myObject = new BigDataStackObjectDefinition(
				"obj20",
				"richardm",
				BigDataStackObjectType.Playbook,
				"Bla bla, bla",
				oStatus);
		
		BigDataStackObjectIO objectIO = new BigDataStackObjectIO(database, true);
		System.err.println("CLEAR: "+objectIO.clearTable());
		
		System.err.println(objectIO.addObject(myObject));
		
		System.err.println(mapper.writeValueAsString(objectIO.getObject(myObject.getObjectID(), myObject.getOwner(), 0)));
		
		myObject.setYamlSource("new bla");
		
		System.err.println(objectIO.updateObject(myObject));
		
		System.err.println(mapper.writeValueAsString(objectIO.getObject(myObject.getObjectID(), myObject.getOwner(), 0))); 
		
		
		BigDataStackEvent event1 = new BigDataStackEvent(
				"app-01",
				"richardm",
				1,
				"richardmproject",
				BigDataStackEventType.User,
				BigDataStackEventSeverity.Info,
				"New Playbook: app-01",
				"User richardm registered a new playbook app-01 with BigDataStack.",
				""
				);
		
		BigDataStackEventIO eventIO = new BigDataStackEventIO(database);
		
		System.err.println(eventIO.addEvent(event1));
		
		for (BigDataStackEvent event : eventIO.getEvents("app-01", "richardm")) {
			System.err.println(mapper.writeValueAsString(event)); 
		}
		
		BigDataStackMetric metric = new BigDataStackMetric(
				"richardm",
				"NDCG@10",
				BigDataStackMetricClassname.Double,
				"Discounted cumulative gain (DCG) is a measure of ranking quality. In information retrieval, it is often used to measure effectiveness of web search engine algorithms or related applications. Using a graded relevance scale of documents in a search-engine result set, DCG measures the usefulness, or gain, of a document based on its position in the result list. The gain is accumulated from the top of the result list to the bottom, with the gain of each result discounted at lower ranks. Search result lists vary in length depending on the query. Comparing a search engine's performance from one query to the next cannot be consistently achieved using DCG alone, so the cumulative gain at each position for a chosen value of p {\\displaystyle p} p should be normalized across queries. This is done by sorting all relevant documents in the corpus by their relative relevance, producing the maximum possible DCG through position p {\\displaystyle p} p, also called Ideal DCG (IDCG) through that position.",
				1.0,
				0.0,
				true,
				"NDCG@10");
		
		BigDataStackMetricIO metricIO = new BigDataStackMetricIO(database);
		
		System.err.println(metricIO.addMetric(metric));
		
		System.err.println(mapper.writeValueAsString(metricIO.getMetric(metric.getOwner(), metric.getName()))); 
		
		Apply applyBuild = new Apply("app-01", "richardm", "richardmproject", "build01");
		Build build = new Build("app-01", "richardm", "richardmproject", "build01");
		WaitFor waitForBuild = new WaitFor("app-01", "richardm", "richardmproject", "build01", "Completed");
		
		List<BigDataStackOperation> operations = new ArrayList<BigDataStackOperation>(3);
		operations.add(applyBuild);
		operations.add(build);
		operations.add(waitForBuild);
		
		BigDataStackOperationSequence sequence = new BigDataStackOperationSequence(
				"app-01", 
				"richardm", 
				"richardmproject",
				"sequence-01",
				"ApplyBuildWait",
				"Example",
				operations,
				BigDataStackOperationSequenceMode.Run);
		
		BigDataStackOperationSequenceIO sequenceIO = new BigDataStackOperationSequenceIO(database, true);
		
		System.err.println(sequenceIO.addSequence(sequence));
		
		System.err.println(mapper.writeValueAsString(sequenceIO.getSequence(sequence.getAppID(), sequence.getSequenceID()))); 
		
		operations.get(1).setState(BigDataStackOperationState.InProgress);
		
		System.err.println(sequenceIO.updateSequence(sequence));
		
		System.err.println(mapper.writeValueAsString(sequenceIO.getSequence(sequence.getAppID(), sequence.getSequenceID()))); 
		
		
		BigDataStackPodStatus status = new BigDataStackPodStatus(
				"app-01",
				"richardm",
				"richardmproject",
				"object0",
				"pod0",
				"Running",
				"0.0.0.0",
				"0.0.0.0"
				);
		
		
		BigDataStackPodStatusIO podStatusIO = new BigDataStackPodStatusIO(database);
		
		System.err.println(podStatusIO.addPodStatus(status));
		
		System.err.println(mapper.writeValueAsString(podStatusIO.getPodStatus(status.getPodID())));
		
		status.setStatus("Complete");
		
		System.err.println(podStatusIO.updatePodStatus(status));
		
		System.err.println(mapper.writeValueAsString(podStatusIO.getPodStatus(status.getPodID())));
	}
	
}
