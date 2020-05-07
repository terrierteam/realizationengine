import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDBConnection {

	public static void main(String[] args) {
		
		
		Connection c = null;

		try {

			Class.forName("com.leanxcale.client.Driver");

			c = DriverManager
					.getConnection("jdbc:leanxcale://lx-store-giorgioproject.ida.dcs.gla.ac.uk:80/BigDataStackDB;user=GFT");
					//.getConnection("jdbc:leanxcale://gdtdb-richardmproject.ida.dcs.gla.ac.uk:80/BigDataStackGDTDB;user=admin");

			DatabaseMetaData md = c.getMetaData();
			ResultSet rs = md.getTables(null, null, "%", null);
			while (rs.next()) {
			  System.out.println(rs.getString(3));
			}
			
			c.close();

		} catch (Exception e) {

			e.printStackTrace();

			System.err.println(e.getClass().getName()+": "+e.getMessage());

			System.exit(0);

		}
		
	}
	
}
