import eu.bigdatastack.gdt.application.GDTAPI;

public class TestAPI {

	public static void main(String[] args) {
		try {
			String[] commandArgs = {"server", "api.json"};
			new GDTAPI(null).run(commandArgs); // Create a new online api and run it
		} catch (Exception e) {e.printStackTrace();}

	}

}
