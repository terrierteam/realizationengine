package eu.bigdatastack.gdt.structures.reports;

import java.util.List;

public class EventTimeSeries {

	List<Integer> infoCountPerHour;
	List<Integer> warnCountPerHour;
	List<Integer> errCountPerHour;
	List<Integer> alertCountPerHour;
	
	long firstHourStart;
	long lastHourStart;
	
	public EventTimeSeries(List<Integer> infoCountPerHour, List<Integer> warnCountPerHour,
			List<Integer> errCountPerHour, List<Integer> alertCountPerHour, long firstHourStart, long lastHourStart) {
		super();
		this.infoCountPerHour = infoCountPerHour;
		this.warnCountPerHour = warnCountPerHour;
		this.errCountPerHour = errCountPerHour;
		this.alertCountPerHour = alertCountPerHour;
		this.firstHourStart = firstHourStart;
		this.lastHourStart = lastHourStart;
	}
	public List<Integer> getInfoCountPerHour() {
		return infoCountPerHour;
	}
	public void setInfoCountPerHour(List<Integer> infoCountPerHour) {
		this.infoCountPerHour = infoCountPerHour;
	}
	public List<Integer> getWarnCountPerHour() {
		return warnCountPerHour;
	}
	public void setWarnCountPerHour(List<Integer> warnCountPerHour) {
		this.warnCountPerHour = warnCountPerHour;
	}
	public List<Integer> getErrCountPerHour() {
		return errCountPerHour;
	}
	public void setErrCountPerHour(List<Integer> errCountPerHour) {
		this.errCountPerHour = errCountPerHour;
	}
	public List<Integer> getAlertCountPerHour() {
		return alertCountPerHour;
	}
	public void setAlertCountPerHour(List<Integer> alertCountPerHour) {
		this.alertCountPerHour = alertCountPerHour;
	}
	public long getFirstHourStart() {
		return firstHourStart;
	}
	public void setFirstHourStart(long firstHourStart) {
		this.firstHourStart = firstHourStart;
	}
	public long getLastHourStart() {
		return lastHourStart;
	}
	public void setLastHourStart(long lastHourStart) {
		this.lastHourStart = lastHourStart;
	}
	
	
	
}
