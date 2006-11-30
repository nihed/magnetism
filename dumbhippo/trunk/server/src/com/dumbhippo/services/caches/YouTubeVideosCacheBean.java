package com.dumbhippo.services.caches;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedYouTubeVideo;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YouTubeVideo;
import com.dumbhippo.services.YouTubeVideoFeedParser;

@Stateless
public class YouTubeVideosCacheBean extends AbstractListCacheBean<String,YouTubeVideo,CachedYouTubeVideo> implements
		YouTubeVideosCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YouTubeVideosCacheBean.class);

	static private final long VIDEO_EXPIRATION = 1000 * 60 * 60 * 24 * 7;
	
	static private final int NUMBER_OF_SAMPLE_VIDEOS = 7; 	
	
	public YouTubeVideosCacheBean() {
		super(Request.YOUTUBE_VIDEOS, YouTubeVideosCache.class, VIDEO_EXPIRATION);
	}

	@Override
	public List<CachedYouTubeVideo> queryExisting(String key) {
		Query q = em.createQuery("SELECT video FROM CachedYouTubeVideo video WHERE video.owner = :owner");
		q.setParameter("owner", key);
		
		List<CachedYouTubeVideo> results = TypeUtils.castList(CachedYouTubeVideo.class, q.getResultList());
		return results;
	}

	@Override
	public void setAllLastUpdatedToZero(String key) {
		EJBUtil.prepareUpdate(em, CachedYouTubeVideo.class);
		
		Query q = em.createQuery("UPDATE CachedYouTubeVideo" + 
				" SET lastUpdated = '1970-01-01 00:00:00' " + 
				" WHERE owner = :owner");
		q.setParameter("owner", key);
		int updated = q.executeUpdate();
		logger.debug("{} cached items expired", updated);
	}	
	
	@Override
	public YouTubeVideo resultFromEntity(CachedYouTubeVideo entity) {
		return entity.toThumbnail();
	}

	@Override
	public CachedYouTubeVideo entityFromResult(String key, YouTubeVideo result) {
		return new CachedYouTubeVideo(key, result);
	}

	@Override
	protected List<YouTubeVideo> fetchFromNetImpl(String key) {
		return YouTubeVideoFeedParser.getVideosForUser(key, NUMBER_OF_SAMPLE_VIDEOS);
	}

	@Override
	public CachedYouTubeVideo newNoResultsMarker(String key) {
		return CachedYouTubeVideo.newNoResultsMarker(key);
	}
}
