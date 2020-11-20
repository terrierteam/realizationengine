package eu.bigdatastack.gdt.operations;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.openshift.OpenshiftObject;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.EventUtil;

public class InstanceRefFromObjectLookup extends BigDataStackOperation{

	private String appID;
	private String owner;
	private String namepace;
	
	private String parameter;
	private String criteria;
	private boolean allowMultipleMatches = false;
	private boolean selectFirst = false;
	
	public InstanceRefFromObjectLookup() {
		this.className = this.getClass().getName();
	}
	
	public InstanceRefFromObjectLookup(String appID, String owner, String namepace, String parameter, String criteria) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.criteria = criteria;
		this.parameter =parameter;
		
		this.className = this.getClass().getName();
	}
	
	public InstanceRefFromObjectLookup(String appID, String owner, String namepace, String parameter, String criteria, boolean allowMultiplerMatches, boolean selectFirst) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namepace = namepace;
		this.criteria = criteria;
		this.parameter =parameter;
		this.allowMultipleMatches= allowMultiplerMatches;
		this.selectFirst = selectFirst;
		
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

	public String getNamepace() {
		return namepace;
	}

	public void setNamepace(String namepace) {
		this.namepace = namepace;
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	public String getCriteria() {
		return criteria;
	}

	public void setCriteria(String criteria) {
		this.criteria = criteria;
	}

	public boolean isAllowMultipleMatches() {
		return allowMultipleMatches;
	}

	public void setAllowMultipleMatches(boolean allowMultipleMatches) {
		this.allowMultipleMatches = allowMultipleMatches;
	}

	public boolean isSelectFirst() {
		return selectFirst;
	}

	public void setSelectFirst(boolean selectFirst) {
		this.selectFirst = selectFirst;
	}

	@Override
	public String describeOperation() {
		return "Records the parameter "+parameter+" to an objectID determined by a criteria search on openshift";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {
		
		
		try {
			String[] criteriaParts = criteria.split(":");
			String namespace = criteriaParts[0];
			String kind = criteriaParts[1];
			String nameregex = criteriaParts[2];
			
			List<OpenshiftObject> resources = openshiftStatusClient.getResources(namespace, kind);
			List<OpenshiftObject> matchedResources = new ArrayList<OpenshiftObject>();
			for (OpenshiftObject resource : resources) {
				if (resource.getName().matches(nameregex)) {
					matchedResources.add(resource);
				}
			}
			
			if (matchedResources.size()==0) {
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Error,
						"InstanceRef From Object Lookup Operation Failed using criteria '"+criteria+"'",
						"Attempted to match an object in namespace '"+namespace+"' of kind '"+kind+"' with name '"+nameregex+"' but no matches were found.",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return false;
			} if (matchedResources.size()>1) {
				if (allowMultipleMatches) {
					List<String> matches = new ArrayList<String>(matchedResources.size());
					for (OpenshiftObject resource : matchedResources) {
						matches.add(resource.getName());
					}
					ObjectMapper mapper = new ObjectMapper();
					parentSequenceRunner.getSequence().getParameters().put(parameter, mapper.writeValueAsString(matches));
					return true;
				} else if (selectFirst) {
					String appID = matchedResources.get(0).getName().split("-")[0];
					String instance = matchedResources.get(0).getName().split("-")[matchedResources.get(0).getName().split("-").length-1];
					String objectID = matchedResources.get(0).getName().substring(appID.length(), matchedResources.get(0).getName().length());
					objectID = objectID.substring(1, objectID.length()-(instance.length()+1));
					String reference = objectID+":"+instance;
					
					parentSequenceRunner.getSequence().getParameters().put(parameter, reference);
					eventUtil.registerEvent(
							getAppID(),
							getOwner(),
							getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Info,
							"InstanceRef From Object Lookup Operation Completed: '"+parameter+"' -> '"+reference+"'",
							"Matched an object in namespace'"+namespace+"' of kind '"+kind+"' with name '"+nameregex+"' and used it to set parameter '"+parameter+"' to '"+reference+"'",
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
							"InstanceRef From Object Lookup Operation Failed using criteria '"+criteria+"'",
							"Attempted to match an object in namespace '"+namespace+"' of kind '"+kind+"' with name '"+nameregex+"' but found multiple matches and multipleMatches was not allowed.",
							parentSequenceRunner.getSequence().getSequenceID(),
							parentSequenceRunner.getSequence().getIndex()
							);
					return false;
				}
			} else {
				String appID = matchedResources.get(0).getName().split("-")[0];
				String instance = matchedResources.get(0).getName().split("-")[matchedResources.get(0).getName().split("-").length-1];
				String objectID = matchedResources.get(0).getName().substring(appID.length(), matchedResources.get(0).getName().length());
				objectID = objectID.substring(1, objectID.length()-(instance.length()+1));
				String reference = objectID+":"+instance;
				
				parentSequenceRunner.getSequence().getParameters().put(parameter, reference);
				eventUtil.registerEvent(
						getAppID(),
						getOwner(),
						getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Info,
						"InstanceRef From Object Lookup Operation Completed: '"+parameter+"' -> '"+matchedResources.get(0).getName()+"'",
						"Matched an object in namespace'"+namespace+"' of kind '"+kind+"' with name '"+nameregex+"' and used it to set parameter '"+parameter+"'",
						parentSequenceRunner.getSequence().getSequenceID(),
						parentSequenceRunner.getSequence().getIndex()
						);
				return true;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
		
	}
	
	@Override
	public void initalizeFromJson(JsonNode configJson) {
		parameter = configJson.get("defineInstanceRef").asText();
		criteria = configJson.get("criteria").asText();
		if (configJson.has("multipleMatches")) {
			String multipleMatches = configJson.get("multipleMatches").asText();
			if (multipleMatches.equalsIgnoreCase("SelectFirst")) selectFirst = true;
			if (multipleMatches.equalsIgnoreCase("Allow")) allowMultipleMatches = true;
		}
	}

	@Override
	public String getObjectID() {
		return "ParentSequence";
	}
	

}
