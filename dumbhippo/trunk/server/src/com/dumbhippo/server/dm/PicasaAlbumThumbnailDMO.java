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
import com.dumbhippo.services.PicasaAlbum;
import com.dumbhippo.services.caches.PicasaAlbumsCache;
import com.dumbhippo.services.caches.WebServiceCache;

@DMO(classId="http://mugshot.org/p/o/youTubeThumbnail")
public abstract class PicasaAlbumThumbnailDMO extends ThumbnailDMO {
	@EJB
	private ExternalAccountSystem externalAccountSystem;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@WebServiceCache
	private PicasaAlbumsCache picasaAlbumsCache;

	protected PicasaAlbumThumbnailDMO(ThumbnailKey key) {
		super(key);
	}
	
	// Although album names can have arbitrary Unicode in them, everything but ascii letters
	// and digits is stripped out in creating the URL (with upper casing of the beginning
	// of ascii letter sequences ... "test bourrée" => "TestBourrE")
	private static Pattern PICASA_URL_PATTERN = Pattern.compile("http://picasaweb.google.com/([A-Za-z0-9]+)/([A-Za-z0-9]+)");

	private static String extractExtra(String url) {
		Matcher m = PICASA_URL_PATTERN.matcher(url);
		if (m.matches())
			return m.group(1) + "-" + m.group(2);
		else
			throw new RuntimeException("Cannot extract key from Picasa URL '" + url + "'");
	}
	
	@Override
	protected void init() throws NotFoundException {
		super.init();
		
		if (thumbnail == null) {
			User user = identitySpider.lookupUser(getKey().getUserId());
			ExternalAccount externalAccount = externalAccountSystem.lookupExternalAccount(SystemViewpoint.getInstance(), user, ExternalAccountType.PICASA);
			if (!externalAccount.isLovedAndEnabled())
				throw new NotFoundException("Account is not loved and enabled");
			
			List<? extends PicasaAlbum> albums = picasaAlbumsCache.getSync(externalAccount.getHandle());
			
			String extra = getKey().getExtra();
			for (PicasaAlbum album : albums) {
				if (extra.equals(extractExtra(album.getThumbnailHref()))) {
					thumbnail = album;
					return;
				}
			}
			
			throw new NotFoundException("Can't find video");
		}
	}
	
	public static ThumbnailKey getKey(User user, PicasaAlbum album) {
		return new ThumbnailKey(user.getGuid(), extractExtra(album.getThumbnailHref()), album); 
	}
}
