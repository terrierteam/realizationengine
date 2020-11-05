package eu.bigdatastack.gdt.structures.data;

import java.util.List;

/**
 * This is an aggregation of information about an applicaytion instance.
 * @author EbonBlade
 *
 */
public class BigDataStackObjectInstanceSummary {

	private String appID;
	private String name;
	private String description;
	private String owner;
	private String namespace;
	private List<BigDataStackApplicationType> types;
	List<BigDataStackObjectDefinition> objects;
	List<BigDataStackOperationSequence> sequences;
	List<BigDataStackSLO> slos;
	List<BigDataStackMetric> metrics;
	List<BigDataStackAppState> possibleStates;
	List<BigDataStackAppState> currentStates;
	List<BigDataStackEvent> pastEvents;
	
	public BigDataStackObjectInstanceSummary() {}

	public BigDataStackObjectInstanceSummary(String appID, String name, String description, String owner,
			String namespace, List<BigDataStackApplicationType> types, List<BigDataStackObjectDefinition> objects,
			List<BigDataStackOperationSequence> sequences, List<BigDataStackSLO> slos, List<BigDataStackMetric> metrics,
			List<BigDataStackAppState> possibleStates, List<BigDataStackAppState> currentStates,
			List<BigDataStackEvent> pastEvents) {
		super();
		this.appID = appID;
		this.name = name;
		this.description = description;
		this.owner = owner;
		this.namespace = namespace;
		this.types = types;
		this.objects = objects;
		this.sequences = sequences;
		this.slos = slos;
		this.metrics = metrics;
		this.possibleStates = possibleStates;
		this.currentStates = currentStates;
		this.pastEvents = pastEvents;
	}

	public String getAppID() {
		return appID;
	}

	public void setAppID(String appID) {
		this.appID = appID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public List<BigDataStackApplicationType> getTypes() {
		return types;
	}

	public void setTypes(List<BigDataStackApplicationType> types) {
		this.types = types;
	}

	public List<BigDataStackObjectDefinition> getObjects() {
		return objects;
	}

	public void setObjects(List<BigDataStackObjectDefinition> objects) {
		this.objects = objects;
	}

	public List<BigDataStackOperationSequence> getSequences() {
		return sequences;
	}

	public void setSequences(List<BigDataStackOperationSequence> sequences) {
		this.sequences = sequences;
	}

	public List<BigDataStackSLO> getSlos() {
		return slos;
	}

	public void setSlos(List<BigDataStackSLO> slos) {
		this.slos = slos;
	}

	public List<BigDataStackMetric> getMetrics() {
		return metrics;
	}

	public void setMetrics(List<BigDataStackMetric> metrics) {
		this.metrics = metrics;
	}

	public List<BigDataStackAppState> getPossibleStates() {
		return possibleStates;
	}

	public void setPossibleStates(List<BigDataStackAppState> possibleStates) {
		this.possibleStates = possibleStates;
	}

	public List<BigDataStackAppState> getCurrentStates() {
		return currentStates;
	}

	public void setCurrentStates(List<BigDataStackAppState> currentStates) {
		this.currentStates = currentStates;
	}

	public List<BigDataStackEvent> getPastEvents() {
		return pastEvents;
	}

	public void setPastEvents(List<BigDataStackEvent> pastEvents) {
		this.pastEvents = pastEvents;
	}


	
	
	
	
	
}
