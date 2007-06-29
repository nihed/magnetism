package com.dumbhippo.services;

import java.net.MalformedURLException;
import java.net.URL;

import com.dumbhippo.StringUtils;
import com.dumbhippo.services.FeedFetcher.FeedFetchResult;
import com.dumbhippo.services.FeedFetcher.FetchFailedException;
import com.sun.syndication.feed.synd.SyndEntry;

public class NetflixWebServices {

    // typical Netflix user id is 35 characters		
	public static final int MAX_NETFLIX_USER_ID_LENGTH = 50;
	
    // typical Netflix movie url is 63 characters
	public static final int MAX_NETFLIX_MOVIE_URL_LENGTH = 100;
	
	// the number of movies per user we want to store
	public static final int MOVIES_PER_USER = 5;
	
	private static URL getQueueFeedUrl(String userId) {
		try {
			return new URL("http://rss.netflix.com/QueueRSS?id=" + StringUtils.urlEncode(userId));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	// TODO: see if can take advantage of being able to check whether the feed was changed,
	// though with the current implementation of movie caching, we only call this method
	// if the movie queue feed is likely to have changed (there was a new movie shipped to us)
	public static NetflixMovies getQueuedMoviesForUser(String userId) {
		FeedFetchResult fetchResult;		
		try {
			fetchResult = FeedFetcher.getFeed(getQueueFeedUrl(userId));
		} catch (FetchFailedException e) {
			return null;
		}
		NetflixMovies movies = new NetflixMovies();
		for (Object o : fetchResult.getFeed().getEntries()) {
			SyndEntry entry = (SyndEntry) o;	
			int priority = getPriorityFromEntryTitle(entry.getTitle());
			String title = getTitleFromEntryTitle(entry.getTitle());
            String description = entry.getDescription().getValue().trim();
			movies.addMovie(new NetflixMovie(priority, title, entry.getLink(), description));
			if (movies.getTotal() >= MOVIES_PER_USER)
			    break;				
		}
		return movies;
	}
	
	private static int getPriorityFromEntryTitle(String entryTitle) {
		if (entryTitle.indexOf("-") < 0)
			return -1;
		
		try {
            int priority = new Integer(entryTitle.substring(0, entryTitle.indexOf("-")));
            return priority;
		} catch (NumberFormatException e) {
		    return -1;	
		}
	}
	
	private static String getTitleFromEntryTitle(String entryTitle) {
		if (entryTitle.indexOf("-") < 0)
			return entryTitle;
		
		if (entryTitle.indexOf("-") == entryTitle.length() - 1)
			return "";
		
		return entryTitle.substring(entryTitle.indexOf("-") + 1).trim();
	}	
}
