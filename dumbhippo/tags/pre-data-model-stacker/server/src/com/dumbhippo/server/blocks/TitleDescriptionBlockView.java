package com.dumbhippo.server.blocks;

import java.util.Date;


public interface TitleDescriptionBlockView extends TitleBlockView {

	public String getDescriptionAsHtml();
	
	public String getDescription();
	
	public Date getSentDate();
	
	public String getSentTimeAgo();
}
