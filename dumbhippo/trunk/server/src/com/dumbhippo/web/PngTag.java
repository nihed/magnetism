package com.dumbhippo.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.XmlBuilder;

public class PngTag extends SimpleTagSupport implements DynamicAttributes {

	private String src;
	private String klass;
	private String style;
	private List<String> dynamicAttributes;
	
	private static void appendExtraAttributes(XmlBuilder xml, List<String> extraAttributes) {
		if (extraAttributes == null)
			return;
		if ((extraAttributes.size() % 2) != 0)
			throw new IllegalArgumentException("attributes come in key-value pairs");
		
		for (int i = 0; i < extraAttributes.size(); i += 2) {
			String key = extraAttributes.get(i);
			String value = extraAttributes.get(i+1);
			
			xml.append(key);
			xml.append("=\"");
			xml.append(value);
			xml.append("\" ");
		}
	}
	
	static void pngHtml(JspContext context, XmlBuilder xml, String src, String buildStamp, String klass, String style, List<String> extraAttributes) {
		HttpServletRequest request = (HttpServletRequest)((PageContext)context).getRequest();
		BrowserBean browser = BrowserBean.getForRequest(request);
		
		if (browser.getIeAlphaImage()) {
			xml.append("<span style=\"background:#bbbbbb\"");
			if (klass != null) {
				xml.append("class=\"");
				xml.append(klass);
				xml.append("\" ");
			}
			if (style != null) {
				xml.append("style=\"");
				xml.append(style);
				xml.append("\" ");
			}
			xml.append("><img src=\"");
			xml.append(src);
			xml.append("\" style=\"visibility:hidden\" onload=\"dh.actions.fillAlphaPng(this)\"/></span>");
		} else {
			xml.append("<img ");
			if (klass != null) {
				xml.append("class=\"");
				xml.append(klass);
				xml.append("\" ");
			}
			xml.append("src=\"");
			xml.append(src);
			xml.append("\" ");
			if (style != null) {
				xml.append("style=\"");
				xml.append(style);
				xml.append("\" ");
			}
			appendExtraAttributes(xml, extraAttributes);
			xml.append("/>");
		}
	}
	
	static void pngHtml(JspContext context, XmlBuilder xml, String src, String buildStamp, String klass, String style, String... extraAttributes) {
		pngHtml(context, xml, src, buildStamp, klass, style, Arrays.asList(extraAttributes));
	}
	
	public void doTag() throws IOException {
		JspWriter writer = getJspContext().getOut();
		String buildStamp;
		try {
			buildStamp = (String) getJspContext().getVariableResolver().resolveVariable("buildStamp");
		} catch (ELException e) {
			throw new RuntimeException(e);
		}
		XmlBuilder xml = new XmlBuilder();
		pngHtml(getJspContext(), xml, src, buildStamp, klass, style, dynamicAttributes);
		writer.print(xml.toString());
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public void setStyle(String style) {
		this.style = style;
	}
	
	public void setKlass(String klass) {
		this.klass = klass;
	}
	
	public void setDynamicAttribute(String uri, String localName, Object value) throws JspException {
		
		if (localName.equals("class"))
			throw new JspException("use klass instead of class on this tag");
		
		if (dynamicAttributes == null) {
			dynamicAttributes = new ArrayList<String>();
		}
		dynamicAttributes.add(localName);
		// no clue under what circumstances the value would not be a String, but we'll fix that 
		// exception when we come to it I suppose
		dynamicAttributes.add((String) value);
	}
}
