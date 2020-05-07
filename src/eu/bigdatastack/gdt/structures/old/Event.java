package eu.bigdatastack.gdt.structures.old;

import java.util.ArrayList;
import java.util.List;

public class Event {

	
	String header;
	String description;
	String time;
	String type;
	String level;
	
	List<EventAction> actions = new ArrayList<EventAction>();

	public Event() {}

	public Event(String header, String description, String time, String type, String level, List<EventAction> actions) {
		super();
		this.header = header;
		this.description = description;
		this.time = time;
		this.type = type;
		this.level = level;
		this.actions = actions;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public List<EventAction> getActions() {
		return actions;
	}

	public void setActions(List<EventAction> actions) {
		this.actions = actions;
	};
	
	


	
	
}
