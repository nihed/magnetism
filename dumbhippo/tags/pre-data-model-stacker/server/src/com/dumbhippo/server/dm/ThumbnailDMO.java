package com.dumbhippo.server.dm;

import com.dumbhippo.Thumbnail;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.MetaConstruct;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/thumbnail", resourceBase="/o/thumbnail")
public abstract class ThumbnailDMO extends DMObject<ThumbnailKey> {
	protected Thumbnail thumbnail;
	
	protected ThumbnailDMO(ThumbnailKey key) {
		super(key);
	}
	
	@MetaConstruct
	public static Class<? extends ThumbnailDMO> getDMOClass(ThumbnailKey key) {
		switch (key.getType()) {
		case FACEBOOK_PHOTO:
			return FacebookPhotoThumbnailDMO.class;
		case FLICKR_PHOTO:
			return FlickrPhotoThumbnailDMO.class;
		case PICASA_ALBUM:
			return PicasaAlbumThumbnailDMO.class;
		case YOUTUBE:
			return YouTubeThumbnailDMO.class;
		}
		
		return null;
	}

	@Override
	protected void init() throws NotFoundException {
		if (getKey().getObject() != null) {
			thumbnail = (Thumbnail)getKey().getObject();
		}
	}
	
	/**
	 * The URL of the thumbnail image 
	 */
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getSrc() {
		return thumbnail.getThumbnailSrc();
	}

	/**
	 * The URL that the thumbnail should link to 
	 */
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getLink() {
		return thumbnail.getThumbnailHref();
	}
	
	/**
	 * The tooltip or caption 
	 */
	@DMProperty(defaultInclude=true)
	public String getTitle() {
		return thumbnail.getThumbnailTitle();
	}
	
	/**
	 * The width of the thumbnail image in pixels 
	 */
	@DMProperty(defaultInclude=true)
	public int getWidth() {
		return thumbnail.getThumbnailWidth();
	}
	
	/**
	 * The height of the thumbnail image in pixels 
	 */
	@DMProperty(defaultInclude=true)
	public int getHeight() {
		return thumbnail.getThumbnailHeight();
	}
}
