package com.dumbhippo.web.tags;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.SkipPageException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class LoginTag extends SimpleTagSupport {
	private String next;
	
	public void setNext(String next) {
		this.next = next;
	}
	
	@Override
	public void doTag() throws IOException, SkipPageException {
		StringBuilder url = new StringBuilder("who-are-you");
		if (next != null) {
			url.append("?next=");
			url.append(URLEncoder.encode(next, "UTF-8"));
		}
		
		HttpServletResponse response = (HttpServletResponse)((PageContext)getJspContext()).getResponse();
		response.sendRedirect(url.toString());
		
		throw new SkipPageException();
	}
}
