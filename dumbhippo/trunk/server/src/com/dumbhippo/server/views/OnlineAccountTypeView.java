package com.dumbhippo.server.views;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.OnlineAccountType;

public class OnlineAccountTypeView {
	private OnlineAccountType onlineAccountType;
	
    public OnlineAccountTypeView(OnlineAccountType onlineAccountType) {
    	this.onlineAccountType = onlineAccountType;
    }
    
	/**
	 *  we are not using lang at the moment
	 */
	public void writeToXmlBuilder(XmlBuilder builder, String lang) {
		builder.openElement("onlineAccountType",
				            "name", onlineAccountType.getName(),
				            "fullName", onlineAccountType.getFullName(),				                
				            "siteName", onlineAccountType.getSiteName(),
				            "site", onlineAccountType.getSite(),
				            "userInfoType", onlineAccountType.getUserInfoType(),
				            "supported", Boolean.toString(onlineAccountType.isSupported()));
		builder.closeElement();
	}
}
