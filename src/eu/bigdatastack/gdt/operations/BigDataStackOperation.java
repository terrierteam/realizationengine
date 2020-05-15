package eu.bigdatastack.gdt.operations;

import com.fasterxml.jackson.databind.JsonNode;

import eu.bigdatastack.gdt.lxdb.LXDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.EventUtil;

/**
 * This interface represents an operation that can be done on the openshift cluster.
 * @author EbonBlade
 *
 */
public abstract class BigDataStackOperation {

	private BigDataStackOperationState state = BigDataStackOperationState.NotStarted;
	protected String className;
	protected String objectID = null;
	
	public abstract String getAppID();

	public abstract String getOwner();

	public abstract String getNamespace();
	
	/**
	 * Gets the target objectID of this operation, not all operations have a target, and hence may return null
	 * @return
	 */
	public String getObjectID() {
		return objectID;
	}
	
	public abstract String describeOperation();
	

	public BigDataStackOperationState getState() {
		return state;
	}

	public void setState(BigDataStackOperationState state) {
		this.state = state;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	
	/**
	 * Executes the operation
	 * @param database - provides access to object state (use this to instantiate IO clients) 
	 * @param openshiftOperationClient - enables actions to be taken on the cluster
	 * @param openshiftStatusClient - enables cluster state to be retrieved
	 * @param mailboxClient - use this to read/write events
	 * @param prometheusDataClient - use this to get application metrics
	 * @return
	 */
	public abstract boolean execute(
			LXDB database,
			OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient,
			RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient,
			OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil);
	
	/**
	 * Configures this operation from a provided json definition
	 * @return
	 */
	public abstract void initalizeFromJson(JsonNode configJson);
	
	public abstract void setAppID(String appID);
	
	public abstract void setOwner(String owner);
	
	public abstract void setNamespace(String namespace);
}
