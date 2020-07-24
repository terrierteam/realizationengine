import java.io.File;

import eu.bigdatastack.gdt.application.GDTManager;
import eu.bigdatastack.gdt.structures.config.GDTConfig;
import eu.bigdatastack.gdt.structures.reports.PerHourTimeSeries;

public class StatusreportTest {

	public static void main(String[] args) throws Exception {
		
		GDTConfig gdtconfig = new GDTConfig(new File("gdt.config.json"));
		GDTManager manager = new GDTManager(gdtconfig);
		
		//manager.generateStatusReport("richardm", "richardmproject");
		
		PerHourTimeSeries timeSeries = manager.prometheusDataClient.perHourAvg("richardm", "richardmproject", null, null, null, "costPerHour", "1w");
		
		for (double d : timeSeries.getValues()) {
			System.out.println(d);
		}
		
		manager.shutdown();
	}
	
}
