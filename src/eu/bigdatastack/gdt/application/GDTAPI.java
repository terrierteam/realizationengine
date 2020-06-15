package eu.bigdatastack.gdt.application;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import org.eclipse.jetty.servlets.CrossOriginFilter;

import eu.bigdatastack.gdt.api.ManagerConfiguration;
import eu.bigdatastack.gdt.api.ManagerHeathCheck;
import eu.bigdatastack.gdt.api.ManagerResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public class GDTAPI extends Application<ManagerConfiguration> {

	GDTManager manager;
	
	public GDTAPI(GDTManager manager) {
		this.manager= manager;
	}
	
	@Override
	public void run(ManagerConfiguration configuration, Environment environment) throws Exception {
		
		// Enable CORS headers (see https://en.wikipedia.org/wiki/Cross-origin_resource_sharing) 
	    final FilterRegistration.Dynamic cors =
	        environment.servlets().addFilter("CORS", CrossOriginFilter.class);

	    // Configure CORS parameters
	    cors.setInitParameter("allowedOrigins", "*");
	    cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
	    cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");

	    // Add URL mapping
	    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
		
		final ManagerResource resource = new ManagerResource(manager);
		final ManagerHeathCheck healthCheck = new ManagerHeathCheck();
		environment.healthChecks().register("manager", healthCheck);
		environment.jersey().register(resource);
		
	}

}
