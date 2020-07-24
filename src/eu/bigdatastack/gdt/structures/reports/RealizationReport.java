package eu.bigdatastack.gdt.structures.reports;

public class RealizationReport {
	RealizationStatus statusIndicators; 
	EventTimeSeries eventTimeSeries;
	PerHourTimeSeries costPerHour;
	ExecutingStatus exeStatus;
	RouteList routeList;
	public RealizationReport(RealizationStatus statusIndicators, EventTimeSeries eventTimeSeries,
			PerHourTimeSeries costPerHour, ExecutingStatus exeStatus, RouteList routeList) {
		super();
		this.statusIndicators = statusIndicators;
		this.eventTimeSeries = eventTimeSeries;
		this.costPerHour = costPerHour;
		this.exeStatus = exeStatus;
		this.routeList = routeList;
	}
	public RealizationStatus getStatusIndicators() {
		return statusIndicators;
	}
	public void setStatusIndicators(RealizationStatus statusIndicators) {
		this.statusIndicators = statusIndicators;
	}
	public EventTimeSeries getEventTimeSeries() {
		return eventTimeSeries;
	}
	public void setEventTimeSeries(EventTimeSeries eventTimeSeries) {
		this.eventTimeSeries = eventTimeSeries;
	}
	public PerHourTimeSeries getCostPerHour() {
		return costPerHour;
	}
	public void setCostPerHour(PerHourTimeSeries costPerHour) {
		this.costPerHour = costPerHour;
	}
	public ExecutingStatus getExeStatus() {
		return exeStatus;
	}
	public void setExeStatus(ExecutingStatus exeStatus) {
		this.exeStatus = exeStatus;
	}
	public RouteList getRouteList() {
		return routeList;
	}
	public void setRouteList(RouteList routeList) {
		this.routeList = routeList;
	}
	
	
	
}


