package eu.bigdatastack.gdt.operations;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import eu.bigdatastack.gdt.lxdb.BigDataStackAppStateIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackApplicationIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackEventIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackMetricIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackOperationSequenceIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackPodStatusIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackSLOIO;
import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackAppState;
import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackEvent;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetric;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectInstanceSummary;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectType;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;
import eu.bigdatastack.gdt.structures.data.BigDataStackPodStatus;
import eu.bigdatastack.gdt.structures.data.BigDataStackSLO;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.ApplicationStateUtil;
import eu.bigdatastack.gdt.util.EventUtil;

public class RegisterWithDynamicOrchestrator extends BigDataStackOperation {

	private String appID;
	private String owner;
	private String namepace;
	
	private String objectID;
	private int instance;
	private int replicasToIncreaseBy = 1;
	
	private String instanceRef = null;
	
	private boolean notConfigured = true;
	
	ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
	ObjectMapper jsonMapper = new ObjectMapper();
	
	private static String dynamicOrchestratorObjectID = "bigdatastackdoproxy";
	private static String dynamicOrchestratorAppID = "dynamicorchestrator";
	private static int dynamicOrchestratorPort = 5000;
	
	
	
	public RegisterWithDynamicOrchestrator() {
		this.className = this.getClass().getName();
	}
	
	public RegisterWithDynamicOrchestrator(String appID, String owner, String namepace) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;

		this.className = this.getClass().getName();
	}
	
	public RegisterWithDynamicOrchestrator(String appID, String owner, String namepace, String objectID, int instance) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.objectID = objectID;
		this.instance = instance;

		this.className = this.getClass().getName();
		
		notConfigured = false;
	}
	
	public RegisterWithDynamicOrchestrator(String appID, String owner, String namepace, String instanceRef) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;

		this.instanceRef = instanceRef;

		this.className = this.getClass().getName();
		
		notConfigured = false;
		
	}


	public String getAppID() {
		return appID;
	}

	public void setAppID(String appID) {
		this.appID = appID;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getNamespace() {
		return namepace;
	}

	public void setNamespace(String namepace) {
		this.namepace = namepace;
	}

	public String getObjectID() {
		return objectID;
	}

	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}
	
	public String getNamepace() {
		return namepace;
	}

	public void setNamepace(String namepace) {
		this.namepace = namepace;
	}

	public int getInstance() {
		return instance;
	}

	public void setInstance(int instance) {
		this.instance = instance;
	}

	public int getReplicasToIncreaseBy() {
		return replicasToIncreaseBy;
	}

	public void setReplicasToIncreaseBy(int replicasToIncreaseBy) {
		this.replicasToIncreaseBy = replicasToIncreaseBy;
	}

	@Override
	public String describeOperation() {
		return "Increases the replication factor of "+objectID+"("+instance+") by "+replicasToIncreaseBy;
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		
		if (instanceRef!=null || (objectID!=null && instance>0)) notConfigured = false;
		
		try {
			
			if (notConfigured) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"RegisterWithDynamicOrchestrator triggered but was not configured",
						"Operation sequence '"+parentSequenceRunner.getSequence().getSequenceID()+"("+parentSequenceRunner.getSequence().getIndex()+")' contained a request to register an object instance with the dynamic orchestrator, but the Operation was not configured (no target object instance could be identified).",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			}
			
			
			// Stage 1: Check that the target object exists and can be retrieved
			
			BigDataStackObjectIO objectInstanceClient = new BigDataStackObjectIO(database, false);
			
			BigDataStackObjectDefinition object = null;
			if (instanceRef!=null) {
				// Get object from instance reference
				if (!parentSequenceRunner.getSequence().getParameters().containsKey(instanceRef)) {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Error,
							"RegisterWithDynamicOrchestrator Operation Failed on lookup of: '"+instanceRef+"'",
							"Attempted to find an instance with within-sequence reference '"+instanceRef+"', but the parent sequence did not have an appropriate instance reference (did you Instantiate first?)",
							parentSequenceRunner.getSequence().getSequenceID(),
							parentSequenceRunner.getSequence().getIndex()
							);
					return false;
				}
				
				String sourceObjectID = parentSequenceRunner.getSequence().getParameters().get(instanceRef).split(":")[0];
				int instance = Integer.valueOf(parentSequenceRunner.getSequence().getParameters().get(instanceRef).split(":")[1]);
				object = objectInstanceClient.getObject(sourceObjectID, owner, instance);
			} else {
				object = objectInstanceClient.getObject(objectID, owner, instance);
			}
			
			
			
			if (object==null) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"RegisterWithDynamicOrchestrator Operation Failed: Object '"+getObjectID()+"("+instance+")' could not be found in the state database",
						"Attempted registration of '"+getObjectID()+"("+instance+")' with the dyanamic orchestrator: but failed as the object was not found",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			}
			
			
			// Stage 2: Check that there exists an instance of the dynamic orchestrator
			List<BigDataStackObjectDefinition> dynamicOrchestratorInstances = objectInstanceClient.getObjects(dynamicOrchestratorObjectID, owner, null, dynamicOrchestratorAppID);
			BigDataStackObjectDefinition instanceToUseForOrchestration = null;
			for (BigDataStackObjectDefinition doInstance : dynamicOrchestratorInstances) {
				if (doInstance.getStatus().contains("Available")) {
					instanceToUseForOrchestration = doInstance;
					break;
				}
			}
			
			if (instanceToUseForOrchestration==null) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"RegisterWithDynamicOrchestrator Operation Failed: Could not find a running Dyanmic Orchestrator instance in the state database",
						"Attempted registration of '"+getObjectID()+"("+instance+")' with the dyanamic orchestrator, but could not find an instance of the dyanmic orchestrator.",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			}
			
			// Stage 3: Check that the object type is correct
			if (object.getType()==BigDataStackObjectType.Job || object.getType()==BigDataStackObjectType.DeploymentConfig) {
				
				// Stage 4: Build Response
				BigDataStackObjectInstanceSummary summary = new BigDataStackObjectInstanceSummary();
				summary.setObject(object);
				
				BigDataStackApplicationIO applicationClient = new BigDataStackApplicationIO(database);
				BigDataStackApplication application = applicationClient.getApp(appID, owner, namepace);
				summary.setApplication(application);
				
				BigDataStackOperationSequenceIO sequenceClient = new BigDataStackOperationSequenceIO(database, true);
				List<BigDataStackOperationSequence> sequences = sequenceClient.getOperationSequences(owner, appID, null);
				summary.setSequences(sequences);

				BigDataStackSLOIO sloClient = new BigDataStackSLOIO(database);
				List<BigDataStackSLO> slos = sloClient.getSLOs(owner, appID, object.getObjectID(), object.getNamespace(), object.getInstance(), null, -1);
				summary.setSlos(slos);
				
				BigDataStackMetricIO metricClient = new BigDataStackMetricIO(database);
				List<BigDataStackMetric> metrics = metricClient.listMetrics(owner, null);
				summary.setMetrics(metrics);
				
				BigDataStackAppStateIO statesClient = new BigDataStackAppStateIO(database);
				List<BigDataStackAppState> possibleStates = statesClient.getAppStates(owner, appID, namepace, null);
				summary.setPossibleStates(possibleStates);
				
				List<BigDataStackAppState> currentStates = ApplicationStateUtil.getActiveStates(objectInstanceClient, new BigDataStackOperationSequenceIO(database, false), statesClient, owner, namepace, appID);
				summary.setCurrentStates(currentStates);
				
				BigDataStackEventIO eventClient = new BigDataStackEventIO(database);
				List<BigDataStackEvent> pastEvents = eventClient.getEvents(appID, owner, null, null, object.getObjectID(), object.getInstance(), -1, -1);
				summary.setPastEvents(pastEvents);
				
				
				// Stage 5: Get Connection End-point to send the data to
				BigDataStackPodStatusIO podClient = new BigDataStackPodStatusIO(database);
				List<BigDataStackPodStatus> pods = podClient.getPodStatuses(appID, owner, object.getObjectID(), namepace, object.getInstance());
				
				BigDataStackPodStatus podToSendTo = null;
				for (BigDataStackPodStatus pod : pods) {
					if (pod.getStatus()=="Running") {
						podToSendTo = pod;
						break;
					}
				}
				
				if (podToSendTo==null) {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Error,
							"RegisterWithDynamicOrchestrator Operation Failed: Could not find a running underlying pod for the Dyanmic Orchestrator to send to",
							"Attempted registration of '"+getObjectID()+"("+instance+")' with the dyanamic orchestrator, but could not find a running pod for the dynamic orchestrator to send to.",
							parentSequenceRunner.getSequence().getSequenceID(),
							parentSequenceRunner.getSequence().getIndex()
							);
				}
				
				// Stage 6: Send the Registration request
				int response = 404;
				String targetURL = "http://"+podToSendTo.getPodIP()+":"+dynamicOrchestratorPort;
				
				try {
					URL url = new URL(targetURL);
					URLConnection con = url.openConnection();
					HttpURLConnection http = (HttpURLConnection)con;
					http.setRequestMethod("POST"); // PUT is another valid option
					http.setDoOutput(true);
					byte[] out = jsonMapper.writeValueAsString(summary).getBytes(StandardCharsets.UTF_8);
					int length = out.length;

					http.setFixedLengthStreamingMode(length);
					http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
					http.connect();
					try(OutputStream os = http.getOutputStream()) {
					    os.write(out);
					}
					response = http.getResponseCode();
					http.disconnect();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (response>=200 && response<300) {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Info,
							"Registered: '"+getObjectID()+"("+instance+")' with the dyanmic orchestrator",
							"Successfully registered '"+getObjectID()+"("+instance+")' with the dyanamic orchestrator",
							parentSequenceRunner.getSequence().getSequenceID(),
							parentSequenceRunner.getSequence().getIndex()
							);
					return true;
				} else {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Error,
							"RegisterWithDynamicOrchestrator Operation Failed: Could not send request, recieved response code '"+response+"'",
							"Attempted registration of '"+getObjectID()+"("+instance+")' with the dyanamic orchestrator, but the send targeting '"+targetURL+"' failed with code '"+response+"'",
							parentSequenceRunner.getSequence().getSequenceID(),
							parentSequenceRunner.getSequence().getIndex()
							);
					return false;
				}
				
				
				
			} else {
				
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Warning,
						"RegisterWithDynamicOrchestrator Operation Skipped for: '"+getObjectID()+"("+instance+")' due to non-compatable objetc type '"+object.getType().name()+"'",
						"Attempted registration of '"+getObjectID()+"("+instance+")' with the dyanamic orchestrator: but failed as the object was not a Job or DeploymentConfig",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return true;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			
			return false;
		}
		
	}
	
	@Override
	public void initalizeFromJson(JsonNode configJson) {
		
		if (configJson.has("instanceRef")) {
			// Lookup By Instance Ref
			instanceRef = configJson.get("instanceRef").asText();
			notConfigured = false;
		} else if (configJson.has("objectID") && configJson.has("instance")) {
			// Lookup by object
			objectID = configJson.get("objectID").asText();
			instance = configJson.get("instance").asInt();
			notConfigured = false;
		}
		
	}
	
}
