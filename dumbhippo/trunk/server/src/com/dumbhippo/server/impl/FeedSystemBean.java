package com.dumbhippo.server.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.XmlMethodErrorCode;
import com.dumbhippo.server.XmlMethodException;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.FetcherException;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;
import com.sun.syndication.io.FeedException;

@Stateless
public class FeedSystemBean implements FeedSystem {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FeedSystemBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;	
	
	@EJB
	private IdentitySpider identitySpider;	

	private Feed lookupExistingFeed(LinkResource link) {
		try {
			Feed feed = (Feed)em.createQuery("SELECT f FROM Feed f WHERE f.link = :link")
			  .setParameter("link", link)
			  .getSingleResult();
			
			return feed;
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	private SyndFeed fetchFeedFromNet(LinkResource link) throws XmlMethodException {
		URL url;
		try {
			url = new URL(link.getUrl());
		} catch (MalformedURLException e) {
			throw new RuntimeException("getFeed passed malformed URL object");
		}
		
		FeedFetcher feedFetcher = new HttpURLFeedFetcher(null);
		try {
			return feedFetcher.retrieveFeed(url);
		} catch (IOException e) {
			throw new XmlMethodException(XmlMethodErrorCode.NETWORK_ERROR, "Network error fetching feed " + url);
		} catch (FetcherException e) {
			throw new XmlMethodException(XmlMethodErrorCode.NETWORK_ERROR, "Error requesting feed from server " + url);
		} catch (FeedException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Error parsing feed " + url);
		}
	}
	
	private void initializeFeedFromSyndFeed(Feed feed, SyndFeed syndFeed) {
		feed.setTitle(syndFeed.getTitle());
		
		Set<String> foundGuids = new HashSet<String>();
		
		for (Object o : syndFeed.getEntries()) {
			SyndEntry syndEntry = (SyndEntry)o;
			
			String guid = syndEntry.getUri();
			if (foundGuids.contains(guid))
				continue;
			
			URL entryUrl;
			try {
				entryUrl = new URL(syndEntry.getLink());
			} catch (MalformedURLException e) {
				continue;
			}
			
			if (!foundGuids.contains(guid)) {
				foundGuids.add(guid);
				
				FeedEntry entry = new FeedEntry(feed);
				entry.setEntryGuid(guid);
				entry.setTitle(syndEntry.getTitle());
				
				SyndContent content = syndEntry.getDescription();
				
				// FIXME: we need to extract text out of HTML here, and so forth
				entry.setDescription(content.getValue());
				
				Date publishedDate = syndEntry.getPublishedDate();
				if (publishedDate != null) {
					entry.setDate(syndEntry.getPublishedDate());
				} else {
					logger.warn("Failed to parse date in feed {}", feed.getLink().getUrl());
					entry.setDate(new Date()); // set to current time - then never overwrite it (important)
				}
				
				entry.setLink(identitySpider.getLink(entryUrl));
				entry.setCurrent(true);
				
				em.persist(entry);
			}
		}

		feed.setLastFetched(new Date());
		feed.setLastFetchSucceeded(true);
	}
	
	public Feed getFeed(final LinkResource link) throws XmlMethodException {
		Feed feed = lookupExistingFeed(link);
		if (feed != null)
			return feed;
		
		final SyndFeed syndFeed = fetchFeedFromNet(link);
		
		try {
			Feed detached = runner.runTaskRetryingOnConstraintViolation(new Callable<Feed>() {
				
				public Feed call() throws Exception {
					Feed newFeed = lookupExistingFeed(link);
					if (newFeed != null) // Someone else already looked it up and stored it
						return newFeed;
					
					// link is not part of the session, but only it's ID is needed for this
					newFeed = new Feed(link);
					em.persist(newFeed);
										
					initializeFeedFromSyndFeed(newFeed, syndFeed);
					
					return newFeed;
				}
			});
			
			return em.find(Feed.class, detached.getId());
			
		} catch (Exception e) {
			if (e instanceof XmlMethodException)
				throw (XmlMethodException) e;
			else
				throw new RuntimeException("Error initializing feed from download result " + link.getUrl(), e);
		}
	}

	public void updateFeed(Feed feed) {
	}

	public List<FeedEntry> getCurrentEntries(Feed feed) {
		List<FeedEntry> result = new ArrayList<FeedEntry>();
		
		for (FeedEntry entry : feed.getEntries()) {
			if (entry.isCurrent())
				result.add(entry);
		}
		
		Collections.sort(result, new Comparator<FeedEntry>() {
			public int compare(FeedEntry a, FeedEntry b) {
				return - a.getDate().compareTo(b.getDate());
			}
		});
		
		return result;
	}

}
