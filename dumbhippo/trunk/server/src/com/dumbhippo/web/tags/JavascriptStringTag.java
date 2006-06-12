package com.dumbhippo.web.tags;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class JavascriptStringTag extends SimpleTagSupport {
	
	private String value;
	
	// public for unit testing
	public static String toJavascriptString(String src) {
		
		if (src == null)
			return "null";
		
		StringBuilder sb = new StringBuilder();
		
		sb.append('\'');
		
		for (char c : src.toCharArray()) {
			// we special-case some things and use the "human readable" escape
			// sequences, though we could just always use the \\uNNNN and simplify 
			// this code. We could also use some more human-readable for vertical tab
			// and form feed and other crap nobody cares about, but we don't
			switch (c) {
			case '\0':
				sb.append("\\0");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\"':
				sb.append("\\\"");
				break;
			case '\'':
				sb.append("\\'");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			// These xml characters don't need escaping if 
			// our web pages are right, since the script section should
			// be cdata'd or something, but paranoia never hurt anyone
			case '&':
			case '<':
			case '>':
				sb.append(String.format("\\u%04X", (int) c));
				break;
			default:
				if (c >= 32 && c <= 126) // printable ascii chars
					sb.append(c);
				else
					sb.append(String.format("\\u%04X", (int) c));
			}
		}
		
		sb.append('\'');
		
		return sb.toString();
	}
	
	@Override
	public void doTag() throws IOException {
		JspWriter writer = getJspContext().getOut();
		
		writer.print(toJavascriptString(value));
	}
	
	public void setValue(String value) {
		this.value = value;
	}
}
