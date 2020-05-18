package eu.bigdatastack.gdt.application;

import java.io.File;
import java.util.List;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import eu.bigdatastack.gdt.structures.data.BigDataStackApplication;
import eu.bigdatastack.gdt.structures.data.BigDataStackEvent;
import eu.bigdatastack.gdt.structures.data.BigDataStackNamespaceState;
import eu.bigdatastack.gdt.structures.data.BigDataStackOperationSequence;
import eu.bigdatastack.gdt.util.GDTFileUtil;

public class GDTCLI {

	GDTManager manager;
	
	public GDTCLI(GDTManager manager) {
		this.manager = manager;
	}
	
	
	public void processCommand(String[] args) throws Exception {
		
		switch (args[0]) {
		case "register":
			if (args.length==1) {
				System.out.println("register [namespace|playbook|object|metric] </path/to/file.yaml>");
			} else {
				switch (args[1]) {
				case "namespace":
					manager.registerNamespace(new File(args[2]));
					break;
				case "playbook":
					manager.loadPlaybook(GDTFileUtil.file2String(new File(args[2]), "UTF-8"));
					break;
				case "object":
					manager.registerObject(new File(args[2]));
					break;
				case "metric":
					manager.registerMetric(new File(args[2]));
					break;
				default:
					System.out.println("register [namespace|playbook|object|metric] </path/to/file.yaml>");
				}
			}
			
			break;
		case "monitor":
			if (args.length==1) {
				System.out.println("monitor [start|stop] <namespace>");
			} else {
				BigDataStackNamespaceState namespace;
				switch (args[1]) {
				case "start":
					namespace = manager.namespaceStateClient.getNamespace(args[2]);
					if (namespace==null) {
						System.out.println("Namespace "+args[2]+" not found");
						return;
					}
					manager.startMonitoringNamespace(namespace, "GDT");
					break;
				case "stop":
					namespace = manager.namespaceStateClient.getNamespace(args[2]);
					if (namespace==null) {
						System.out.println("Namespace "+args[2]+" not found");
						return;
					}
					manager.stopMonitoringNamespace(namespace, "GDT");
					break;
				default:
					System.out.println("monitor [start|stop] <namespace>");
				}
			}
			
			break;
		case "sequence":
			if (args.length==1) {
				System.out.println("sequence [start|startsync] <appID> <sequenceID>");
			} else {
				BigDataStackOperationSequence seq;
				switch (args[1]) {
				case "start":
					seq = manager.sequenceTemplateClient.getOperationSequence(args[2], args[3], 0);
					if (seq==null) {
						System.out.println("Sequence in app "+args[2]+" with id "+args[3]+" not found");
						return;
					}
					manager.executeSequenceFromTemplate(seq);
					break;
				case "startsync":
					seq = manager.sequenceTemplateClient.getOperationSequence(args[2], args[3], 0);
					if (seq==null) {
						System.out.println("Sequence in app "+args[2]+" with id "+args[3]+" not found");
						return;
					}
					manager.executeSequenceFromTemplateSync(seq);
					break;
				default:
					System.out.println("sequence [start|startsync] <appID> <sequenceID>");
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
				case "playbook":
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
				default:
					System.out.println("describe namespace <namespace>");
					System.out.println("describe app <owner> <namespace> <appID>");
					System.out.println("describe objecttemplate <owner> <objectID>");
					System.out.println("describe objectinstance <owner> <objectID> <instance>");
					System.out.println("describe metric <owner> <metric>");
					System.out.println("describe sequencetemplate <appID> <sequenceID>");
					System.out.println("describe sequenceinstance <appID> <sequenceID> <instance>");
					System.out.println("describe pod <podID>");
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
		}
		
		manager.shutdown();
		
	}
	
}
