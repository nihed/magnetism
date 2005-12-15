package com.dumbhippo.server.formatters;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.postinfo.EbayPostInfo;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.server.PostView;

public class EbayFormatter extends DefaultFormatter {
	
	@Override
	public String getTextAsHtml(PostView postView) {
		PostInfo postInfo = postView.getPost().getPostInfo();

		if (postInfo == null)
			return super.getTextAsHtml(postView);
		
		EbayPostInfo itemData = (EbayPostInfo) postInfo;
		
		String pictureUrl = itemData.getPictureUrl();
		if (pictureUrl == null)
			return super.getTextAsHtml(postView);
		
		XmlBuilder xml = new XmlBuilder();
		xml.append("<div class=\"dh-ebay-item\">");
		xml.append("  <img class=\"dh-ebay-small-image\" style=\"float: left; max-width: 70; max-height: 70;\" src=\"");
		xml.appendEscaped(pictureUrl);
		xml.append("\"/> ");
		xml.append("</div>");
		xml.append("<p class=\"dh-ebay-description\">");
		xml.appendTextAsHtml(postView.getPost().getText(), postView.getSearchTerms());
		xml.append("</p>");
		return xml.toString();
	}
}
