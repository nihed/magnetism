package com.dumbhippo.server.dm;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.EJB;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.services.smugmug.rest.bind.Image;
import com.dumbhippo.services.caches.SmugmugAlbumsCache;
import com.dumbhippo.services.caches.WebServiceCache;

@DMO(classId="http://mugshot.org/p/o/smugmugAlbumThumbnail")
public abstract class SmugmugAlbumThumbnailDMO extends ThumbnailDMO {
	@EJB
	private ExternalAccountSystem externalAccountSystem;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@WebServiceCache
	private SmugmugAlbumsCache smugmugAlbumsCache;

	protected SmugmugAlbumThumbnailDMO(ThumbnailKey key) {
		super(key);
	}
	
	@Override
	protected void init() throws NotFoundException {
		super.init();
		
		if (thumbnail == null) {
			User user = identitySpider.lookupUser(getKey().getUserId());
			ExternalAccount externalAccount = externalAccountSystem.lookupExternalAccount(SystemViewpoint.getInstance(), user, ExternalAccountType.SMUGMUG);
			if (!externalAccount.isLovedAndEnabled())
				throw new NotFoundException("Account is not loved and enabled");
			
			List<? extends Image> albums = smugmugAlbumsCache.getSync(externalAccount.getExtra());
			
			String extra = getKey().getExtra();
			for (Image album : albums) 
			{
					thumbnail = album;
					return;
			}
			
			throw new NotFoundException("Can't find album");
		}
	}
	
	public static ThumbnailKey getKey(User user, Image album) {
		return new ThumbnailKey(user.getGuid(), ThumbnailType.SMUGMUG_ALBUM, album.getFileName(), album); 
	}
}
