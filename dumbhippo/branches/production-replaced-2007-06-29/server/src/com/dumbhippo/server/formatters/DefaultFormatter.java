package com.dumbhippo.server.formatters;

import javax.ejb.EJBContext;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.server.views.PostView;

public class DefaultFormatter implements PostFormatter {
	
	protected PostView postView;
	
	public void init(PostView postView, EJBContext ejbContext) {
		this.postView = postView;
	}
	
	public String getTitleAsText() {
		String title = postView.getPost().getExplicitTitle();
		if (title != null && !title.equals(""))
			return title;
		else
			return postView.getUrl();
	}
	
	public String getTextAsText() {
		return postView.getPost().getText();
	}
	
	public String getTitleAsHtml() {
		XmlBuilder xml = new XmlBuilder();
		xml.appendEscaped(getTitleAsText(), postView.getSearchTerms());
		return xml.toString();
	}

	public String getTextAsHtml() {
		XmlBuilder xml = new XmlBuilder();
		xml.appendTextAsHtml(getTextAsText(), postView.getSearchTerms());
		return xml.toString();
	}
}
