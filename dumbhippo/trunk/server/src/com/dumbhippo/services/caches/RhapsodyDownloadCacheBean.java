package com.dumbhippo.services.caches;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.slf4j.Logger;

import sun.text.Normalizer;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.URLUtils;
import com.dumbhippo.persistence.caches.CachedRhapsodyDownload;
import com.dumbhippo.server.BanFromWebTier;

@BanFromWebTier
//@Stateless // for now, these cache beans are our own special kind of bean and not EJBs due to a jboss bug
public class RhapsodyDownloadCacheBean extends AbstractBasicCacheWithStorageBean<String,Boolean,CachedRhapsodyDownload> implements
		RhapsodyDownloadCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(RhapsodyDownloadCacheBean.class);

	// 14 days
	static private final int RHAPLINK_EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24 * 14;
	
	public RhapsodyDownloadCacheBean() {
		super(Request.RHAPSODY_DOWNLOAD, RhapsodyDownloadCache.class, RHAPLINK_EXPIRATION_TIMEOUT);
	}
	
	/*
	 * Mangle a string to work in a Rhapsody friendly URL - remove all 
	 * punctuation and whitespace and lowercase it
	 */
	private static String rhapString(String s, boolean canonicalize) {
		// See http://rws-blog.rhapsody.com/rhapsody_web_services/2006/04/new_album_urls.html		
		//
		// Internationalization:
		//  Rhapsody somtimes strips accents
		//   http://play.rhapsody.com/edithpiaf (Édith Piaf)
		//   http://play.rhapsody.com/lunasa (Lúnasa)
		//   http://play.rhapsody.com/danu (Danú)
		// Or strips accented characters:
		//   http://www.rhapsody.com/carlosacua (Carlos Acuña)
		//   http://www.rhapsody.com/claeshkonahnsjo (Claes-Håkon Ahnsjö)
		// Or sometimes something different:
		//   http://www.rhapsody.com/concertokoln2 (Concerto Köln)
		//
		// We try both of the first two ways.
		
		if (canonicalize)
			s = Normalizer.normalize(s, Normalizer.DECOMP_COMPAT, 0);
		
		//  implementing the first is to normalize to Unicode normalization
		//  form NFKD before doing the stripping below. (java.text.Normalizer
		//  is new for Java 6, but I think there are alternatives that can be used
		//  for Java 5.) The harder part is adapting the code that calls this
		//  to deal with multiple URLs. I think the right way to handle it
		//  is to make the service  artist/album/track => URL rather than
		//  URL => boolean.
		//
		return s.replaceAll("[^a-zA-Z0-9]","").toLowerCase();
	}
	
	private void addLink(List<String> links, String album, String artist, String name, boolean canonicalize) {
		String link = "http://play.rhapsody.com/" + rhapString(artist, canonicalize) + "/" + rhapString(album, canonicalize) + "/" + rhapString(name, canonicalize);
		
		if (!links.contains(link))
			links.add(link);
	}

	// nothing database-related about this method, so don't force a transaction
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> buildLinks(String album, String artist, String name) {
		if (artist == null || album == null || name == null) {
			logger.debug("missing artist or album or track null, not looking up album {} by artist {} on Rhapsody", 
					     album, artist);
			return Collections.emptyList();
		}
		
		List<String> links = new ArrayList<String>();
		
		addLink(links, album, artist, name, false);
		addLink(links, album, artist, name, true);
		
		return links;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override
	public Boolean checkCache(String link) throws NotCachedException {
		
		// allowing a null return here is just annoying for everyone 
		// (null return means "no results marker" which we treat the 
		// same as a result of false)
		
		Boolean b = super.checkCache(link);
		if (b == null)
			return false;
		else
			return b;
	}

	@Override
	protected Boolean fetchFromNetImpl(String link) {
		boolean found = false;
		try {
			URL u = new URL(link);
			URLConnection connection = URLUtils.openConnection(u);
			if (!(connection instanceof HttpURLConnection)) {
				logger.error("Got a weird connection of type {} for URL {}; skipping", connection.getClass().getName(), u);
			} else {
				logger.debug("Making http request for link {}", link);
				
				HttpURLConnection httpConnection = (HttpURLConnection)connection;
				httpConnection.setRequestMethod("HEAD");
				httpConnection.setInstanceFollowRedirects(false);
				httpConnection.setConnectTimeout(REQUEST_TIMEOUT);
				httpConnection.setReadTimeout(REQUEST_TIMEOUT);
				httpConnection.setAllowUserInteraction(false);
				httpConnection.connect();
				
				// if we get a 200 response it's a playable URL
				// if we get anything else, it's not
				
				int httpResponse = httpConnection.getResponseCode();
				httpConnection.disconnect();
				
				// logger.debug("response for rhaplink {} was {}", link, httpResponse);
				
				if (httpResponse == HttpURLConnection.HTTP_OK) {
					logger.debug("got working rhaplink {}", link);
					found = true;
				}
			}
		} catch (MalformedURLException mue) {
			logger.error("malformed URL when trying to fetch rhapsody link: " + mue.getMessage(), mue);
		} catch (IOException ioe) {
			logger.warn("IO exception when trying to fetch rhapsody link: " + ioe.getMessage(), ioe);
		}
		return found;
	}

	@Override
	public CachedRhapsodyDownload queryExisting(String key) {
		try {
			Query q = em.createQuery("SELECT crd FROM CachedRhapsodyDownload crd WHERE crd.url = :url");
			q.setParameter("url", key);
			return (CachedRhapsodyDownload)(q.getSingleResult());
		} catch (NoResultException e) {
			//logger.debug("No cached rhaplink status for {}", rhaplink);
			return null;
		}
	}

	@Override
	public Boolean resultFromEntity(CachedRhapsodyDownload entity) {
		return entity.isActive();
	}

	@Override
	public CachedRhapsodyDownload entityFromResult(String key, Boolean result) {
		CachedRhapsodyDownload entity = new CachedRhapsodyDownload();
		entity.setUrl(key);
		entity.setActive(result);
		return entity;
	}

	@Override
	public void updateEntityFromResult(String key, Boolean result, CachedRhapsodyDownload entity) {
		entity.setUrl(key);
		entity.setActive(result);
	}

	 @Override
	public CachedRhapsodyDownload newNoResultsMarker(String key) {
		return CachedRhapsodyDownload.newNoResultsMarker();
	}
}
