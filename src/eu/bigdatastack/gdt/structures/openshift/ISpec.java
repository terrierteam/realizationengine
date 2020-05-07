package eu.bigdatastack.gdt.structures.openshift;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ISpec {

	int parallelism;
	int completions;
	int backoffLimit;
	
	ISelector selector;
	ObjectNode template;
	
	
	public ISpec( ) {}


	public int getParallelism() {
		return parallelism;
	}


	public void setParallelism(int parallelism) {
		this.parallelism = parallelism;
	}


	public int getCompletions() {
		return completions;
	}


	public void setCompletions(int completions) {
		this.completions = completions;
	}


	public int getBackoffLimit() {
		return backoffLimit;
	}


	public void setBackoffLimit(int backoffLimit) {
		this.backoffLimit = backoffLimit;
	}


	public ISelector getSelector() {
		return selector;
	}


	public void setSelector(ISelector selector) {
		this.selector = selector;
	}


	public ObjectNode getTemplate() {
		return template;
	}


	public void setTemplate(ObjectNode template) {
		this.template = template;
	}


	
	
	
	
}
