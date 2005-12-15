package com.dumbhippo.server.formatters;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.server.PostView;

public class DefaultFormatter implements PostFormatter {
	
	public String getTitleAsHtml(PostView postView) {
		XmlBuilder xml = new XmlBuilder();
		xml.appendEscaped(postView.getTitle(), postView.getSearchTerms());
		return xml.toString();
	}

	public String getTextAsHtml(PostView postView) {
		XmlBuilder xml = new XmlBuilder();
		xml.appendTextAsHtml(postView.getPost().getText(), postView.getSearchTerms());
		return xml.toString();
	}
}
