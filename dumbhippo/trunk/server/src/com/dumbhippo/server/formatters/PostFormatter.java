package com.dumbhippo.server.formatters;

import com.dumbhippo.server.PostView;

public interface PostFormatter {

	public String getTitleAsHtml(PostView postView);
	
	public String getTextAsHtml(PostView postView);
	
}
