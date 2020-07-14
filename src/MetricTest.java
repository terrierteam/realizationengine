import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.structures.data.BigDataStackMetricValue;

public class MetricTest {

	public static void main(String[] args) throws JsonProcessingException {
		
		
		PrometheusDataClient client = new PrometheusDataClient("ida.dcs.gla.ac.uk");
		
		BigDataStackMetricValue value = client.basicQuery("richardm", "richardmproject", "gdtdefaultapp", "gdtapi", "costPerHour");
		
		ObjectMapper mapper = new ObjectMapper();
		
		for (int i =0; i<value.getLabels().size(); i++) {
			System.err.println(mapper.writeValueAsString(value.getLabels().get(i))+" "+value.getLastUpdated().get(i)+" "+value.getValue().get(i));
		}


	}

}
