package com.dumbhippo.server.applications;

import com.dumbhippo.persistence.ApplicationIcon;

public class ApplicationIconView {
	private String url;
	private ApplicationIcon icon;
	private int desiredSize;
	private int displayWidth;
	private int displayHeight;
	
	public ApplicationIconView(ApplicationIcon icon) {
		this(icon, -1);
	}
	
	public ApplicationIconView(ApplicationIcon icon, int desiredSize) {
		this.icon = icon;
		this.desiredSize = desiredSize; // we want the field to remain -1 even if we override it below
		
		int effectiveSize = icon.getSize();
		if (effectiveSize == -1) {
			effectiveSize = Math.max(icon.getActualWidth(), icon.getActualHeight());
		}
		
		// for computing scale, if desiredSize is -1 that means "use the effective size" 

		double scale = desiredSize >= 0 ? ((double)desiredSize / effectiveSize) : 1.0;
		if (scale > 0.9 && scale < 1.1)
			scale = 1.0;
		
		displayWidth = (int)Math.round(icon.getActualWidth() * scale);
		displayHeight = (int)Math.round(icon.getActualHeight() * scale);
	}
	
	public ApplicationIconView(String url, int actualSize, int desiredSize) {
		this.url = url;
	
		// always set desiredSize to -1 since the url is fixed anyway
		// (this is used for the "unknown icon") we don't want to add the 
		// size= param ... we ignore this.desiredSize completely in getUrl() 
		// in this case, in fact.
		this.desiredSize = -1;
	
		// (but used passed-in desiredSize to scale)
		double scale = desiredSize >= 0 ? ((double)desiredSize / actualSize) : 1.0;		
		if (scale > 0.9 && scale < 1.1)
			scale = 1.0;
		
		displayWidth = (int)Math.round(actualSize * scale);
		displayHeight = (int)Math.round(actualSize * scale);
	}

	public String getIconKey() {
		return icon != null ? icon.getIconKey() : null;
	}

	/**
	 * @return width to scale to if we are displaying this icon at the
	 *    size at which it was looked up.the idea here is to preserve 
	 *    aspect ratio and to avoid scaling icons by small amounts (less than 10%)
	 */
	public int getDisplayWidth() {
		return displayWidth;
	}
	
	/**
	 * @return height to scale to if we are displaying this icon at the
	 *    size at which it was looked up.the idea here is to preserve 
	 *    aspect ratio and to avoid scaling icons by small amounts (less than 10%)
	 */
	public int getDisplayHeight() {
		return displayHeight;
	}
	
	public String getUrl() {
		if (url != null) {
			return url;
		} else {
			// The database ID should work fine here as a version to allow web-server
			// caching; it will change iff. the image changes.
			StringBuilder sb = new StringBuilder("/files/appicons/");
			sb.append(icon.getApplication().getId());
			if (desiredSize >= 0) {
				sb.append("?size=");
				sb.append(desiredSize);
				sb.append("&v=");
				sb.append(icon.getId());
			} else {
				sb.append("?v=");
				sb.append(icon.getId());
			}
			return sb.toString();
		}
	}
}
