package eu.bigdatastack.gdt.structures.data;

import java.util.List;

/**
 * This is an aggregation of information about an applicaytion instance.
 * @author EbonBlade
 *
 */
public class BigDataStackObjectInstanceSummary {

	BigDataStackApplication application;
	BigDataStackObjectDefinition object;
	List<BigDataStackOperationSequence> sequences;
	List<BigDataStackSLO> slos;
	List<BigDataStackMetric> metrics;
	List<BigDataStackAppState> possibleStates;
	List<BigDataStackAppState> currentStates;
	List<BigDataStackEvent> pastEvents;
	
	public BigDataStackObjectInstanceSummary() {}
	
	public BigDataStackObjectInstanceSummary(BigDataStackApplication application, BigDataStackObjectDefinition object,
			List<BigDataStackOperationSequence> sequences, List<BigDataStackSLO> slos, List<BigDataStackMetric> metrics,
			List<BigDataStackAppState> possibleStates, List<BigDataStackAppState> currentStates,
			List<BigDataStackEvent> pastEvents) {
		super();
		this.application = application;
		this.object = object;
		this.sequences = sequences;
		this.slos = slos;
		this.metrics = metrics;
		this.possibleStates = possibleStates;
		this.currentStates = currentStates;
		this.pastEvents = pastEvents;
	}

	public BigDataStackApplication getApplication() {
		return application;
	}

	public void setApplication(BigDataStackApplication application) {
		this.application = application;
	}

	public BigDataStackObjectDefinition getObject() {
		return object;
	}

	public void setObject(BigDataStackObjectDefinition object) {
		this.object = object;
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
