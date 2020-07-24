package eu.bigdatastack.gdt.structures.reports;

public class PerHourTimeSeries {

	long startTime;
	double[] values;
	public PerHourTimeSeries(long startTime, double[] values) {
		super();
		this.startTime = startTime;
		this.values = values;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public double[] getValues() {
		return values;
	}
	public void setValues(double[] values) {
		this.values = values;
	}
	
	
	
}
