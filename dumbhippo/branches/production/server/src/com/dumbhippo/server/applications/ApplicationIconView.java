package com.dumbhippo.server.applications;

import com.dumbhippo.persistence.ApplicationIcon;

public class ApplicationIconView {
	ApplicationIcon icon;
	int desiredSize;
	int displayWidth;
	int displayHeight;
	
	public ApplicationIconView(ApplicationIcon icon, int desiredSize) {
		this.icon = icon;
		this.desiredSize = desiredSize;

		int effectiveSize = icon.getSize();
		if (effectiveSize == -1) {
			effectiveSize = Math.max(icon.getActualWidth(), icon.getActualHeight());
		}
		
		double scale = (double)desiredSize / effectiveSize;
		if (scale > 0.9 && scale < 1.1)
			scale = 1.0;
		
		displayWidth = (int)Math.round(icon.getActualWidth() * scale);
		displayHeight = (int)Math.round(icon.getActualHeight() * scale);
	}

	public ApplicationIcon getIcon() {
		return icon;
	}

	public int getDesiredSize() {
		return desiredSize;
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
		// The database ID should work fine here as a version to allow web-server
		// caching; it will change iff. the image changes.
		return "/files/appicons/" + icon.getApplication().getId() + "?size=" + desiredSize + "&v=" + icon.getId();
	}
}
