import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetricValue;

public class MetricTest {

	public static void main(String[] args) {
		
		BigDataStackMetricValue metricValue = new BigDataStackMetricValue(
				"richardm", "richardmproject", "atoswl", "atoswl-0",
				"ndcg_at_k_valid");
		
		PrometheusDataClient client = new PrometheusDataClient();
		
		client.update(metricValue);
		
		System.err.println(metricValue.getValue());
		System.err.println(metricValue.getLastUpdated());
		for (String k : metricValue.getLabels().keySet()) {
			System.err.println(k+" "+metricValue.getLabels().get(k));
		}

	}

}
