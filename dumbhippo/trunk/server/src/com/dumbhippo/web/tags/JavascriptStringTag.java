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
			case '\\':
				sb.append("\\\\");
				break;
			// We use unicode escapes for all XML characters inside the string
			// so that we can safely use the jsString tag inside an attribute
		    // value, say. Note that although we use a unicode escape for 
			// ' here, the outer ' is still going to be there literally
			// so the caller is responsible for making sure that's OK. (The
			// enclosing attribute should be quoted with ", for example.)
			// But that's static and easy to verify. 
			case '&':
			case '<':
			case '>':
			case '\"':
			case '\'':
				sb.append(String.format("\\u%04X", new Integer(c)));
				break;
			default:
				if (c >= 32 && c <= 126) // printable ascii chars
					sb.append(c);
				else
					sb.append(String.format("\\u%04X", new Integer(c)));
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
