package com.dumbhippo.server.formatters;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.postinfo.EbayPostInfo;
import com.dumbhippo.postinfo.PostInfo;

public class EbayFormatter extends DefaultFormatter {
	
	@Override
	public String getTextAsHtml() {
		PostInfo postInfo = postView.getPost().getPostInfo();

		if (postInfo == null)
			return super.getTextAsHtml();
		
		EbayPostInfo itemData = (EbayPostInfo) postInfo;
		
		String pictureUrl = itemData.getPictureUrl();
		if (pictureUrl == null)
			return super.getTextAsHtml();
		
		XmlBuilder xml = new XmlBuilder();
		xml.append("<div class=\"dh-ebay-item\">");
		xml.append("  <img class=\"dh-ebay-small-image\" style=\"float: left; max-width: 70; max-height: 70;\" src=\"");
		xml.appendEscaped(pictureUrl);
		xml.append("\"/> ");
		xml.append("</div>");
		xml.append("<div class=\"dh-ebay-description\">");
		xml.appendTextAsHtml(postView.getPost().getText(), postView.getSearchTerms());
		xml.append("</div>");
		return xml.toString();
	}
}
