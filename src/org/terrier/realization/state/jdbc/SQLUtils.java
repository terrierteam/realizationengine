package org.terrier.realization.state.jdbc;

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

public class SQLUtils {

	public static String prepareText(String text, int maxLength) {
		text = prepareTextNoQuote(text, maxLength);
		
		if (text.endsWith("'") && text.endsWith("'")) return text;
		else return "'"+text+"'";
	}
	
	public static String prepareTextNoQuote(String text, int maxLength) {
		if (text.length()>maxLength) text = text.substring(0, maxLength-1);
		text = text.replaceAll("'", "");
		return text;
	}
	
}
