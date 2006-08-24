package com.dumbhippo.server.formatters;

import javax.ejb.EJBContext;

import com.dumbhippo.server.PostView;

public interface PostFormatter {

	public void init(PostView postView, EJBContext ejbContext);
	
	public String getTitleAsText();
	
	public String getTextAsText();
	
	public String getTitleAsHtml();
	
	public String getTextAsHtml();
}
