package eu.bigdatastack.gdt.operations;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import eu.bigdatastack.gdt.lxdb.JDBCDB;
import eu.bigdatastack.gdt.openshift.OpenshiftOperationClient;
import eu.bigdatastack.gdt.openshift.OpenshiftStatusClient;
import eu.bigdatastack.gdt.prometheus.PrometheusDataClient;
import eu.bigdatastack.gdt.rabbitmq.RabbitMQClient;
import eu.bigdatastack.gdt.threads.OperationSequenceThread;
import eu.bigdatastack.gdt.util.EventUtil;

/**
 * This is a basic operation that sends HTTP requests with pre-defined properties to a specified
 * URL 
 * @author EbonBlade
 *
 */
public class GenerateHTTPPostLoad extends BigDataStackOperation {

	private String appID;
	private String owner;
	private String namespace;

	private int threads;
	private int delayBetweenRequestsMS;
	private int requestsPerThread;
	private String URL;
	private JsonNode requestBodyTemplate;
	private JsonNode variables;



	public GenerateHTTPPostLoad() {
		this.className = this.getClass().getName();
	};
	
	

	public String getAppID() {
		return appID;
	}



	public void setAppID(String appID) {
		this.appID = appID;
	}



	public String getOwner() {
		return owner;
	}



	public void setOwner(String owner) {
		this.owner = owner;
	}



	public String getNamespace() {
		return namespace;
	}



	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}



	public int getThreads() {
		return threads;
	}



	public void setThreads(int threads) {
		this.threads = threads;
	}



	public int getDelayBetweenRequestsMS() {
		return delayBetweenRequestsMS;
	}



	public void setDelayBetweenRequestsMS(int delayBetweenRequestsMS) {
		this.delayBetweenRequestsMS = delayBetweenRequestsMS;
	}



	public int getRequestsPerThread() {
		return requestsPerThread;
	}



	public void setRequestsPerThread(int requestsPerThread) {
		this.requestsPerThread = requestsPerThread;
	}



	public String getURL() {
		return URL;
	}



	public void setURL(String uRL) {
		URL = uRL;
	}



	public JsonNode getRequestBodyTemplate() {
		return requestBodyTemplate;
	}



	public void setRequestBodyTemplate(JsonNode requestBodyTemplate) {
		this.requestBodyTemplate = requestBodyTemplate;
	}



	public JsonNode getVariables() {
		return variables;
	}



	public void setVariables(JsonNode variables) {
		this.variables = variables;
	}



	public GenerateHTTPPostLoad(String appID, String owner, String namespace, int threads, int delayBetweenRequestsMS,
			int requestsPerThread, String uRL, JsonNode requestBodyTemplate, JsonNode variables) {
		super();
		this.appID = appID;
		this.owner = owner;
		this.namespace = namespace;
		this.threads = threads;
		this.delayBetweenRequestsMS = delayBetweenRequestsMS;
		this.requestsPerThread = requestsPerThread;
		URL = uRL;
		this.requestBodyTemplate = requestBodyTemplate;
		this.variables = variables;
	}



	@Override
	public String describeOperation() {
		return "Sends HTTP post requests to a URL";
	}

	@Override
	public boolean execute(JDBCDB database, OpenshiftOperationClient openshiftOperationClient,
			OpenshiftStatusClient openshiftStatusClient, RabbitMQClient mailboxClient,
			PrometheusDataClient prometheusDataClient, OperationSequenceThread parentSequenceRunner,
			EventUtil eventUtil) {

		List<BasicHTTPPostThread> launchedThreads = new ArrayList<BasicHTTPPostThread>();
		
		for (int t =0; t<threads; t++) {
			BasicHTTPPostThread thread = new BasicHTTPPostThread(delayBetweenRequestsMS, requestsPerThread, URL, requestBodyTemplate, variables);
			new Thread(thread).start();
			launchedThreads.add(thread);			
		}

		boolean allEnded = false;
		while (!allEnded) {
			boolean foundActive = false;
			for (BasicHTTPPostThread t : launchedThreads) {
				System.err.println(t.done+" "+t.kill);
				
				if (!t.done && !t.kill) {
					foundActive = true;
					break;
				}
			}
			
			if (foundActive) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			else allEnded = true;
		}

		return true;
	}

	@Override
	public String getObjectID() {
		return null;
	}

	@Override
	public void initalizeFromJson(JsonNode configJson) {
		threads = Integer.parseInt(configJson.get("threads").asText());
		requestsPerThread = Integer.parseInt(configJson.get("requestsPerThread").asText());
		delayBetweenRequestsMS = Integer.parseInt(configJson.get("delayBetweenRequestsMS").asText());
		URL = configJson.get("URL").asText();
		requestBodyTemplate = configJson.get("requestBodyTemplate");
		variables = configJson.get("variables");
	}

	

	public class BasicHTTPPostThread implements Runnable {

		int delayBetweenRequestsMS;
		int requestsPerThread;
		String URL;
		JsonNode requestBodyTemplate;
		JsonNode variables;
		
		boolean kill = false;
		boolean done = false;
	
		public BasicHTTPPostThread(int delayBetweenRequestsMS, int requestsPerThread, String URL,
				JsonNode requestBodyTemplate, JsonNode variables) {
			super();
			this.delayBetweenRequestsMS = delayBetweenRequestsMS;
			this.requestsPerThread = requestsPerThread;
			this.URL = URL;
			this.requestBodyTemplate = requestBodyTemplate;
			this.variables = variables;
		}

		@Override
		public void run() {
			
			System.err.println("New Thread");
			
			/*
			 * requestBodyTemplate:
			 *   customerId: "1"
			 *   productId: "1"
			 *   feedbackType: "PRODUCT_VISUALIZED"
			 * variables:
			 *   customerId: "R:1000"
			 *   productId: "R:1000"
			 *   feedbackType: "D:PRODUCT_VISUALIZED|PRODUCT_ADDED_TO_BASKET|PRODUCT_REMOVED_FROM_BASKET|PRODUCT_RECOMMENDATION_REMOVED|PRODUCT_RECOMMENDATION_SHOWN"
			 */
			
			Random r = new Random();
			
			int requestsMade = 0;
			
			while (!kill && requestsMade<requestsPerThread) {
				
				ObjectNode requestBody = requestBodyTemplate.deepCopy();
				
				Iterator<Entry<String,JsonNode>> fields= variables.fields();
				while (fields.hasNext()) {
					Entry<String,JsonNode> field = fields.next();
					String variableName= field.getKey();
					String expresssion = field.getValue().asText();
					
					if (expresssion.startsWith("R:")) {
						// generate a number in a range
						int maxValue = Integer.parseInt(expresssion.split(":")[1]);
						
						int value = r.nextInt(maxValue);
						((ObjectNode)requestBody).put(variableName, String.valueOf(value));
					} else if (expresssion.startsWith("D:")) {
						// Discrete values, select one
						String[] values = expresssion.split(":")[1].split(",");
						int index = r.nextInt(values.length);
						((ObjectNode)requestBody).put(variableName, values[index]);
					}
					
				}
				
				try {
					System.err.println(requestBody.toString());
					
					URL url = new URL(URL);
					URLConnection con = url.openConnection();
					HttpURLConnection http = (HttpURLConnection)con;
					http.setRequestMethod("POST");
					http.setRequestProperty("Content-Type", "application/json; utf-8");
					http.setDoOutput(true);
					try(OutputStream os = http.getOutputStream()) {
					    byte[] input = requestBody.toString().getBytes("utf-8");
					    os.write(input, 0, input.length);			
					}
					
					try(BufferedReader br = new BufferedReader(
							  new InputStreamReader(http.getInputStream(), "utf-8"))) {
							    StringBuilder response = new StringBuilder();
							    String responseLine = null;
							    while ((responseLine = br.readLine()) != null) {
							        response.append(responseLine.trim());
							    }
							    System.out.println(response.toString());
							}
					
					
					http.disconnect();
					
					
					
				} catch (Exception e) {
					e.printStackTrace();
					kill = true;
				}
				
				try {
					Thread.sleep(delayBetweenRequestsMS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				requestsMade++;
			}
			
			done=true;	
		}
		
		
	}
	
	public static void main(String[] args) {
		
		String config = "className: GenerateHTTPPostLoad\n" + 
				"threads: \"1\"\n" + 
				"delayBetweenRequestsMS: \"0\"\n" + 
				"URL: \"http://feedbackcollector-realization.apps.moc.bigdatastack.com/feedbacks\"\n" + 
				"requestsPerThread: \"1000\"\n" + 
				"requestBodyTemplate:\n" + 
				"  customerId: \"1\"\n" + 
				"  productId: \"1\"\n" + 
				"  feedbackType: \"PRODUCT_VISUALIZED\"\n" + 
				"variables:\n" + 
				"  customerId: \"R:1000\"\n" + 
				"  productId: \"R:1000\"\n" + 
				"  feedbackType: \"D:PRODUCT_VISUALIZED,PRODUCT_ADDED_TO_BASKET,PRODUCT_REMOVED_FROM_BASKET,PRODUCT_RECOMMENDATION_REMOVED,PRODUCT_RECOMMENDATION_SHOWN\"";
		
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory() );
		
		
		GenerateHTTPPostLoad operation = new GenerateHTTPPostLoad();
		try {
			operation.initalizeFromJson(mapper.readTree(config));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		operation.execute(null, null, null, null, null, null, null);
		
		
		
	}
	
	
}