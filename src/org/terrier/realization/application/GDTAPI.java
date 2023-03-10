package org.terrier.realization.application;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.terrier.realization.api.ManagerConfiguration;
import org.terrier.realization.api.ManagerHeathCheck;
import org.terrier.realization.api.ManagerResource;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

 /*
 * Realization Engine 
 * Webpage: https://github.com/terrierteam/realizationengine
 * Contact: richard.mccreadie@glasgow.ac.uk
 * University of Glasgow - School of Computing Science
 * http://www.gla.ac.uk/
 * 
 * The contents of this file are subject to the Apache License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 *
 * The Original Code is Copyright (C) to the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richard.mccreadie@glasgow.ac.uk> (original author)
 */

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
