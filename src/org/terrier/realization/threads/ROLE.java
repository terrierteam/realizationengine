package org.terrier.realization.threads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.terrier.realization.application.GDTManager;
import org.terrier.realization.state.jdbc.JDBCDB;
import org.terrier.realization.structures.data.BigDataStackEventSeverity;
import org.terrier.realization.structures.data.BigDataStackEventType;
import org.terrier.realization.structures.data.BigDataStackMetric;
import org.terrier.realization.structures.data.BigDataStackMetricSource;
import org.terrier.realization.structures.data.BigDataStackMetricValue;
import org.terrier.realization.structures.data.BigDataStackOperationSequence;
import org.terrier.realization.structures.data.BigDataStackSLO;
import org.terrier.realization.util.EventUtil;

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
public class ROLE implements Runnable{

	GDTManager manager;
	JDBCDB database;
	String namespace;
	String owner;

	boolean kill = false;
	boolean failed = false;
	
	public ROLE(GDTManager manager, String owner, String namespace) {
		this.namespace = namespace;
		this.owner = owner;
		this.manager = manager;
	}
	
	@Override
	public void run() {

		Map<String,Long> triggerActionStartTimes = new HashMap<String,Long>();
		
		while (!kill) {
			
			// ROLE main logic starts here
			try {
				
				
				List<BigDataStackSLO> slos = manager.sloClient.getSLOs(owner, null, null);
				
				for (BigDataStackSLO slo : slos) {
					
					if (slo==null) continue;
					 
					// Get the associated metric
					BigDataStackMetric metric = manager.metricClient.getMetric(owner, slo.getMetricName());
					
					if (metric.getSource()==BigDataStackMetricSource.prometheus) {
						
						System.err.println(metric.getName());
						
						BigDataStackMetricValue metricValue = manager.prometheusDataClient.basicQuery(null, slo.getNamespace(), null, null, null, metric.getName(), null);
						
						// List of sampled values
						List<String> taskList = new ArrayList<String>();
						
						for (int i =0; i<metricValue.getLabels().size(); i++) {
							String taskID = metricValue.getLabels().get(i).get("task_id");
							if (!taskList.contains(taskID)) taskList.add(taskID);
						}
						
						for (String taskID : taskList) {
							double total = 0;
							int samples = 0;
							
							for (int i =0; i<metricValue.getLabels().size(); i++) {
								String thisTaskID = metricValue.getLabels().get(i).get("task_id");
							
								if (taskID.equalsIgnoreCase(thisTaskID)) {
									
									total=total+Double.parseDouble(metricValue.getValue().get(i));
									samples++;
									
								}
								
							}
							
							double average = total/samples;
							
							System.err.println("  "+taskID+" "+average);
							
							
							if (average>slo.getValue()) {
								// valid trigger condition
								
								// check if we are on cooldown
								if (triggerActionStartTimes.containsKey(metric.getName())) {
									if ((System.currentTimeMillis()-(slo.getCoolDownMins()*60*1000))<triggerActionStartTimes.get(metric.getName())) {
										// still on cooldown
										continue;
									}
								}
								
								// if we get here, trigger action
								BigDataStackOperationSequence matchingSequence = manager.sequenceTemplateClient.getOperationSequence(slo.getAppID(), slo.getAction(), 0, slo.getOwner());
								
								if(matchingSequence==null) {
									
									manager.eventUtil.registerEvent(
											slo.getAppID(),
											slo.getOwner(),
											slo.getNamespace(),
											BigDataStackEventType.DynamicOrchestrator,
											BigDataStackEventSeverity.Warning,
											"ROLE attempted to trigger action: '"+slo.getAction()+"' within app "+slo.getAppID()+", but the action was not found",
											"The rule-based orchestration logic engine detected a trigger condition '"+slo.getTriggerID()+"' within app "+slo.getAppID()+", but failed to find the operation sequence for the action.",
											slo.getTriggerID(),
											-1
											);
									
									continue;
								}
								
								manager.executeSequenceFromTemplate(matchingSequence);
								
								manager.eventUtil.registerEvent(
										slo.getAppID(),
										slo.getOwner(),
										slo.getNamespace(),
										BigDataStackEventType.DynamicOrchestrator,
										BigDataStackEventSeverity.Alert,
										slo.getTriggerMessage(),
										"The rule-based orchestration logic engine has autonomously triggered '"+slo.getAction()+"' within app "+slo.getAppID()+" in response to trigger "+slo.getTriggerID()+". The threshold was "+slo.getValue()+", while the observed value was "+average+".",
										slo.getTriggerID(),
										-1
										);
								
								triggerActionStartTimes.put(metric.getName(), System.currentTimeMillis());
								
							}
						}
					}
					
					
					
				}
				
				
				
				
				
				
				
				
				
				Thread.sleep(5000);
				
				
				
				
			} catch (Exception e) {
				e.printStackTrace();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			
			
		}
		
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
