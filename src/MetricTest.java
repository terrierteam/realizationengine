import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetricValue;

public class MetricTest {

	public static void main(String[] args) throws JsonProcessingException {
		
		BigDataStackMetricValue metricValue = new BigDataStackMetricValue(
				"richardm", "richardmproject", "atoswl", "atoswl-0",
				"ndcg_at_k_valid");
		
		PrometheusDataClient client = new PrometheusDataClient();
		
		client.update(metricValue);
		
		ObjectMapper mapper = new ObjectMapper();
		
		for (int i =0; i<metricValue.getLabels().size(); i++) {
			System.err.println(mapper.writeValueAsString(metricValue.getLabels().get(i))+" "+metricValue.getLastUpdated().get(i)+" "+metricValue.getValue().get(i));
		}


	}

}
