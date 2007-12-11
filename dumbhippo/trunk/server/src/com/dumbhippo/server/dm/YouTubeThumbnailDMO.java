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
import com.dumbhippo.services.YouTubeVideo;
import com.dumbhippo.services.caches.WebServiceCache;
import com.dumbhippo.services.caches.YouTubeVideosCache;

@DMO(classId="http://mugshot.org/p/o/youTubeThumbnail")
public abstract class YouTubeThumbnailDMO extends ThumbnailDMO {
	@EJB
	private ExternalAccountSystem externalAccountSystem;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@WebServiceCache
	private YouTubeVideosCache youTubeVideosCache;

	protected YouTubeThumbnailDMO(ThumbnailKey key) {
		super(key);
	}
	
	static private Pattern YOUTUBE_URL_PATTERN = Pattern.compile("http://youtube.com/?v=([A-Za-z0-9_-]+)");

	private static String extractExtra(String url) {
		Matcher m = YOUTUBE_URL_PATTERN.matcher(url);
		if (m.matches())
			return m.group(1);
		else
			throw new RuntimeException("Cannot extract key from YouTube URL '" + url + "'");
	}
	
	@Override
	protected void init() throws NotFoundException {
		super.init();
		
		if (thumbnail == null) {
			User user = identitySpider.lookupUser(getKey().getUserId());
			ExternalAccount externalAccount = externalAccountSystem.lookupExternalAccount(SystemViewpoint.getInstance(), user, ExternalAccountType.YOUTUBE);
			if (!externalAccount.isLovedAndEnabled())
				throw new NotFoundException("Account is not loved and enabled");
			
			List<? extends YouTubeVideo> videos = youTubeVideosCache.getSync(externalAccount.getHandle());
			
			String extra = getKey().getExtra();
			for (YouTubeVideo video : videos) {
				if (extra.equals(extractExtra(video.getThumbnailHref()))) {
					thumbnail = video;
					return;
				}
			}
			
			throw new NotFoundException("Can't find video");
		}
	}
	
	public static ThumbnailKey getKey(User user, YouTubeVideo video) {
		return new ThumbnailKey(user.getGuid(), extractExtra(video.getThumbnailHref()), video); 
	}
}
