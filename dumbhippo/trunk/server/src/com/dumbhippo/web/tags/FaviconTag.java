package com.dumbhippo.web.tags;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class FaviconTag extends SimpleTagSupport {
	String link;
	
	@Override
	public void doTag() throws IOException {
		// FIXME this really isn't right; we should just upload the favicon when the 
		// link gets shared in all probability
		
		// this craziness is in order to first try 
		// foo.com/bar/baz/favicon.ico and then foo.com/favicon.ico
		// div instead of img is to avoid the broken image icon
		String template = "<div class=\"dh-favicon\" style=\"width: 16; height: 16;\">\n"
			// on the bottom /images/favicon.ico, a popular choice but requires a <link>
			// tag pointing to it (there isn't a convention here) so we really shouldn't
			// use this without parsing the page
				+ "    <div style=\"background: url(%s); position: absolute; width: 16; height: 16; z-index: 0;\"></div>\n"
			// in the middle is the base favicon, probably most common
				+ "    <div style=\"background: url(%s); position: absolute; width: 16; height: 16; z-index: 1;\"></div>\n"
			// on the top is the more specific one
				+ "    <div style=\"background: url(%s); position: absolute; width: 16; height: 16; z-index: 2;\"></div>\n"
				+ "</div>";
		
		URL url;
		try {
			url = new URL(link);
		} catch (MalformedURLException e) {
			return;
		}
		URL images = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/images/favicon.ico");
		URL base = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/favicon.ico");
		String path = url.getPath();
		int lastSlash = path.lastIndexOf('/');
		if (lastSlash >= 0)
			path = path.substring(0, lastSlash + 1) + "favicon.ico";
		else
			path = "/favicon.ico";
		URL specific = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
		
		String html = String.format(template,
				images.toExternalForm(), base.toExternalForm(), specific.toExternalForm());
		
		JspWriter writer = getJspContext().getOut();
		writer.print(html);
	}
	
	public void setLink(String link) {
		this.link = link;
	}
}
