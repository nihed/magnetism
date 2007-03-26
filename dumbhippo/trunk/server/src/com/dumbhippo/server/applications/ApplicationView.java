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
	
	public String getIconUrl() {
		if (icon != null)
			return icon.getUrl();
		return "/images3/unknownapp48.png";
	}

	public void setIcon(ApplicationIconView icon) {
		this.icon = icon;
	}

	public void writeToXmlBuilder(XmlBuilder builder) {
		builder.appendEmptyNode("application",
								"id", application.getId(),
				                "name", application.getName(),
				                "tooltip", application.getTooltip(),
				                "category", application.getCategory().getDisplayName(),
				                "description", application.getDescription(),
				                "desktopNames", application.getDesktopNames(),
				                "iconUrl", getIconUrl(),
				                "usageCount", "" + application.getUsageCount(),
				                "rank", "" + application.getRank());
				       
	}
}
