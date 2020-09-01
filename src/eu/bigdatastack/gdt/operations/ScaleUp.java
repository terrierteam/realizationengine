package eu.bigdatastack.gdt.operations;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectType;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.EventUtil;

public class ScaleUp extends BigDataStackOperation {

	private String appID;
	private String owner;
	private String namepace;
	
	private String objectID;
	private int instance;
	private int replicasToIncreaseBy = 1;
	
	ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
	
	
	public ScaleUp() {
		this.className = this.getClass().getName();
	}
	
	public ScaleUp(String appID, String owner, String namepace, String objectID, int instance) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.objectID = objectID;
		this.instance = instance;

		this.className = this.getClass().getName();
	}
	
	

	public ScaleUp(String appID, String owner, String namepace, String objectID, int instance, int replicasToIncreaseBy) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.objectID = objectID;
		this.instance = instance;
		
		this.replicasToIncreaseBy = replicasToIncreaseBy;
		
		this.className = this.getClass().getName();
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
		
		
		try {
			BigDataStackObjectIO objectInstanceClient = new BigDataStackObjectIO(database, false);
			
			BigDataStackObjectDefinition object = objectInstanceClient.getObject(objectID, owner, instance);
			if (object==null) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"ScaleUp Operation Failed on: '"+getObjectID()+"("+instance+")'",
						"Attempted scaleing up of pod count on '"+getObjectID()+"("+instance+")': but failed as the object was not found",
						getObjectID()
						);
				return false;
			}
			
			if (object.getType()==BigDataStackObjectType.Job || object.getType()==BigDataStackObjectType.DeploymentConfig) {
				
				ObjectNode objectYaml = (ObjectNode) yamlMapper.readTree(object.getYamlSource());
				if (object.getType()==BigDataStackObjectType.Job) {
					ObjectNode spec = (ObjectNode) objectYaml.get("spec");
					spec.put("parallelism", spec.get("parallelism").asInt()+replicasToIncreaseBy);
				}
				
				if (object.getType()==BigDataStackObjectType.DeploymentConfig) {
					ObjectNode spec = (ObjectNode) objectYaml.get("spec");
					spec.put("replicas", spec.get("replicas").asInt()+replicasToIncreaseBy);
				}
				
				object.setYamlSource(yamlMapper.writeValueAsString(objectYaml));
				
				objectInstanceClient.updateObject(object);
				
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Info,
						"ScaleUp Operation Updated Definition of: '"+getObjectID()+"("+instance+")'",
						"Increased pod count on '"+getObjectID()+"("+instance+")'.",
						getObjectID()
						);
				
				boolean applySuccessful = openshiftOperationClient.applyOperation(object);
				
				if (applySuccessful) {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Info,
							"ScaleUp Operation Completed on: '"+getObjectID()+"("+instance+")'",
							"Scale-up operation applied to the cluster for '"+getObjectID()+"("+instance+")'.",
							getObjectID()
							);
					return true;
				} else {
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Error,
							"ScaleUp Operation failed to apply to the cluster object '"+getObjectID()+"("+instance+")'",
							"Failed to Increased pod count on '"+getObjectID()+"("+instance+")', because the cluster rejected it",
							getObjectID()
							);
					return false;
				}
				
				
			
				
			} else {
				
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"ScaleUp Operation Failed on: '"+getObjectID()+"("+instance+")'",
						"Attempted scaleing up of pod count on '"+getObjectID()+"("+instance+")': but failed as the object was not a Job or DeploymentConfig",
						getObjectID()
						);
				return false;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			
			return false;
		}
		
	}
	
	@Override
	public void initalizeFromJson(JsonNode configJson) {
		objectID = configJson.get("objectID").asText();
		instance = configJson.get("instance").asInt();
		if (configJson.has("increaseBy")) replicasToIncreaseBy = configJson.get("increaseBy").asInt();
		
	}

}
