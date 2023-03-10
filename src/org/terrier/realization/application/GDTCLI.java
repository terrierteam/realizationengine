package org.terrier.realization.application;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.terrier.realization.operations.Apply;
import org.terrier.realization.operations.BigDataStackOperation;
import org.terrier.realization.structures.data.BigDataStackApplication;
import org.terrier.realization.structures.data.BigDataStackEvent;
import org.terrier.realization.structures.data.BigDataStackMetric;
import org.terrier.realization.structures.data.BigDataStackMetricValue;
import org.terrier.realization.structures.data.BigDataStackNamespaceState;
import org.terrier.realization.structures.data.BigDataStackObjectDefinition;
import org.terrier.realization.structures.data.BigDataStackObjectType;
import org.terrier.realization.structures.data.BigDataStackOperationSequence;
import org.terrier.realization.structures.data.BigDataStackPodStatus;
import org.terrier.realization.util.GDTFileUtil;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

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

public class GDTCLI {

	GDTManager manager;
	
	public GDTCLI(GDTManager manager) {
		this.manager = manager;
	}
	
	
	//@SuppressWarnings("unchecked")
	public void processCommand(String[] args) throws Exception {
		
		switch (args[0]) {
		case "reset":
			if (args.length==1) {
				System.out.println("reset all");
			} else {
				switch (args[1]) {
				case "all":
					manager.appClient.clearTable();
					manager.eventClient.clearTable();
					manager.metricClient.clearTable();
					manager.objectInstanceClient.clearTable();
					manager.objectTemplateClient.clearTable();
					manager.sequenceInstanceClient.clearTable();
					manager.getSequenceTemplateClient().clearTable();
					manager.podStatusClient.clearTable();
					manager.namespaceStateClient.clearTable();
					manager.credentialsClient.clearTable();
					manager.metricValueClient.clearTable();
					manager.sloClient.clearTable();
					break;
				default:
					System.out.println("reset all");
				}
			}
			break;
		case "register":
			if (args.length==1) {
				System.out.println("register [namespace|object|metric|sequence] </path/to/file.yaml>");
				System.out.println("register playbook </path/to/file.yaml>");
				System.out.println("register playbook <owner> <namespace> </path/to/file.yaml> ");
				System.out.println("register playbook <owner> <namespace> </path/to/file.yaml> <key1:value1,key2:value2>");
			} else {
				switch (args[1]) {
				case "namespace":
					manager.registerNamespace(new File(args[2]));
					break;
				case "playbook":
					if (args.length==3) manager.loadPlaybook(GDTFileUtil.file2String(new File(args[2]), "UTF-8"), null, null);
					else if (args.length==5) manager.loadPlaybook(GDTFileUtil.file2String(new File(args[4]), "UTF-8"), args[2], args[3]);
					else if (args.length==6) manager.loadPlaybook(GDTFileUtil.file2String(new File(args[4]), "UTF-8"), args[2], args[3], args[5]);
					break;
				case "playbookPrint":
					List<String> returns = null;
					if (args.length==3) returns = manager.loadPlaybook(GDTFileUtil.file2String(new File(args[2]), "UTF-8"), null, null);
					else if (args.length==5) returns = manager.loadPlaybook(GDTFileUtil.file2String(new File(args[4]), "UTF-8"), args[2], args[3]);
					else if (args.length==6) returns = manager.loadPlaybook(GDTFileUtil.file2String(new File(args[4]), "UTF-8"), args[2], args[3], args[5]);
					
					for (String r : returns) {
						System.err.println(r);
					}
					break;
				case "object":
					manager.registerObject(new File(args[2]));
					break;
				case "metric":
					manager.registerMetric(new File(args[2]));
					break;
				case "sequence":
					manager.registerOperationSequence(new File(args[2]));
					break;
				default:
					System.out.println("register [namespace|playbook|object|metric|sequence] </path/to/file.yaml>");
				}
			}
			
			break;
		case "monitor":
			if (args.length==1) {
				System.out.println("monitor [start|stop] <owner> <namespace>");
			} else {
				BigDataStackNamespaceState namespace;
				switch (args[1]) {
				case "start":
					namespace = manager.namespaceStateClient.getNamespace(args[3]);
					if (namespace==null) {
						System.out.println("Namespace "+args[3]+" not found");
						return;
					}
					manager.startMonitoringNamespace(namespace.getNamespace(), args[2]);
					break;
				case "stop":
					namespace = manager.namespaceStateClient.getNamespace(args[3]);
					if (namespace==null) {
						System.out.println("Namespace "+args[3]+" not found");
						return;
					}
					manager.stopMonitoringNamespace(namespace.getNamespace(), args[2]);
					break;
				default:
					System.out.println("monitor [start|stop] <namespace>");
				}
			}
			
			break;
		case "sequence":
			if (args.length==1) {
				System.out.println("sequence [start|startsync] <appID> <sequenceID>");
				System.out.println("sequence [start] <appID> <sequenceID> param1=val1,param2=val2");
				System.out.println("sequence [stop] <appID> <sequenceID> <instance>");
			} else {
				BigDataStackOperationSequence seq;
				switch (args[1]) {
				case "start":
					seq = manager.sequenceTemplateClient.getOperationSequence(args[2], args[3], 0, null);
					if (seq==null) {
						System.out.println("Sequence in app "+args[2]+" with id "+args[3]+" not found");
						return;
					}
					Map<String,String> params = new HashMap<String,String>();
					if (args.length==5) {
						String[] parts = args[4].split(",");
						for (String part : parts) {
							params.put(part.split("=")[0], part.split("=")[1]);
						}
					}
					manager.executeSequenceFromTemplate(seq, params);
					break;
				case "startsync":
					seq = manager.sequenceTemplateClient.getOperationSequence(args[2], args[3], 0, null);
					if (seq==null) {
						System.out.println("Sequence in app "+args[2]+" with id "+args[3]+" not found");
						return;
					}
					manager.executeSequenceFromTemplateSync(seq);
					break;
				case "stop":
					seq = manager.sequenceInstanceClient.getSequence(args[2], args[3], Integer.parseInt(args[4]));
					if (seq==null) {
						System.out.println("Sequence in app "+args[2]+" with id "+args[3]+" not found");
						return;
					}
					manager.stopOperationSequenceInstance(seq.getOwner(), seq.getAppID(), seq.getSequenceID(), seq.getIndex(), seq.getNamepace());
					break;
				default:
					System.out.println("sequence [start|startsync] <appID> <sequenceID>");
					System.out.println("sequence [start] <appID> <sequenceID> param1=val1,param2=val2");
					System.out.println("sequence [stop] <appID> <sequenceID> <instance>");
				}
			}
			
			
			break;
		case "describe":
			if (args.length==1) {
				System.out.println("describe namespace <namespace>");
				System.out.println("describe app <owner> <namespace> <appID>");
				System.out.println("describe objecttemplate <owner> <objectID>");
				System.out.println("describe objectinstance <owner> <objectID> <instance>");
				System.out.println("describe metric <owner> <metric>");
				System.out.println("describe sequencetemplate <appID> <sequenceID>");
				System.out.println("describe sequenceinstance <appID> <sequenceID> <instance>");
				System.out.println("describe pod <podID>");
			} else {
				switch (args[1]) {
				case "namespace":
					System.out.println(new YAMLMapper().writeValueAsString(manager.namespaceStateClient.getNamespace(args[2])));
					break;
				case "app":
					System.out.println(new YAMLMapper().writeValueAsString(manager.appClient.getApp(args[4], args[2], args[3])));
					break;
				case "objecttemplate":
					System.out.println(new YAMLMapper().writeValueAsString(manager.objectTemplateClient.getObject(args[3], args[2])));
					break;
				case "objectinstance":
					System.out.println(new YAMLMapper().writeValueAsString(manager.objectInstanceClient.getObject(args[3], args[2], Integer.parseInt(args[4]))));
					break;
				case "metric":
					System.out.println(new YAMLMapper().writeValueAsString(manager.metricClient.getMetric(args[2], args[3])));
				case "pod":
					System.out.println(new YAMLMapper().writeValueAsString(manager.podStatusClient.getPodStatus(args[2])));
				case "sequencetemplate":
					System.out.println(new YAMLMapper().writeValueAsString(manager.sequenceTemplateClient.getSequence(args[2], args[3])));
					break;
				case "sequenceinstance":
					System.out.println(new YAMLMapper().writeValueAsString(manager.sequenceInstanceClient.getSequence(args[2], args[3], Integer.parseInt(args[4]))));
					break;
				case "metrics":
					if (args.length==2) System.out.println(new YAMLMapper().writeValueAsString(manager.metricClient.listMetrics(null, null)));
					else System.out.println(new YAMLMapper().writeValueAsString(manager.metricClient.listMetrics(args[2], null)));
					break;
				case "triggers":
					System.out.println(new YAMLMapper().writeValueAsString(manager.sloClient.getSLOs(args[2], null, null)));
					break;
				default:
					System.out.println("describe namespace <namespace>");
					System.out.println("describe app <owner> <namespace> <appID>");
					System.out.println("describe objecttemplate <owner> <objectID>");
					System.out.println("describe objectinstance <owner> <objectID> <instance>");
					System.out.println("describe metric <owner> <metric>");
					System.out.println("describe sequencetemplate <appID> <sequenceID>");
					System.out.println("describe sequenceinstance <appID> <sequenceID> <instance>");
					System.out.println("describe pod <podID>");
					System.out.println("describe metrics");
					System.out.println("describe metrics <owner>");
					System.out.println("describe triggers <owner>");
				}
			}
			
			break;
		case "list":
			if (args.length==1) {
				System.out.println("list events <owner> <appID>");
				System.out.println("list apps <owner>");
			} else {
				switch (args[1]) {
				case "events":
					List<BigDataStackEvent> events = manager.eventClient.getEvents(args[3], args[2]);
					for (BigDataStackEvent event : events) {
						event.print();
					}
					break;
				case "apps":
					List<BigDataStackApplication> apps = manager.appClient.getApplications(args[2]);
					for (BigDataStackApplication app : apps) System.out.println(app.getAppID()+": "+app.getName());
					break;
				default:
					System.out.println("list events <owner> <appID>");
					System.out.println("list apps <owner>");
				}
			}
			
			break;
			
		case "summarize":
			if (args.length==1) {
				System.out.println("sequence <appID> <sequenceID>");
				System.out.println("appmetrics <owner> <namespace> <appID>");
			} else {
				switch (args[1]) {
				case "sequence":
					BigDataStackOperationSequence sequenceTemplate = manager.getSequenceTemplateClient().getSequence(args[2], args[3]);
					System.out.println("|------------------------------------------------------");
					System.out.println("| ID: "+sequenceTemplate.getSequenceID());
					System.out.println("| Title: "+sequenceTemplate.getName());
					System.out.println("| App: "+sequenceTemplate.getAppID());
					System.out.println("| Owned By: "+sequenceTemplate.getOwner());
					System.out.println("| Namespace: "+sequenceTemplate.getNamespace());
					System.out.println("|------------------------------------------------------");
					System.out.println();
					System.out.println("|------------------------------------------------------");
					System.out.println("| Operations: ");
					for (BigDataStackOperation operation : sequenceTemplate.getOperations()) {
						System.out.println("| > "+operation.getClassName()+": "+operation.describeOperation());
					}
					System.out.println("|------------------------------------------------------");
					System.out.println();
					System.out.println("|------------------------------------------------------");
					System.out.println("| Instances: ");
					List<BigDataStackOperationSequence> sequences = manager.getSequenceInstanceClient().getOperationSequences(args[2], args[3]);
					for (BigDataStackOperationSequence sequence : sequences) {
						if (sequence.getCurrentOperation()==null) System.out.println("| > "+sequence.getIndex()+": Not Started");
						else System.out.println("| > "+sequence.getIndex()+": "+sequence.getCurrentOperation().getClassName()+"="+sequence.getCurrentOperation().getState());
						for (BigDataStackOperation operation : sequence.getOperations()) {
							if (operation instanceof Apply) {
								Apply apply = (Apply)operation;
								String[] objectID = sequence.getParameters().get(apply.getInstanceRef()).split(":");
								BigDataStackObjectDefinition objectDef = manager.objectInstanceClient.getObject(objectID[0], sequenceTemplate.getOwner(), Integer.parseInt(objectID[1]));
								if (objectDef==null) continue;
								if (objectDef.getType()==BigDataStackObjectType.DeploymentConfig || objectDef.getType()==BigDataStackObjectType.Job) {
									System.out.println("|     - "+objectDef.getObjectID()+"("+objectDef.getInstance()+") of type "+objectDef.getType()+", states="+objectDef.getStatus());
									List<BigDataStackPodStatus> statuses = manager.podStatusClient.getPodStatuses(sequenceTemplate.getAppID(), sequenceTemplate.getOwner(), objectID[0], sequenceTemplate.getNamespace(), objectDef.getInstance());
									for (BigDataStackPodStatus status : statuses) {
										System.out.println("|      ~ POD:"+status.getPodID()+", state=["+status.getStatus()+"]");
									}
								}
								
							}
						}
					}
					System.out.println("|------------------------------------------------------");
					break;
				case "appmetrics":
					BigDataStackApplication app = manager.appClient.getApp(args[4], args[2], args[3]);
					System.out.println("|------------------------------------------------------");
					System.out.println("| ID: "+app.getAppID());
					System.out.println("| Title: "+app.getName());
					System.out.println("|------------------------------------------------------");
					System.out.println();
					List<BigDataStackMetricValue> metrics = manager.metricValueClient.getMetricValues(args[4], args[2], args[3], null, null);
					System.out.println("|------------------------------------------------------");
					System.out.println("| Metrics: ");
					for (BigDataStackMetricValue metric : metrics) {
						BigDataStackMetric metricDesc = manager.metricClient.getMetric(args[2], metric.getMetricname());
						System.out.println("| > "+metric.getMetricname());
						if (metricDesc!=null) {
							System.out.println("|   "+metricDesc.getMinimumValue()+"<="+metricDesc.getDisplayUnit()+"<="+metricDesc.getMaximumValue());
							if (metricDesc.isHigherIsBetter()) System.out.println("|   (Higher values are better)");
							else System.out.println("|   (Lower values are better)");
						}
						for (int i =0; i<metric.getLabels().size(); i++) {
							Map<String,String> labels = metric.getLabels().get(i);
							String value = metric.getValue().get(i);
							//Long timestamp = metric.getLastUpdated().get(i);
							BigDataStackObjectDefinition objectDef = manager.objectInstanceClient.getObject(labels.get("objectID"), app.getOwner(), Integer.parseInt(labels.get("instance")));
							if (labels.containsKey("appID")) if (!labels.get("appID").equalsIgnoreCase(app.getAppID())) continue;
							
							
							System.out.println("|   - "+labels.get("objectID")+"("+labels.get("instance")+") "+value +" (status="+objectDef.getStatus()+")");
							for (String label :labels.keySet()) {
								if (label.contains("host")) continue;
								if (label.contains("port")) continue;
								if (label.contains("username")) continue;
								if (label.contains("password")) continue;
								if (label.contains("database")) continue;
								if (label.contains("owner")) continue;
								if (label.contains("namespace")) continue;
								if (label.contains("__name__")) continue;
								if (label.contains("instance")) continue;
								if (label.contains("appID")) continue;
								if (label.contains("objectID")) continue;
								if (label.contains("job")) continue;
								System.out.println("|     ~ "+label+"="+labels.get(label));
							}
							
						}
						System.err.println("| ");
						System.out.println("|    ---");
						System.err.println("| ");
						
					}
					System.out.println("|------------------------------------------------------");
					break;
				default:
					System.out.println("sequence <appID> <sequenceID>");
					System.out.println("appmetrics <owner> <namespace> <appID>");
				}
			}
			
			break;
			
		case "delete":
			if (args.length==1) {
				System.out.println("delete app <owner> <namespace> <appID>");
			} else {
				if (args[1].equalsIgnoreCase("app")) {
					manager.deleteApp(args[2], args[3], args[4]);
				}
				
				if (args[1].equalsIgnoreCase("sequenceRunners")) {
					manager.cleanupSequenceRunners(args[2], args[3], args[4]);
				}
			}
			break;
		}
		
		manager.shutdown();
		
	}
	
}
