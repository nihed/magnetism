package com.dumbhippo.server.applications;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Application;

public class ApplicationView {
	private Application application;
	private ApplicationIconView icon;
	
	public ApplicationView(Application application) {
		this.application = application;
	}

	public Application getApplication() {
		return application;
	}

	public ApplicationIconView getIcon() {
		return icon;
	}

	public void setIcon(ApplicationIconView icon) {
		this.icon = icon;
	}

	public void writeToXmlBuilder(XmlBuilder builder) {
		builder.appendEmptyNode("application",
								"id", application.getId(),
				                "name", application.getName(),
				                "description", application.getDescription(),
				                "desktopNames", application.getDesktopNames(),
				                "iconUrl", getIcon().getUrl(),
				                "usageCount", "" + application.getUsageCount(),
				                "rank", "" + application.getRank());
				       
	}
}
