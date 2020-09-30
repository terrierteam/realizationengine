package eu.bigdatastack.gdt.threads;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;

import eu.bigdatastack.gdt.lxdb.BigDataStackEventIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackObjectIO;
import eu.bigdatastack.gdt.lxdb.BigDataStackOperationSequenceIO;
import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.operations.BigDataStackOperation;
import eu.bigdatastack.gdt.operations.BigDataStackOperationState;
import eu.bigdatastack.gdt.operations.Delete;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventSeverity;
import eu.bigdatastack.gdt.structures.data.BigDataStackEventType;
import eu.bigdatastack.gdt.structures.data.BigDataStackObjectDefinition;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequenceMode;
import eu.bigdatastack.gdt.util.EventUtil;

/**
 * This thread contains all of the logic for running a BigDataStack operation
 * sequence. This may interact with the Openshift cluster and update the database
 * during execution. 
 * @author EbonBlade
 *
 */
public class OperationSequenceThread implements Runnable {

	OpenshiftOperationClient operationsClient;
	OpenshiftStatusClient statusClient;
	JDBCDB database;
	RabbitMQClient mailboxClient;
	PrometheusDataClient prometheusDataClient;
	BigDataStackOperationSequence sequence;
	EventUtil eventUtil;
	
	boolean kill = false;
	boolean failed = false;

	private Map<String,String> newParameters = null;

	public OperationSequenceThread(JDBCDB database,
			OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient,
			RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, 
			BigDataStackOperationSequence sequence) {
		super();
		this.operationsClient = openshiftOperationClient;
		this.statusClient = openshiftStatusClient;
		this.mailboxClient =mailboxClient;
		this.prometheusDataClient = prometheusDataClient;
		this.database = database;
		this.sequence = sequence;
	}

	public OperationSequenceThread(JDBCDB database,
			OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient,
			RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, 
			BigDataStackOperationSequence sequence,
			Map<String,String> newParameters) {
		super();
		this.operationsClient = openshiftOperationClient;
		this.statusClient = openshiftStatusClient;
		this.mailboxClient =mailboxClient;
		this.prometheusDataClient = prometheusDataClient;
		this.database = database;
		this.sequence = sequence;
		this.newParameters = newParameters;
	}
	
	public OperationSequenceThread(JDBCDB database,
			OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient,
			RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, 
			BigDataStackOperationSequence sequence,
			EventUtil eventUtil) {
		super();
		this.operationsClient = openshiftOperationClient;
		this.statusClient = openshiftStatusClient;
		this.mailboxClient =mailboxClient;
		this.prometheusDataClient = prometheusDataClient;
		this.database = database;
		this.sequence = sequence;
		this.eventUtil = eventUtil;
	}

	public OperationSequenceThread(JDBCDB database,
			OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient,
			RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, 
			BigDataStackOperationSequence sequence,
			Map<String,String> newParameters,
			EventUtil eventUtil) {
		super();
		this.operationsClient = openshiftOperationClient;
		this.statusClient = openshiftStatusClient;
		this.mailboxClient =mailboxClient;
		this.prometheusDataClient = prometheusDataClient;
		this.database = database;
		this.sequence = sequence;
		this.newParameters = newParameters;
		this.eventUtil = eventUtil;
	}

	@Override
	public void run() {

		BigDataStackObjectIO objectIO = null;
		try {
			objectIO = new BigDataStackObjectIO(database, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Stage 1: Instantiate Instance of Sequence
		try {
			if (this.eventUtil==null) {
				BigDataStackEventIO eventClient = new BigDataStackEventIO(database);
				this.eventUtil = new EventUtil(eventClient, mailboxClient);
			}

			BigDataStackOperationSequenceIO sequenceIO = new BigDataStackOperationSequenceIO(database, false);
			
			
			if (sequence.getIndex()<=0) {
				// this is a template, so we need to instantiate
				boolean sequenceAddedOK = tryAddSequence(sequenceIO);
				if (!sequenceAddedOK) {
					eventUtil.registerEvent(
							sequence.getAppID(),
							sequence.getOwner(),
							sequence.getNamespace(),
							BigDataStackEventType.ObjectRegistry,
							BigDataStackEventSeverity.Error,
							"Operation Sequence Creation Failed for: '"+sequence.getSequenceID()+"'",
							"Tried to create a new operation sequence for app '"+sequence.getAppID()+"' but failed when adding to the Object Registry (SequenceID='"+sequence.getSequenceID()+"', Index='"+sequence.getIndex()+"')",
							sequence.getSequenceID(),
							sequence.getIndex()
							);
					failed = true;
					return;
				}
				
				eventUtil.registerEvent(
						sequence.getAppID(),
						sequence.getOwner(),
						sequence.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Info,
						"New Operation Sequence Created: '"+sequence.getSequenceID()+"'",
						"The user created a new operation sequence for app '"+sequence.getAppID()+"', it has been registered and is being processed (SequenceID='"+sequence.getSequenceID()+"', Index='"+sequence.getIndex()+"')",
						sequence.getSequenceID(),
						sequence.getIndex()
						);
			}
			
			
			
		} catch (SQLException | JsonProcessingException e) {
			e.printStackTrace();
			failed = true;
			return;
		}
		
		BigDataStackObjectDefinition runnerObject = null;
		if (sequence.getParameters().containsKey("runnerIndex")) {
			// we are in a containerized runner, so we should keep the state of the runner object updated as well
			try {
				
				runnerObject = objectIO.getObject("operationsequence", "gdt", Integer.parseInt(sequence.getParameters().get("runnerIndex")));
				
				if (runnerObject!=null) {
					Set<String> runnerStatus = new HashSet<String>();
					runnerStatus.add("Progressing");
					objectIO.updateObject(runnerObject);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (kill || failed) {
			if (runnerObject!=null) {
				Set<String> runnerStatus = new HashSet<String>();
				runnerStatus.add("Failed");
				try {
					objectIO.updateObject(runnerObject);
				} catch (Exception e) {
					e.printStackTrace();
				}
				operationsClient.deleteOperation(runnerObject); // delete self;
			}
			
			return;
		}

		// Stage 2: Process Operations
		BigDataStackOperationSequenceMode mode = sequence.getMode();
		for (BigDataStackOperation operation : sequence.getOperations()) {
			try {
				BigDataStackOperationSequenceIO sequenceIO = new BigDataStackOperationSequenceIO(database, false);
				
				BigDataStackOperationState currentState = operation.getState();
				boolean operationSucceeded = false;
				if (kill || failed) return;
				switch (currentState) {
				case NotStarted:
					// clean state, ready to run
					operation.setState(BigDataStackOperationState.InProgress);
					sequenceIO.updateSequence(sequence);
					operationSucceeded = operation.execute(database, operationsClient, statusClient, mailboxClient, prometheusDataClient, this, eventUtil);
					break; 
				case InProgress:
					// We are in a bad situation, where a previous operation sequence run left the sequence
					// in an unknown state, we will need to clean up before running again
					if (!tryCleanUpPreviousOperation(operation)) break;
					operation.setState(BigDataStackOperationState.InProgress);
					sequenceIO.updateSequence(sequence);
					operationSucceeded = operation.execute(database, operationsClient, statusClient, mailboxClient, prometheusDataClient, this, eventUtil);
					break;
				case Completed:
					switch (mode) {
					case Run:
						if (!tryCleanUpPreviousOperation(operation)) break;
						operation.setState(BigDataStackOperationState.InProgress);
						sequenceIO.updateSequence(sequence);
						operationSucceeded = operation.execute(database, operationsClient, statusClient, mailboxClient, prometheusDataClient, this, eventUtil);
						break;
					case Continue:
						// Skip to next operation
						operationSucceeded = true;
						break;
					}
					break;
				case Failed:
					if (!tryCleanUpPreviousOperation(operation)) break;
					operationSucceeded = operation.execute(database, operationsClient, statusClient, mailboxClient, prometheusDataClient, this, eventUtil);
					break;
				}
				
				if (operationSucceeded) operation.setState(BigDataStackOperationState.Completed);
				else operation.setState(BigDataStackOperationState.Failed);
				
				sequenceIO.updateSequence(sequence);
				
				if (kill || failed) {
					if (runnerObject!=null) {
						Set<String> runnerStatus = new HashSet<String>();
						runnerStatus.add("Failed");
						try {
							objectIO.updateObject(runnerObject);
						} catch (Exception e) {
							e.printStackTrace();
						}
						operationsClient.deleteOperation(runnerObject); // delete self;
					}
				}
				
				if (!operationSucceeded) {
					failed = true;
					eventUtil.registerEvent(
							sequence.getAppID(),
							sequence.getOwner(),
							sequence.getNamespace(),
							BigDataStackEventType.Stage,
							BigDataStackEventSeverity.Error,
							"Operation Sequence Failed for: '"+sequence.getSequenceID()+"' at operation targeting '"+operation.getObjectID()+"' of type "+operation.getClass().getName(),
							"Operation targeting '"+operation.getObjectID()+"' failed within sequence '"+sequence.getSequenceID()+"' with instance index '"+sequence.getIndex()+"'",
							sequence.getSequenceID(),
							sequence.getIndex()
							);
					if (runnerObject!=null) {
						Set<String> runnerStatus = new HashSet<String>();
						runnerStatus.add("Failed");
						try {
							objectIO.updateObject(runnerObject);
						} catch (Exception e) {
							e.printStackTrace();
						}
						operationsClient.deleteOperation(runnerObject); // delete self;
					}
					return;
				}
				
				/*eventUtil.registerEvent(
						sequence.getAppID(),
						sequence.getOwner(),
						sequence.getNamespace(),
						BigDataStackEventType.Stage,
						BigDataStackEventSeverity.Info,
						"Operation targeting '"+operation.getObjectID()+"' of type "+operation.getClass().getSimpleName()+" Complete within sequence '"+sequence.getSequenceID()+"'",
						"Operation targeting '"+operation.getObjectID()+"' of type "+operation.getClass().getSimpleName()+" completed within sequence '"+sequence.getSequenceID()+"' with instance index '"+sequence.getIndex()+"'",
						sequence.getSequenceID(),
						sequence.getIndex()
						);*/
				
			} catch (Exception e) {
				e.printStackTrace();
				failed = true;
				
				try {
					eventUtil.registerEvent(
							sequence.getAppID(),
							sequence.getOwner(),
							sequence.getNamespace(),
							BigDataStackEventType.GlobalDecisionTracker,
							BigDataStackEventSeverity.Error,
							"Operation Sequence '"+sequence.getSequenceID()+"' Failed",
							"Operation Sequence '"+sequence.getSequenceID()+"' failed during processing due to an internal exception '"+e.getMessage()+"'",
							sequence.getSequenceID(),
							sequence.getIndex()
							);
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
				
				if (runnerObject!=null) {
					Set<String> runnerStatus = new HashSet<String>();
					runnerStatus.add("Failed");
					try {
						objectIO.updateObject(runnerObject);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					operationsClient.deleteOperation(runnerObject); // delete self;
				}
				return;
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}

		
		if (failed) {
			try {
				eventUtil.registerEvent(
						sequence.getAppID(),
						sequence.getOwner(),
						sequence.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Error,
						"Operation Sequence '"+sequence.getSequenceID()+"' Failed",
						"Operation Sequence '"+sequence.getSequenceID()+"' failed due to one of the contained operations failing",
						sequence.getSequenceID(),
						sequence.getIndex()
						);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			if (runnerObject!=null) {
				Set<String> runnerStatus = new HashSet<String>();
				runnerStatus.add("Failed");
				try {
					objectIO.updateObject(runnerObject);
				} catch (Exception e) {
					e.printStackTrace();
				}
				operationsClient.deleteOperation(runnerObject); // delete self;
			}
		} else {
			try {
				eventUtil.registerEvent(
						sequence.getAppID(),
						sequence.getOwner(),
						sequence.getNamespace(),
						BigDataStackEventType.GlobalDecisionTracker,
						BigDataStackEventSeverity.Alert,
						"Operation Sequence '"+sequence.getSequenceID()+"' Completed Successfully",
						"Operation Sequence '"+sequence.getSequenceID()+"' completed successfuly",
						sequence.getSequenceID(),
						sequence.getIndex()
						);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			if (runnerObject!=null) {
				Set<String> runnerStatus = new HashSet<String>();
				runnerStatus.add("Completed");
				try {
					objectIO.updateObject(runnerObject);
				} catch (Exception e) {
					e.printStackTrace();
				}
				operationsClient.deleteOperation(runnerObject); // delete self;
			}
		}

	}

	/**
	 * Try to add an operation sequence instance to the database
	 * @param sequenceIO
	 * @return
	 * @throws SQLException
	 * @throws JsonProcessingException
	 */
	protected boolean tryAddSequence(BigDataStackOperationSequenceIO sequenceIO) throws SQLException, JsonProcessingException {

		int failedAttempts = 0;
		boolean sequenceAdded = false;

		while (!sequenceAdded) {
			List<BigDataStackOperationSequence> existingSequenceInstances = sequenceIO.getOperationSequences(sequence.getAppID(), sequence.getSequenceID());
			int highestIndex = 0;
			for (BigDataStackOperationSequence sequenceInstance : existingSequenceInstances) {
				if (sequenceInstance.getIndex()>highestIndex) highestIndex = sequenceInstance.getIndex();
			}

			int newIndex = highestIndex+1;
			sequence = sequence.clone();
			sequence.setIndex(newIndex);
			
			if (newParameters!=null) {
				for (String paramKey : newParameters.keySet()) {
					sequence.getParameters().put(paramKey, newParameters.get(paramKey));
				}
			}

			sequenceAdded = sequenceIO.addSequence(sequence);
			if (!sequenceAdded) {
				failedAttempts++;
				if (failedAttempts>=5) {
					return false;
				} else continue;
			}
		}

		return true;

	}
	
	/**
	 * Tries to delete the underlying object which is the target of this operation
	 * @param operation
	 * @return
	 */
	protected boolean tryCleanUpPreviousOperation(BigDataStackOperation operation) {
		Delete deleteOperation = new Delete(operation.getAppID(), operation.getOwner(), operation.getNamespace(), operation.getObjectID());
		boolean deleteOK = deleteOperation.execute(database, operationsClient, statusClient, mailboxClient, prometheusDataClient, this, eventUtil);
		if (!deleteOK) {
			operation.setState(BigDataStackOperationState.Failed);
			return false;
		}
		operation.setState(BigDataStackOperationState.NotStarted);
		return true;
	}
	
	
	
	public BigDataStackOperationSequence getSequence() {
		return sequence;
	}



	/**
	 * Call this to kill the thread
	 */
	public void kill() {
		kill = true;
	}

	/**
	 * If the thread has exited, you can use this to check whether it died
	 * due to an internal exception
	 * @return
	 */
	public boolean hasFailed() {
		return failed;
	} 

}
