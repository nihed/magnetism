package com.dumbhippo.server.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.YouTubeUpdateStatus;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.YouTubeUpdater;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YouTubeVideo;
import com.dumbhippo.services.caches.YouTubeVideosCache;

@Stateless
public class YouTubeUpdaterBean implements YouTubeUpdater {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(YouTubeUpdaterBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em; 	

	@EJB
	private YouTubeVideosCache videosCache;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private Notifier notifier;
	
	public YouTubeUpdateStatus getCachedStatus(User user) throws NotFoundException {
		ExternalAccount external = user.getAccount().getExternalAccount(ExternalAccountType.YOUTUBE);
		if (external == null)
			throw new NotFoundException("User has no YouTube external account: " + user);
		// sentiment and handle are checked in here
		return getCachedStatus(external);
	}
	
	public YouTubeUpdateStatus getCachedStatus(ExternalAccount external) throws NotFoundException {
		if (external.getAccountType() != ExternalAccountType.YOUTUBE)
			throw new RuntimeException("Invalid account type " + external);
		
		if (external.getHandle() == null || external.getSentiment() != Sentiment.LOVE)
			throw new NotFoundException("YouTube account has null handle or is unloved: " + external);
		return getCachedStatus(external.getHandle());
	}
	
	public YouTubeUpdateStatus getCachedStatus(String username) throws NotFoundException {
		Query q = em.createQuery("SELECT updateStatus FROM YouTubeUpdateStatus updateStatus " +
				"WHERE updateStatus.username = :username");
		q.setParameter("username", username);
		
		try {
			return (YouTubeUpdateStatus) q.getSingleResult();
		} catch (NoResultException e) {
			throw new NotFoundException("Have not cached a status for YouTube username: " + username);
		}
	}
	
	public Set<String> getActiveYouTubeUsers() {
		Query q = em.createQuery("SELECT ea.handle FROM ExternalAccount ea WHERE " + 
				" ea.accountType = " + ExternalAccountType.YOUTUBE.ordinal() + 
				" AND ea.sentiment = " + Sentiment.LOVE.ordinal() + 
				" AND ea.handle IS NOT NULL " + 
				" AND ea.account.disabled = false AND ea.account.adminDisabled = false");
		// we need a set because multiple accounts can have the same flickr account
		Set<String> results = new HashSet<String>();
		results.addAll(TypeUtils.castList(String.class, q.getResultList()));
		return results;
	}
	
	public Collection<User> getUsersWhoLoveYouTubeAccount(String username) {
		Query q = em.createQuery("SELECT user FROM User user WHERE EXISTS " + 
				" (SELECT ea from ExternalAccount ea WHERE " +
				"  ea.account.owner = user " +
				"  AND ea.accountType = " + ExternalAccountType.YOUTUBE.ordinal() + 
				"  AND ea.sentiment = " + Sentiment.LOVE.ordinal() +
				"  AND ea.handle IS NOT NULL " + 
				"  AND ea.account.disabled = false AND ea.account.adminDisabled = false)");
		return TypeUtils.castList(User.class, q.getResultList());
	}	
	
	// avoid log messages in here that will happen on every call, or it could get noisy
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void periodicUpdate(String username) {
		EJBUtil.assertNoTransaction();
		YouTubeUpdater proxy = EJBUtil.defaultLookup(YouTubeUpdater.class);
		
		videosCache.expireCache(username);
		List<YouTubeVideo> videos = videosCache.getSync(username);
		proxy.saveUpdatedStatus(username, videos);
	}
	
	//TODO: Identify the unique part of youtube urls instead of using the whole thing
	private String computeHash(List<YouTubeVideo> videos) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; ++i) {
			if (i >= videos.size())
				break;
			sb.append(videos.get(i).getThumbnailHref());
		}
		return sb.toString();
	}	

	public void saveUpdatedStatus(String username, List<YouTubeVideo> videos) {
		logger.debug("Saving new YouTube status for " + username + ": videos {}",
				videos);
		
		YouTubeUpdateStatus updateStatus;
		try {
			updateStatus = getCachedStatus(username);
		} catch (NotFoundException e) {
			updateStatus = new YouTubeUpdateStatus(username);
			em.persist(updateStatus);
		}

		String hash = computeHash(videos);
		
		if (!updateStatus.getVideoHash().equals(hash)) {
			logger.debug("Most recent videos changed '{}' -> '{}'",
					updateStatus.getVideoHash(), hash);
			updateStatus.setVideoHash(hash);
			notifier.onYouTubeRecentVideosChanged(username, videos);
		}
	}

	private boolean isLovedAndEnabled(ExternalAccount external) {
		return external.hasLovedAndEnabledType(ExternalAccountType.YOUTUBE) && 
			external.getHandle() != null;
	}	
	
	private void onExternalAccountChange(User user, ExternalAccount external) {
		if (!isLovedAndEnabled(external))
			return;

		final String username = external.getHandle();
		runner.runTaskOnTransactionCommit(new Runnable() {
			public void run() {
				YouTubeUpdater updater = EJBUtil.defaultLookup(YouTubeUpdater.class);
				updater.periodicUpdate(username);
			}
		});		
	}
	
	public void onExternalAccountCreated(User user, ExternalAccount external) {
		onExternalAccountChange(user, external);
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		onExternalAccountChange(user, external);		
	}
}
