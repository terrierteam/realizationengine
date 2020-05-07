package eu.bigdatastack.gdt.operations;

import java.sql.SQLException;

import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.EventUtil;

public class Apply extends BigDataStackOperation{

	private String objectID;
	private String appID;
	private String owner;
	private String namespace;
	
	public Apply() {}
	
	public Apply(String appID, String owner, String namepace, String objectID) {
		super();
		this.objectID = objectID;
		this.appID = appID;
		this.owner = owner;
		this.namespace = namepace;
		
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
	public void setNamespace(String namepace) {
		this.namespace = namepace;
	}

	@Override
	public String describeOperation() {
		return "Creates "+objectID+" on the Openshift Cluster in "+namespace+".";
	}

	@Override
	public boolean execute(LXDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner) {

		try {
			EventUtil eventUtil = new EventUtil(database, mailboxClient);
			
			// Stage 1: Retrieve template object
			BigDataStackObjectIO objectTemplateClient = new BigDataStackObjectIO(database, true);
			BigDataStackObjectDefinition templateObject = objectTemplateClient.getObject(getObjectID(), getOwner(), 0);
			if (templateObject==null) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Apply Operation Failed: '"+getObjectID()+"'",
						"Attempted to apply an object '"+getObjectID()+"', but was unable to find an associated object definition from available templates (did you upload a object with that ID?)",
						getObjectID()
						);
			}
			
			// Stage 2: Create a new object from the template
			BigDataStackObjectDefinition instanceObject = templateObject.clone();

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
						"Apply Operation Failed: '"+getObjectID()+"'",
						"Attempted to apply an object '"+getObjectID()+"', but was unable to register the new object instance definition",
						getObjectID()
						);
			}
			
			// Stage 5: Apply Object
			boolean applySuccessful = openshiftOperationClient.applyOperation(instanceObject);
			if (applySuccessful) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Info,
						"Apply Operation Excecuted for: '"+getObjectID()+"', instance "+instanceObject.getInstance(),
						"Executed an apply operation for '"+getObjectID()+"' instance "+instanceObject.getInstance(),
						getObjectID()
						);
			} else {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"Apply Operation Failed: '"+getObjectID()+"'",
						"Attempted to apply an object '"+getObjectID()+"', failed when communicating with Openshift",
						getObjectID()
						);
				return false;
			}
			
			
			
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		

		return true;
		
		
		
	}

	
	public String replaceDefaultParameters(String yaml, int instance) {
		yaml = yaml.replaceAll("\\$appID\\$", appID);
		yaml = yaml.replaceAll("\\$owner\\$", owner);
		yaml = yaml.replaceAll("\\$namespace\\$", namespace);
		yaml = yaml.replaceAll("\\$objectID\\$", objectID+"-"+instance);
		
		yaml = yaml.replaceAll("\\$appid\\$", appID);
		yaml = yaml.replaceAll("\\$objectid\\$", objectID+"-"+instance);
		
		yaml = yaml.replaceAll("\\$APPID\\$", appID);
		yaml = yaml.replaceAll("\\$OWNER\\$", owner);
		yaml = yaml.replaceAll("\\$NAMESPACE\\$", namespace);
		yaml = yaml.replaceAll("\\$OBJECTID\\$", objectID+"-"+instance);
		
		return yaml;
	}
	
}
