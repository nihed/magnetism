package com.dumbhippo.services;

import java.util.List;

public interface FlickrPhotosetData {	
	public static class PhotoData {
		private String thumbnailUrl;
		private String pageUrl;
		
		public String getPageUrl() {
			return pageUrl;
		}
		public void setPageUrl(String pageUrl) {
			this.pageUrl = pageUrl;
		}
		public void setThumbnailUrl(String thumbnailUrl) {
			this.thumbnailUrl = thumbnailUrl;
		}
		public String getThumbnailUrl() {
			return thumbnailUrl;
		}
	};
	
	public List<PhotoData> getPhotoData();
}
