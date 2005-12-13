package com.dumbhippo.server.formatters;

import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.postinfo.FlickrPostInfo;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.server.PostView;

public class FlickrFormatter extends DefaultFormatter {
	
	@Override
	public String getTextAsHtml(PostView postView) {
		PostInfo postInfo = postView.getPost().getPostInfo();

		if (postInfo == null)
			return super.getTextAsHtml(postView);
		
		FlickrPostInfo itemData = (FlickrPostInfo) postInfo;
		
		XmlBuilder xml = new XmlBuilder();		
		List<String> urls = itemData.getThumbnailUrls();
		for (String url : urls) {
			xml.append("<img class=\"dh-flickr-thumbnail\" src=\"");
			xml.appendEscaped(url);
			xml.append("\"></img>");
			
		}
		return xml.toString();
	}
}
