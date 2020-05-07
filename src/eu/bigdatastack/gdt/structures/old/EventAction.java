package eu.bigdatastack.gdt.structures.old;

public class EventAction {

	String descriptionText;
	String directLink;
	
	public EventAction() {}
	
	public EventAction(String descriptionText, String directLink) {
		super();
		this.descriptionText = descriptionText;
		this.directLink = directLink;
	}

	public String getDescriptionText() {
		return descriptionText;
	}

	public void setDescriptionText(String descriptionText) {
		this.descriptionText = descriptionText;
	}

	public String getDirectLink() {
		return directLink;
	}

	public void setDirectLink(String directLink) {
		this.directLink = directLink;
	}
	
	
	
}
