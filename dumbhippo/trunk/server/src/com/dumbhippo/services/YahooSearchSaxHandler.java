package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.SongDownloadSource;
import com.dumbhippo.persistence.YahooSongDownloadResult;
import com.dumbhippo.persistence.YahooSongResult;

class YahooSearchSaxHandler extends EnumSaxHandler<YahooSearchSaxHandler.Element> {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooSearchSaxHandler.class);
	
	// The enum names should match the xml element names (including case)
	enum Element {
		// General
		ResultSet,
		Result,
		Name,
		Thumbnail,
			Url,
			Width,
			Height,
		RelatedArtists,
		Artist,
		PopularSongs,
		Song,
		YahooMusicPage,
		
		// Album search 
		Title,
		Publisher,
		ReleaseDate,
		RelatedAlbums,
		Album,
		
		// Song search
		Length,
		Track,
		
		// Download search
		Source,
		Format,
		Price,
		Channels,
		Restrictions,
		Quality,
		
		// Error
		Error,
		Message,
		
		IGNORED // an element we don't care about
	};	

	private class Result {
		private String id;
		private Map<Element,String> values;
		private String artistId;
		private String albumId;
		
		public Result(String id) {
			this.id = id;
			values = new EnumMap<Element,String>(Element.class);
		}
		
		public String getValue(Element e) {
			return values.get(e);
		}

		public int getValueInt(Element e) {
			String v = values.get(e);
			if (v == null)
				return -1;
			try {
				return Integer.parseInt(v);
			} catch (NumberFormatException e1) {
				return -1;
			}
		}
		
		public void setValue(Element e, String v) {
			values.put(e, v);
		}
		
		public String getId() {
			return id;
		}

		public String getAlbumId() {
			return albumId;
		}

		public void setAlbumId(String albumId) {
			this.albumId = albumId;
		}

		public String getArtistId() {
			return artistId;
		}

		public void setArtistId(String artistId) {
			this.artistId = artistId;
		}
	}
	
	private List<Result> results;
	private String errorMessage;
	
	YahooSearchSaxHandler() {
		super(Element.class, Element.IGNORED);
		results = new ArrayList<Result>();
	}

	private Result currentResult() {
		// note that unlike the other current() things, 
		// "results" is not a stack, just a list 
		if (results.size() > 0)
			return results.get(results.size() - 1);
		else
			return null;
	}
	
	
	@Override
	protected void openElement(Element c) throws SAXException {
		//logger.debug("opening element " + c);
		if (c == Element.Result) {
			String id = currentAttributes().getValue("id");
			Result result = new Result(id);
			results.add(result);
		}
	}
	
	@Override
	protected void closeElement(Element c) throws SAXException {
		/*
		logger.debug("closing element " + c);
		
		Attributes a = currentAttributes();
		for (int i = 0; i < a.getLength(); ++i) {
			logger.debug("qName = " + a.getQName(i) + " localname = " + a.getLocalName(i) + " value = " + a.getValue(i));
		}
		*/
		
		if (parent() == Element.Result) {
			currentResult().setValue(c, getCurrentContent());
			
			if (c == Element.Artist)
				currentResult().setArtistId(currentAttributes().getValue("id"));
			else if (c == Element.Album)
				currentResult().setAlbumId(currentAttributes().getValue("id"));	
		} else if (parent() == Element.Thumbnail) {
			if (c == Element.Url || c == Element.Width || c == Element.Height)
				currentResult().setValue(c, getCurrentContent());
		} else if (c == Element.Message && parent() == Element.Error) {
			errorMessage = getCurrentContent();
			logger.debug("Error reply from Yahoo: {}", errorMessage);
			throw new ServiceException(true, "Yahoo! search failed: " + errorMessage);	
		}
	}
	
	/*
	@Override 
	public void endDocument() throws SAXException {
		logger.debug("End of yahoo document");
	}
	*/
	
	private YahooSongResult songFromResult(Result r) {
		YahooSongResult song = new YahooSongResult();
		song.setLastUpdated(new Date());
		song.setSongId(r.getId());
		song.setAlbumId(r.getAlbumId());
		song.setArtistId(r.getArtistId());
		song.setPublisher(r.getValue(Element.Publisher));
		song.setReleaseDate(r.getValue(Element.ReleaseDate));
		song.setDuration(r.getValueInt(Element.Length));
		song.setTrackNumber(r.getValueInt(Element.Track));
		return song;		
	}
	
	public List<YahooSongResult> getBestSongs() {
		// FIXME this could doubtless be more sophisticated ...
		// right now it just hopes the first two results are the 
		// good ones. you sometimes need two different song IDs 
		// to get all the download urls we care about (iTunes 
		// in particular)
		if (results.isEmpty()) {
			logger.debug("No song results were parsed");
			return Collections.emptyList();
		}

		List<YahooSongResult> list = new ArrayList<YahooSongResult>();
		Result r = results.get(0);
		list.add(songFromResult(r));
		if (results.size() > 1) {
			r = results.get(1);
			// the rhapsody/yahoo/itunes/etc. good results 
			// tend to have these fields...
			if (r.getAlbumId() != null &&
					r.getValue(Element.ReleaseDate) != null &&
					r.getValue(Element.Publisher) != null && 
					!r.getId().equals(results.get(0).getId())) {
				list.add(songFromResult(r));
			}
		}
		return list;
	}
	
	private YahooSongDownloadResult downloadFromResult(Result r) {
		
		// need a known source and an url to be useful
		String sourceStr = r.getValue(Element.Source);
		SongDownloadSource source = SongDownloadSource.parseYahooSourceName(sourceStr);
		if (source == null) {
			//logger.debug("Ignoring download result from unknown source {}", sourceStr);
			return null;
		}
		String url = r.getValue(Element.Url);
		if (url == null) {
			//logger.debug("Ignoring download result with no url");
			return null;
		}
		
		YahooSongDownloadResult song = new YahooSongDownloadResult();
		song.setLastUpdated(new Date());
		song.setSource(source);
		song.setUrl(url);
		song.setFormats(r.getValue(Element.Format));
		song.setPrice(r.getValue(Element.Price));
		song.setRestrictions(r.getValue(Element.Restrictions));
		return song;		
	}
	
	public List<YahooSongDownloadResult> getBestDownloads() {
		if (results.isEmpty()) {
			logger.debug("No download results were parsed");
			return Collections.emptyList();
		}

		List<YahooSongDownloadResult> list = new ArrayList<YahooSongDownloadResult>();
		for (Result r : results) {
			YahooSongDownloadResult d = downloadFromResult(r);
			if (d != null)
				list.add(d);
		}
		logger.debug("Got {} download results: {}", list.size(), list);
		return list;		
	}
}
