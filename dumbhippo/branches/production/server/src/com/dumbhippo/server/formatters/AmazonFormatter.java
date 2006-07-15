package com.dumbhippo.server.formatters;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.postinfo.AmazonPostInfo;
import com.dumbhippo.postinfo.PostInfo;

public class AmazonFormatter extends DefaultFormatter {

	private void addPrice(XmlBuilder xml, String type, String value) {
		if (value == null)
			return;
		xml.appendTextNode("b", value, "class", "dh-amazon-price");
		xml.append(" " + type + "<br/>");
	}
	
	@Override
	public String getTextAsHtml() {
		PostInfo postInfo = postView.getPost().getPostInfo();

		if (postInfo == null)
			return super.getTextAsHtml();
		
		AmazonPostInfo itemData = (AmazonPostInfo) postInfo;
		
		XmlBuilder xml = new XmlBuilder();
		xml.append("<div class=\"dh-amazon-item\">");
		if (itemData.getSmallImageUrl() != null) {
			xml.append("  <img class=\"dh-amazon-small-image\" style=\"float: left;\" src=\"");
			xml.appendEscaped(itemData.getSmallImageUrl());
			xml.append("\" width=\"" + itemData.getSmallImageWidth());
			xml.append("\" height=\"" + itemData.getSmallImageHeight());
			xml.append("\"/> ");
		}
		addPrice(xml, "New", itemData.getNewPrice());
		addPrice(xml, "Used", itemData.getUsedPrice());
		addPrice(xml, "Refurbished", itemData.getRefurbishedPrice());
		addPrice(xml, "Collectible", itemData.getCollectiblePrice());
		xml.append("<br/></div>");
		xml.append("<div class=\"dh-amazon-description\">");
		xml.appendTextAsHtml(postView.getPost().getText(), postView.getSearchTerms());
		xml.append("</div>");
		return xml.toString();
	}
}
