package com.dumbhippo.server.dm;

import javax.ejb.EJB;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.services.FlickrPhotoView;
import com.dumbhippo.services.FlickrPhotosView;
import com.dumbhippo.services.FlickrPhotosetView;
import com.dumbhippo.services.FlickrPhotosetsView;
import com.dumbhippo.services.caches.FlickrPhotosetPhotosCache;
import com.dumbhippo.services.caches.FlickrUserPhotosCache;
import com.dumbhippo.services.caches.FlickrUserPhotosetsCache;
import com.dumbhippo.services.caches.WebServiceCache;

@DMO(classId="http://mugshot.org/p/o/facebookPhotoThumbnail")
public abstract class FlickrPhotoThumbnailDMO extends ThumbnailDMO {
	@EJB
	ExternalAccountSystem externalAccountSystem;
	
	@EJB
	IdentitySpider identitySpider;
	
	@WebServiceCache
	private FlickrUserPhotosCache flickrUserPhotosCache;

	@WebServiceCache
	private FlickrUserPhotosetsCache flickrUserPhotosetsCache;

	@WebServiceCache
	private FlickrPhotosetPhotosCache flickrPhotosetPhotosCache;

	protected FlickrPhotoThumbnailDMO(ThumbnailKey key) {
		super(key);
	}
	
	@Override
	protected void init() throws NotFoundException {
		super.init();
		
		if (thumbnail == null) {
			// This is horribly inefficient ... in order to find the photo, we load all user photos
			// and all photos in all photosets of user to find the one with the right ID. We'll normally
			// be saved by the cached object in ThumbnailKey.
			
			User user = identitySpider.lookupUser(getKey().getUserId());
			ExternalAccount externalAccount = externalAccountSystem.lookupExternalAccount(SystemViewpoint.getInstance(), user, ExternalAccountType.FLICKR);
			if (!externalAccount.isLovedAndEnabled())
				throw new NotFoundException("Account is not loved and enabled");
			
			FlickrPhotosView photos = flickrUserPhotosCache.getSync(externalAccount.getHandle());
			
			String extra = getKey().getExtra();
			for (FlickrPhotoView photo : photos.getPhotos()) {
				if (extra.equals(photo.getId())) {
					thumbnail = photo;
					return;
				}
			}
			
			FlickrPhotosetsView photosets = flickrUserPhotosetsCache.getSync(externalAccount.getHandle());
			for (FlickrPhotosetView photoset : photosets.getSets()) {
				for (FlickrPhotoView photo : flickrPhotosetPhotosCache.getSync(photoset.getId())) {
					if (extra.equals(photo.getId())) {
						thumbnail = photo;
						return;
					}
				}
			}
			
			throw new NotFoundException("Can't find photo");
		}
	}
	
	public static ThumbnailKey getKey(User user, FlickrPhotoView photo) {
		return new ThumbnailKey(user.getGuid(), ThumbnailType.FLICKR_PHOTO, photo.getId(), photo); 
	}
}
