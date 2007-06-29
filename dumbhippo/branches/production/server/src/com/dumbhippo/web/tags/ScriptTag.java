package com.dumbhippo.web.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.web.JavascriptResolver;
import com.dumbhippo.web.JavascriptResolver.Page;

public class ScriptTag extends SimpleTagSupport {
	
	private List<String> modules;
	
	private static final String CONTEXT_KEY = "scriptTagContext";
	private static final Pattern commaSplitPattern = 
		Pattern.compile("\\s*,\\s*");
	
	private static Page getJavascriptPage(JspContext context) {
		// FIXME REQUEST_SCOPE screws up if we use jsp forwarding, but
		// PAGE_SCOPE is really tag scope not page scope so doesn't work at all
		Page p = (Page) context.getAttribute(CONTEXT_KEY, PageContext.REQUEST_SCOPE);
		
		if (p == null) {
			JavascriptResolver jsResolver =
				(JavascriptResolver) context.getAttribute("jsResolver",
					PageContext.APPLICATION_SCOPE);
			if (jsResolver == null)
				throw new RuntimeException("jsResolver not found in servlet context");
			p = jsResolver.newPage();
			// save for next script tag
			context.setAttribute(CONTEXT_KEY, p, PageContext.REQUEST_SCOPE);
		}
		
		return p;		
	}
	
	public static void includeModule(String module, JspContext context, JspWriter writer) throws IOException {
		Page p = getJavascriptPage(context);
		p.includeModule(module, writer);
	}
	
	public static void includeModule(String module, JspContext context, XmlBuilder xml) {
		Page p = getJavascriptPage(context);
		p.includeModule(module, xml);
	}	
	
	private Page getJavascriptPage() {
		return getJavascriptPage(getJspContext());
	}	
	
	@Override
	public void doTag() throws IOException {
		
		Page p = getJavascriptPage();
		
		JspWriter writer = getJspContext().getOut();
		
		if (modules != null) {
			for (String m : modules)
				p.includeModule(m, writer);
		}
	}
	
	// we keep modules in the order they were added, but modules vs. files
	// we don't bother to order
	private void addModule(String module) {
		if (modules == null)
			modules = new ArrayList<String>();
		// no need to check for dups here, JavascriptResolver.Page does it
		modules.add(module);
	}

	public void setModule(String module) {
		addModule(module);
	}
	
	public void setModules(String modules) {
		String[] splitModules = commaSplitPattern.split(modules);
		for (String s : splitModules) {
			addModule(s);
		}
	}
}
