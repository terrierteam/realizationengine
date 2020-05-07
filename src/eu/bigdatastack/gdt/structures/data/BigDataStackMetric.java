package eu.bigdatastack.gdt.structures.data;

/**
 * This represents the generic definition of a metric that can be tracked within BigDataStack.
 * A metric is defined for a user. Note that a metric may have a range that defines the maximum
 * and minimum values. Constraints/threholds are not set my metrics, but instead are stored separately.
 * @author EbonBlade
 *
 */
public class BigDataStackMetric {

	private String owner;
	
	private String name;
	private BigDataStackMetricClassname metricClassname;
	private String summary;
	private double maximumValue;
	private double minimumValue;
	private boolean higherIsBetter;
	private String displayUnit;
	
	public BigDataStackMetric() {}
	
	public BigDataStackMetric(String owner, String name, BigDataStackMetricClassname metricClassname, String summary,
			double maximumValue, double minimumValue, boolean higherIsBetter, String displayUnit) {
		super();
		this.owner = owner;
		this.name = name;
		this.metricClassname = metricClassname;
		this.summary = summary;
		this.maximumValue = maximumValue;
		this.minimumValue = minimumValue;
		this.higherIsBetter = higherIsBetter;
		this.displayUnit = displayUnit;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public BigDataStackMetricClassname getMetricClassname() {
		return metricClassname;
	}
	public void setMetricClassname(BigDataStackMetricClassname metricClassname) {
		this.metricClassname = metricClassname;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public double getMaximumValue() {
		return maximumValue;
	}
	public void setMaximumValue(double maximumValue) {
		this.maximumValue = maximumValue;
	}
	public double getMinimumValue() {
		return minimumValue;
	}
	public void setMinimumValue(double minimumValue) {
		this.minimumValue = minimumValue;
	}
	public boolean isHigherIsBetter() {
		return higherIsBetter;
	}
	public void setHigherIsBetter(boolean higherIsBetter) {
		this.higherIsBetter = higherIsBetter;
	}
	public String getDisplayUnit() {
		return displayUnit;
	}
	public void setDisplayUnit(String displayUnit) {
		this.displayUnit = displayUnit;
	}
	
	
      	
        	
	
}
