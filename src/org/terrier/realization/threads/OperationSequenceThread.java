package org.terrier.realization.threads;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.terrier.realization.openshift.OpenshiftOperationClient;
import org.terrier.realization.openshift.OpenshiftStatusClient;
import org.terrier.realization.operations.BigDataStackOperation;
import org.terrier.realization.operations.BigDataStackOperationState;
import org.terrier.realization.operations.Delete;
import org.terrier.realization.prometheus.PrometheusDataClient;
import org.terrier.realization.rabbitmq.RabbitMQClient;
import org.terrier.realization.state.jdbc.BigDataStackEventIO;
import org.terrier.realization.state.jdbc.BigDataStackObjectIO;
import org.terrier.realization.state.jdbc.BigDataStackOperationSequenceIO;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.structures.data.BigDataStackEvent;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackOperationSequence;
import org.terrier.realization.structures.data.BigDataStackOperationSequenceMode;
import org.terrier.realization.util.EventUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

 /*
 * Realization Engine 
 * Webpage: https://github.com/terrierteam/realizationengine
 * Contact: richard.mccreadie@glasgow.ac.uk
 * University of Glasgow - School of Computing Science
 * http://www.gla.ac.uk/
 * 
 * The contents of this file are subject to the Apache License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 *
 * The Original Code is Copyright (C) to the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richard.mccreadie@glasgow.ac.uk> (original author)
 */

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
					if (sequence.getParameters().containsKey("writeOnExit")) writeOnExit(sequence.getParameters().get("writeOnExit"));
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
			if (sequence.getParameters().containsKey("writeOnExit")) writeOnExit(sequence.getParameters().get("writeOnExit"));
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
			if (sequence.getParameters().containsKey("writeOnExit")) writeOnExit(sequence.getParameters().get("writeOnExit"));
			return;
		}

		// Stage 2: Process Operations
		BigDataStackOperationSequenceMode mode = sequence.getMode();
		for (BigDataStackOperation operation : sequence.getOperations()) {
			try {
				BigDataStackOperationSequenceIO sequenceIO = new BigDataStackOperationSequenceIO(database, false);
				
				BigDataStackOperationState currentState = operation.getState();
				boolean operationSucceeded = false;
				if (kill || (failed && mode != BigDataStackOperationSequenceMode.RunIgnoreFailures)) return;
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
				
				if (kill || (failed && mode != BigDataStackOperationSequenceMode.RunIgnoreFailures)) {
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
						if (mode != BigDataStackOperationSequenceMode.RunIgnoreFailures) operationsClient.deleteOperation(runnerObject); // delete self;
					}
					if (sequence.getParameters().containsKey("writeOnExit")) writeOnExit(sequence.getParameters().get("writeOnExit"));
					if (mode != BigDataStackOperationSequenceMode.RunIgnoreFailures) return;
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
					if (mode != BigDataStackOperationSequenceMode.RunIgnoreFailures) operationsClient.deleteOperation(runnerObject); // delete self;
				}
				if (sequence.getParameters().containsKey("writeOnExit")) writeOnExit(sequence.getParameters().get("writeOnExit"));
				if (mode != BigDataStackOperationSequenceMode.RunIgnoreFailures) return;
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
		
		if (sequence.getParameters().containsKey("writeOnExit")) writeOnExit(sequence.getParameters().get("writeOnExit"));

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
	
	public void writeOnExit(String dir) {
		
		if (!dir.endsWith("/")) dir=dir+"/";
		
		for (String paramKey : sequence.getParameters().keySet()) {
			dir = dir.replaceAll("\\$"+paramKey+"\\$", sequence.getParameters().get(paramKey));
		}
		
		
		// Stage 1: Write out the sequence data
		String seqOutputFile = dir+sequence.getSequenceID()+"-"+sequence.getIndex()+".sequence.log";
		
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(seqOutputFile),"UTF-8"));
			
			bw.write(sequence.getName()+"\n");
			bw.write(sequence.getDescription()+"\n");
			bw.write("\n");
			bw.write("Status:\n");
			bw.write("  Pending: "+sequence.isPending()+"\n");
			bw.write("  InProgress: "+sequence.isInProgress()+"\n");
			bw.write("  Failed: "+sequence.hasFailed()+"\n");
			bw.write("  Completed: "+sequence.isComplete()+"\n");
			bw.write("\n");
			bw.write("Operations/Stages:\n");
			for (BigDataStackOperation operation : sequence.getOperations()) {
				bw.write("  "+operation.getClassName()+"\n");
				bw.write("  "+operation.describeOperation()+"\n");
				bw.write("  State: "+operation.getState().name()+"\n");
				bw.write("\n");
			}
			
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Stage 2: Write out the event data
		try {
			BigDataStackEventIO eventIO = new BigDataStackEventIO(database);
			
			List<BigDataStackEvent> events = eventIO.getEvents(sequence.getAppID(), sequence.getOwner(), BigDataStackEventType.GlobalDecisionTracker, sequence.getSequenceID());
			
			String evOutputFile = dir+sequence.getSequenceID()+"-"+sequence.getIndex()+".events.log";
			
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(evOutputFile),"UTF-8"));
			
			bw.write("Events:\n");
			for (BigDataStackEvent event : events) {
				
				if (event.getInstance()!=sequence.getIndex()) continue;
				
				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(event.getEventTime());
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");  
				String strDate = dateFormat.format(c.getTime()); 
				
				bw.write("  "+strDate+" ["+event.getNamepace()+"/"+event.getAppID()+"/"+event.getObjectID()+"] ["+event.getSeverity()+"]: "+event.getTitle()+"\n");
				bw.write("  "+event.getDescription()+"\n");
				bw.write("\n");
			}
			
			bw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
