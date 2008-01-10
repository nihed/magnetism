package com.dumbhippo.services;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.services.FeedFetcher.FeedFetchResult;
import com.dumbhippo.services.FeedFetcher.FetchFailedException;
import com.sun.syndication.feed.module.mediarss.MediaEntryModule;
import com.sun.syndication.feed.module.mediarss.types.MediaGroup;
import com.sun.syndication.feed.module.mediarss.types.Metadata;
import com.sun.syndication.feed.module.mediarss.types.Thumbnail;
import com.sun.syndication.feed.synd.SyndEntry;

public class PicasaAlbumFeedParser {
	
	static private final Logger logger = GlobalSetup.getLogger(PicasaAlbumFeedParser.class);
	
	private static URL getAlbumFeedUrl(String username) {
		try {
			return new URL("http://picasaweb.google.com/data/feed/base/user/" + StringUtils.urlEncode(username) + "?kind=album&alt=rss&hl=en_US&access=public");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}	
	
	public static List<PicasaAlbum> getAlbumsForUser(String username, int count) {
		FeedFetchResult fetchResult;		
		try {
			fetchResult = FeedFetcher.getFeed(getAlbumFeedUrl(username));
		} catch (FetchFailedException e) {
			return null;
		}
		List<PicasaAlbum> albums = new ArrayList<PicasaAlbum>();
		for (Object o : fetchResult.getFeed().getEntries()) {
			SyndEntry entry = (SyndEntry) o;
			MediaEntryModule module = (MediaEntryModule) entry.getModule("http://search.yahoo.com/mrss/");
			if (module == null) {
				logger.debug("no MediaEntryModule in picasa feed entry");
				continue;
			}
			// we're expecting one "group" per entry (each entry is an album) and then one thumbnail for the group
			MediaGroup[] groups = module.getMediaGroups();
			if (groups.length == 0) {
				logger.debug("no groups in Picasa album feed entry");
				continue;
			}			
			Metadata meta = groups[0].getMetadata();
			Thumbnail[] thumbnails = meta.getThumbnail();
			if (thumbnails.length == 0) {
				logger.debug("no thumbnails in Picasa album feed entry");
				continue;
			}
			albums.add(new PicasaAlbum(entry.getTitle(), entry.getLink(), thumbnails[0].getUrl().toString()));
		}
		return albums;
	}
	
	static public final void main(String[] args) {
		org.apache.log4j.Logger log4jRoot = org.apache.log4j.Logger.getRootLogger();
		ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%d %-5p [%c] (%t): %m%n"));
		log4jRoot.addAppender(appender);
		log4jRoot.setLevel(Level.DEBUG);
		
		List<PicasaAlbum> albums = getAlbumsForUser("havoc.pennington", 7);
		for (PicasaAlbum album : albums) {
			System.out.println("album: " + album.getThumbnailTitle() + " src: " + album.getThumbnailSrc() + " href: " + album.getThumbnailHref());
		}
		System.out.println(albums.size() + " albums downloaded");
	}
}
