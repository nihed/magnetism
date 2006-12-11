package com.dumbhippo.email;

import com.dumbhippo.XmlBuilder;

public abstract class MessageContent {

	private String messageText;
	private String messageHtml;
	
	public abstract String getSubject();
	
	protected abstract void buildMessage(StringBuilder messageText, XmlBuilder messageHtml);	
	
	private void buildMessage() {
		if (messageText == null) {
			StringBuilder sb = new StringBuilder();
			XmlBuilder xml = new XmlBuilder();
			buildMessage(sb, xml);
			messageText = sb.toString();
			messageHtml = xml.toString();
		}
	}
	
	public String getTextAlternative() {
		buildMessage();
		return messageText;
	}
	
	public String getHtmlAlternative() {
		buildMessage();
		return messageHtml;
	}
	
	public boolean getHtmlReferencesLogoImage() {
		return true;
	}
	
	protected void openStandardHtmlTemplate(XmlBuilder xml) {
		xml.appendHtmlHead("");
		xml.append("<body style=\"margin: 1em;\">\n");
		xml.append("<div style=\"margin-bottom: 10px;\"><img src=\"cid:mugshotlogo\"/></div>\n");
	}
	
	protected void closeStandardHtmlTemplate(XmlBuilder xml) {
		xml.append("</body>\n</html>\n");
	}
}
