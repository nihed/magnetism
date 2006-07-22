package com.dumbhippo.server.impl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.RhapLink;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.RhapsodyDownloadCache;
import com.dumbhippo.server.TransactionRunner;

@BanFromWebTier
@Stateless
public class RhapsodyDownloadCacheBean extends AbstractCacheBean implements
		RhapsodyDownloadCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(RhapsodyDownloadCacheBean.class);

	// 14 days
	static private final int RHAPLINK_EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24 * 14;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;

	private RhapLink rhapLinkQuery(String rhaplink) {
		try {
			Query q = em.createQuery("FROM RhapLink rhaplink WHERE rhaplink.url = :rhaplink");
			q.setParameter("rhaplink", rhaplink);
			return (RhapLink)(q.getSingleResult());
		} catch (EntityNotFoundException e) {
			//logger.debug("No cached rhaplink status for {}", rhaplink);
			return null;
		}
	}
	
	public String getRhapsodyDownloadUrl(String artistName, String albumName, int trackNumber) {
		if ((trackNumber>0) && (artistName != null) && (albumName != null)) {
			// Try to concoct a Rhapsody friendly URL; see:
			//  http://rws-blog.rhapsody.com/rhapsody_web_services/2006/04/new_album_urls.html
			final String rhaplink = "http://play.rhapsody.com/" + rhapString(artistName) + "/" + rhapString(albumName) + "/track-" + trackNumber;
			
			boolean rhapLinkActive = false;
			RhapLink oldRhapLink = rhapLinkQuery(rhaplink);
			
			final long now = System.currentTimeMillis();
			if ((oldRhapLink == null) || ((oldRhapLink.getLastUpdated() + RHAPLINK_EXPIRATION_TIMEOUT) < now)) {
				logger.debug("Unknown or outdated Rhapsody link; testing status for rhaplink {}", rhaplink);
				
				try {
					URL u = new URL(rhaplink);
					URLConnection connection = u.openConnection();
					if (!(connection instanceof HttpURLConnection)) {
						logger.warn("Got a weird connection of type {} for URL {}; skipping", connection.getClass().getName(), u);
					} else {
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
						
						logger.debug("response for rhaplink {} was {}", rhaplink, httpResponse);
						
						if (httpResponse == HttpURLConnection.HTTP_OK) {
							logger.debug("got working rhaplink {}", rhaplink);
							rhapLinkActive = true;
						}
					}
				} catch (MalformedURLException mue) {
					logger.warn("malformed URL when trying to fetch rhapsody link: " + mue.getMessage(), mue);
				} catch (IOException ioe) {
					logger.warn("IO exception when trying to fetch rhapsody link: " + ioe.getMessage(), ioe);
				}
				
				final boolean isActive = rhapLinkActive;
				
				// need to retry on constraint violation to deal with race where multiple TrackViews are
				// being updated in parallel
				try {
					runner.runTaskRetryingOnConstraintViolation(new Callable<RhapLink>() {
						
						public RhapLink call() {
							
							RhapLink rhapLink = rhapLinkQuery(rhaplink);
							if (rhapLink == null) {
								rhapLink = new RhapLink();
								rhapLink.setUrl(rhaplink);
								rhapLink.setActive(isActive);
								rhapLink.setLastUpdated(now);
								em.persist(rhapLink);
							} else {
								rhapLink.setActive(isActive);
								rhapLink.setLastUpdated(now);
							}
							return rhapLink;
						}
						
					});
				} catch (Exception e) {
					logger.warn("Exception updating RhapLink entity in getRhapsodyDownloadUrl", e);
				}
				
			} else {
				// we found a rhaplink result that wasn't outdated, so return it
				rhapLinkActive = oldRhapLink.isActive();
				//logger.debug("cached rhaplink status for {} was {}", rhaplink, rhapLinkActive);
			}
		
			if (rhapLinkActive) {
				return rhaplink;
			} else {
				return null;
			}
		}
		return null;
	}
	
	/*
	 * Mangle a string to work in a Rhapsody friendly URL - remove all 
	 * punctuation and whitespace and lowercase it
	 */
	private static String rhapString(String s) {
		// strip all non-word characters, e.g. other than [a-zA-Z0-9]
		// TODO: figure out how to deal with broader character set, if Rhapsody even supports that?
		return s.replaceAll("[^a-zA-Z0-9]","").toLowerCase();
	}
}
