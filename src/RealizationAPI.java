

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class enables connection to the Realization API
 * @author EbonBlade
 *
 */
public class RealizationAPI {

	static final String apiVersion = "v1";
	
	static ObjectMapper mapper = new ObjectMapper();
	
	String host;
	String username;
	String password;

	public RealizationAPI(String host, String username, String password) {
		this.host = host;
		this.username = username;
		this.password = password;
	}
	
	//-----------------------------------------------------------------
	// API Methods
	//-----------------------------------------------------------------
	
	/**
	 * @return
	 */
	public boolean isAlive() {
		String response = get("");
		if (response==null) return false;
		else return true;
	}
	
	/**
	 * "/list/{owner}"
	 * @param owner
	 * @return
	 */
	public List<JsonNode> listOwner(String owner) {
		String response = get("list/"+owner);
		if (response==null) return null;
		return jsonToNodeList(response);
	}
	
	/**
	 * "/list/{owner}/apps"
	 * @param owner
	 * @return
	 */
	public List<JsonNode> listOwnerApps(String owner) {
		String response = get("list/"+owner+"/apps");
		if (response==null) return null;
		return jsonToNodeList(response);
	}
	
	/**
	 * "/list/{owner}/{appID}/objectTemplates"
	 * @param owner
	 * @param appID
	 * @return
	 */
	public List<JsonNode> listOwnerAppObjectTemplates(String owner, String appID) {
		String response = get("list/"+owner+"/"+appID+"/objectTemplates");
		if (response==null) return null;
		return jsonToNodeList(response);
	}
	
	/**
	 * "/list/{owner}/objectTemplates"
	 * @param owner
	 * @return
	 */
	public List<JsonNode> listOwnerObjectTemplates(String owner) {
		String response = get("list/"+owner+"/objectTemplates");
		if (response==null) return null;
		return jsonToNodeList(response);
	}
	
	
	/**
	 * "/list/{owner}/{appID}/objects"
	 * @param owner
	 * @param appID
	 * @return
	 */
	public List<JsonNode> listOwnerAppObjectInstances(String owner, String appID) {
		String response = get("list/"+owner+"/"+appID+"/objects");
		if (response==null) return null;
		return jsonToNodeList(response);
	}
	
	/**
	 * "/list/{owner}/objects"
	 * @param owner
	 * @return
	 */
	public List<JsonNode> listOwnerObjectInstances(String owner) {
		String response = get("list/"+owner+"/objects");
		if (response==null) return null;
		return jsonToNodeList(response);
	}
	
	/**
	 * "/list/{owner}/{appID}/objects/{objectID}"
	 * @param owner
	 * @param appID
	 * @param objectID
	 * @return
	 */
	public List<JsonNode> listOwnerAppObjectInstances(String owner, String appID, String objectID) {
		String response = get("list/"+owner+"/"+appID+"/objects/"+objectID);
		if (response==null) return null;
		return jsonToNodeList(response);
	}
	
	/**
	 * "/get/{owner}/{appID}/objects/{objectID}/instance/{instance}"
	 * @param owner
	 * @param appID
	 * @param objectID
	 * @param instance
	 * @return
	 */
	public JsonNode getOwnerAppObjectInstance(String owner, String appID, String objectID, int instance) {
		String response = get("list/"+owner+"/"+appID+"/objects/"+objectID+"/"+instance);
		if (response==null) return null;
		try {
			return mapper.readTree(response);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	

	
	/**
	 * Performs the underlying HTTP GET request
	 * @param method
	 * @return
	 */
	public String get(String method) {

		try {
			URL url = new URL(host+"/api/"+apiVersion+"/"+method);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Content-Type", "application/json");
			con.setConnectTimeout(1000);
			con.setReadTimeout(5000);
			con.setInstanceFollowRedirects(true);

			int status = con.getResponseCode();
			if (status>=300) return null;
			
			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer content = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
				content.append('\n');
			}
			in.close();
			con.disconnect();
			return content.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}
	
	/**
	 * Performs the underlying HTTP POST request
	 * @param method
	 * @param json
	 * @return
	 */
	public String post(String method, String json) {

		try {
			byte[] postData       = json.getBytes( StandardCharsets.UTF_8 );
			int    postDataLength = postData.length;
			
			URL url = new URL(host+"/api/"+apiVersion+"/"+method);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");
			con.setConnectTimeout(1000);
			con.setReadTimeout(5000);
			con.setInstanceFollowRedirects(true);
			con.setRequestProperty( "charset", "utf-8");
			con.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
			con.setUseCaches( false );

			try( DataOutputStream wr = new DataOutputStream( con.getOutputStream())) {
				   wr.writeUTF(json);
				}
			
			int status = con.getResponseCode();
			if (status>=300) return null;
			
			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer content = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
				content.append('\n');
			}
			in.close();
			con.disconnect();
			return content.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}
	
	/**
	 * Utility method to generate a list of JsonNodes from a String response
	 * @param json
	 * @return
	 */
	public static List<JsonNode> jsonToNodeList(String json) {
		try {
			JsonNode root = mapper.readTree(json);
			List<JsonNode> entries = new ArrayList<JsonNode>();
			if (root.isArray()) {
				Iterator<JsonNode> listIterator = root.elements();
				while ( listIterator.hasNext() ) entries.add(listIterator.next());
			} else {
				entries.add(root);
			}
			return entries;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void main(String[] args) {
		RealizationAPI api = new RealizationAPI("http://gdtapi-richardmproject.ida.dcs.gla.ac.uk", "richardm", "Syn6Blade");
		
		List<JsonNode> items = api.listOwner("richardm");
		for (JsonNode item : items) {
			System.err.println(item);
		}
		
		
	}

}
