
public class RegexTest {

	public static void main(String[] args) {
		
		String val = "bla: $appID$\nbla2: $appID$";
		
		System.out.println(val.replaceAll("\\$appID\\$", "myapp"));
		
	}
	
}
