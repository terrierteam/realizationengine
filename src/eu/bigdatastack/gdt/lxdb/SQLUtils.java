package eu.bigdatastack.gdt.lxdb;

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
