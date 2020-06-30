package eu.bigdatastack.gdt.operations;

import com.fasterxml.jackson.databind.JsonNode;

import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackOperationSequenceIO;
import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.EventUtil;

public class Instantiate extends BigDataStackOperation{

	private String appID;
	private String owner;
	private String namespace;

	private String objectID;
	private String seqInstanceRef;

	public Instantiate() {
		this.className = this.getClass().getName();
	}

	public Instantiate(String appID, String owner, String namespace, String objectID, String defineInstanceRef) {
		super();
		this.objectID = objectID;
		this.appID = appID;
		this.owner = owner;
		this.namespace = namespace;
		this.seqInstanceRef = defineInstanceRef;

		this.className = this.getClass().getName();
	}
	public String getObjectID() {
		return objectID;
	}
	public void setObjectID(String objectID) {
		this.objectID = objectID;
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
		return namespace;
	}
	public void setNamepace(String namespace) {
		this.namespace = namespace;
	}

	public String getSeqInstanceRef() {
		return seqInstanceRef;
	}

	public void setSeqInstanceRef(String seqInstanceRef) {
		this.seqInstanceRef = seqInstanceRef;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@Override
	public String describeOperation() {
		return "Creates an instance of "+objectID+", it can be referred to within the sequence as '"+seqInstanceRef+"'";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {

		try {

			// Stage 1: Retrieve instance object
			BigDataStackObjectIO objectTemplateClient = new BigDataStackObjectIO(database, true);
			BigDataStackObjectDefinition templateObject = objectTemplateClient.getObject(getObjectID(), getOwner(), 0);
			if (templateObject==null) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Object Instantiate Operation Failed: '"+getObjectID()+"'",
						"Attempted to create a new instance of object '"+getObjectID()+"', but was unable to find an associated object definition from available instances.",
						getObjectID()
						);
				return false;
			}

			// Stage 2: Create a new object from the template
			BigDataStackObjectDefinition instanceObject = templateObject.clone();
			instanceObject.setNamespace(namespace);
			instanceObject.setAppID(appID);

			// Stage 3: Register object instance
			BigDataStackObjectIO objectInstanceClient = new BigDataStackObjectIO(database, false);

			int registerFailures =0;
			boolean hasRegistered = false;

			while (!hasRegistered) {
				int highestInstanceID = objectInstanceClient.getObjectCount(objectID, owner);
				int newInstanceID = highestInstanceID++;
				instanceObject.setInstance(newInstanceID);

				// Overwrite any default parameters listed in the yaml
				String updatedYaml = replaceDefaultParameters(instanceObject.getYamlSource(), newInstanceID);
				instanceObject.setYamlSource(updatedYaml);


				if (!objectInstanceClient.addObject(instanceObject)) {
					registerFailures++;
					if (registerFailures>=5) break;
				}

				hasRegistered = true;

			}

			if (!hasRegistered) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Object Instantiate Operation Failed: '"+getObjectID()+"'",
						"Attempted to create a new instance of object '"+getObjectID()+"', but failed when attempting to register the new instance with the database.",
						getObjectID()
						);
				return false;
			} else {
				parentSequenceRunner.getSequence().getParameters().put(seqInstanceRef, getObjectID()+":"+String.valueOf(instanceObject.getInstance()));
				// we have just changed the information stored in the sequence instance, so sync that with the db
				BigDataStackOperationSequenceIO sequenceInstanceClient = new BigDataStackOperationSequenceIO(database,false);
				sequenceInstanceClient.updateSequence(parentSequenceRunner.getSequence());
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Info,
						"Object Instantiate Operation Completed: '"+getObjectID()+"("+instanceObject.getInstance()+")'",
						"Created a new instance of object '"+getObjectID()+"("+instanceObject.getInstance()+")'",
						getObjectID()
						);
				
			}


		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;

	}
	
	
	public String replaceDefaultParameters(String yaml, int instance) {
		yaml = yaml.replaceAll("\\$appID\\$", appID);
		yaml = yaml.replaceAll("\\$owner\\$", owner);
		yaml = yaml.replaceAll("\\$namespace\\$", namespace);
		yaml = yaml.replaceAll("\\$objectID\\$", objectID);
		yaml = yaml.replaceAll("\\$instance\\$", String.valueOf(instance));
		
		yaml = yaml.replaceAll("\\$appid\\$", appID);
		yaml = yaml.replaceAll("\\$objectid\\$", objectID);
		
		yaml = yaml.replaceAll("\\$APPID\\$", appID);
		yaml = yaml.replaceAll("\\$OWNER\\$", owner);
		yaml = yaml.replaceAll("\\$NAMESPACE\\$", namespace);
		yaml = yaml.replaceAll("\\$OBJECTID\\$", objectID);
		yaml = yaml.replaceAll("\\$INSTANCE\\$", String.valueOf(instance));
		
		return yaml;
	}
	
	@Override
	public void initalizeFromJson(JsonNode configJson) {
		objectID = configJson.get("objectID").asText();
		seqInstanceRef = configJson.get("defineInstanceRef").asText();
	}
}
