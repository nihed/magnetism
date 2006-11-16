package com.dumbhippo.services;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

import com.dumbhippo.StringUtils;
import com.dumbhippo.services.FeedFetcher.FetchFailedException;
import com.sun.syndication.feed.module.mediarss.MediaEntryModule;
import com.sun.syndication.feed.module.mediarss.types.Metadata;
import com.sun.syndication.feed.module.mediarss.types.Thumbnail;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

public class YouTubeVideoFeedParser {
	
	private static URL getVideosFeedUrl(String username) {
		try {
			return new URL("http://www.youtube.com/rss/user/" + StringUtils.urlEncode(username) + "/videos.rss");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}	
	
	public static List<YouTubeVideo> getVideosForUser(String username, int count) {
		SyndFeed feed;		
		try {
			feed = FeedFetcher.getFeed(getVideosFeedUrl(username));
		} catch (FetchFailedException e) {
			return null;
		}
		List<YouTubeVideo> videos = new ArrayList<YouTubeVideo>();
		for (Object o : feed.getEntries()) {
			SyndEntry entry = (SyndEntry) o;
			MediaEntryModule module = (MediaEntryModule) entry.getModule("http://search.yahoo.com/mrss/");
			if (module == null)
				continue;
			Metadata meta = module.getMetadata();
			Thumbnail[] thumbnails = meta.getThumbnail();
			if (thumbnails.length == 0)
				continue;
			videos.add(new YouTubeVideo(entry.getTitle(), entry.getLink(), thumbnails[0].getUrl().toString()));
		}
		return videos;
	}
	
	static public final void main(String[] args) {
		org.apache.log4j.Logger log4jRoot = org.apache.log4j.Logger.getRootLogger();
		ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%d %-5p [%c] (%t): %m%n"));
		log4jRoot.addAppender(appender);
		log4jRoot.setLevel(Level.DEBUG);
		
		List<YouTubeVideo> videos = getVideosForUser("mitchnozka", 7);
		for (YouTubeVideo video : videos) {
			System.out.println("video: " + video.getThumbnailTitle() + " src: " + video.getThumbnailSrc() + " href: " + video.getThumbnailHref());
		}
	}
}
