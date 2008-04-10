package com.dumbhippo.server.dm;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.services.FacebookPhotoDataView;
import com.dumbhippo.services.caches.FacebookPhotoDataCache;
import com.dumbhippo.services.caches.WebServiceCache;

@DMO(classId="http://mugshot.org/p/o/facebookPhotoThumbnail")
public abstract class FacebookPhotoThumbnailDMO extends ThumbnailDMO {
	@WebServiceCache
	private FacebookPhotoDataCache facebookPhotoDataCache;

	protected FacebookPhotoThumbnailDMO(ThumbnailKey key) {
		super(key);
	}
	
	private static Pattern FACEBOOK_URL_PATTERN = Pattern.compile("http://www.facebook.com/photo.php\\?pid=([0-9]+)&id=([0-9]+])");

	private static String extractExtra(String url) {
		Matcher m = FACEBOOK_URL_PATTERN.matcher(url);
		if (m.matches())
			return m.group(2) + "-" + m.group(1);
		else
			throw new RuntimeException("Cannot extract key from Facebook URL '" + url + "'");
	}
	
	@Override
	protected void init() throws NotFoundException {
		super.init();
		
		if (thumbnail == null) {
			List<? extends FacebookPhotoDataView> photos = facebookPhotoDataCache.getSync(getKey().getUserId().toString());
			
			String extra = getKey().getExtra();
			for (FacebookPhotoDataView photo : photos) {
				if (extra.equals(extractExtra(photo.getLink()))) {
					thumbnail = photo;
					return;
				}
			}
			
			throw new NotFoundException("Can't find photo");
		}
	}
	
	public static ThumbnailKey getKey(User user, FacebookPhotoDataView photo) {
		return new ThumbnailKey(user.getGuid(), ThumbnailType.FACEBOOK_PHOTO, extractExtra(photo.getLink()), photo); 
	}
}
