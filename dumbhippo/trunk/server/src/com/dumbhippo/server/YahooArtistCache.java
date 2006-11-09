package com.dumbhippo.server;

import java.util.List;
import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.services.YahooArtistData;

/**
 * Conceptually this should be two different "web service cache beans," 
 * one that looks up artist id list by artist name, one that returns
 * the artist data given an artist id. However, since we let Yahoo!
 * do both of those conceptual operations in one request, we also 
 * keep the logic together here so we can sort it out.
 */
@Local
public interface YahooArtistCache extends AbstractCache<String,YahooArtistData> {

	// FIXME these "get by name" should really return a list and then
	// in the UI we should try to resolve the multiple search results
	// in some way... for now we pick a result at random if we get more 
	// than one. Another alternative is that the UI could merge all the
	// results somehow. For now we keep the list up to this method 
	// where we pick the one at random.
	public YahooArtistData getSyncByName(String artist); 
	
	public Future<YahooArtistData> getAsyncByName(String artist);	
	
	public List<YahooArtistData> checkCacheByName(String artist);
	
	public List<YahooArtistData> fetchFromNetByName(String artist);

	public List<YahooArtistData> saveInCacheByName(String artist, List<YahooArtistData> data);
}
