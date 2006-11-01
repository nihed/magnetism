package com.dumbhippo.web.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.web.JavascriptResolver;
import com.dumbhippo.web.JavascriptResolver.Page;

public class ScriptTag extends SimpleTagSupport {
	
	private List<String> modules;
	private List<String> files;
	
	private static final String REQUEST_KEY = "scriptTagContext";
	private static final Pattern commaSplitPattern = 
		Pattern.compile("\\s*,\\s*");
	
	private Page getJavascriptPage() {
		Page p = (Page) getJspContext().getAttribute(REQUEST_KEY, PageContext.REQUEST_SCOPE);
		
		if (p == null) {
			JavascriptResolver jsResolver =
				(JavascriptResolver) getJspContext().getAttribute("jsResolver",
					PageContext.APPLICATION_SCOPE);
			if (jsResolver == null)
				throw new RuntimeException("jsResolver not found in servlet context");
			p = jsResolver.newPage();
			// save for next script tag
			getJspContext().setAttribute(REQUEST_KEY, p, PageContext.REQUEST_SCOPE);
		}
		
		return p;
	}
	
	@Override
	public void doTag() throws IOException {
		
		Page p = getJavascriptPage();
		
		JspWriter writer = getJspContext().getOut();
		
		if (modules != null) {
			for (String m : modules)
				p.includeModule(m, writer);
		}
		if (files != null) {
			for (String f : files)
				p.includeFile(f, writer);
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

	// we keep files in the order they were added, but modules vs. files
	// we don't bother to order
	private void addFile(String filename) {
		if (files == null)
			files = new ArrayList<String>();
		// no need to check for dups here, JavascriptResolver.Page does it		
		files.add(filename);
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
	
	public void setSrc(String filename) {
		addFile(filename);
	}
	
	public void setSrcs(String filenames) {
		String[] splitFiles = commaSplitPattern.split(filenames);
		for (String s : splitFiles) {
			addFile(s);
		}
	}
}
