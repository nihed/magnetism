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

	// this could be more sophisticated eventually, e.g. know that "fedora7" matches "fedora" or 
	// things of that nature. For now this is mostly pointless, clients could easily do it 
	// themselves.
	private String extractPackageName(String packageNames, String forDistribution) {
		String[] distPackagePairs = packageNames.split(";");
		for (String pair : distPackagePairs) {
			int eq = pair.indexOf('=');
			if (eq < 0)
				continue;
			String dist = pair.substring(0, eq);
			if (dist.equals(forDistribution))
				return pair.substring(eq+1);
		}
		return null;
	}
	
	/**
	 *  distribution and lang can be null if unknown
	 */
	public void writeToXmlBuilder(XmlBuilder builder, String distribution, String lang) {
		String packageName = null;
		if (distribution != null) {
			packageName = extractPackageName(application.getPackageNames(), distribution);
		}
		
		builder.openElement("application",
							"id", application.getId(),
				            "name", application.getName(),
				            "genericName", application.getGenericName(),				                
				            "tooltip", application.getTooltip(),
				            "category", application.getCategory().getDisplayName(),
				            "desktopNames", application.getDesktopNames(),
				            // only include the full packageNames if distribution was unspecified, 
				            // if distribution was given we provide packageName if there's a useful 
				            // name
				            "packageNames", distribution == null ? application.getPackageNames() : null,
				            // openElement just skips this if it's null
				            "packageName", packageName,
				            "iconUrl", getIconUrl(),
				            "usageCount", "" + application.getUsageCount(),
				            "rank", "" + application.getRank());
		builder.appendTextNode("description", application.getDescription());
		builder.closeElement();
	}
}
