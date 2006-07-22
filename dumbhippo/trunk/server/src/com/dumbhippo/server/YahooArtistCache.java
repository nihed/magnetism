package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.YahooArtistResult;

@Local
public interface YahooArtistCache {
	/*
	 * Returns a yahoo artist that matches an artist name and an artistId. Might return an artist with a different 
	 * name, if an artist with a different name matches a passed in artist id, or if multiple artist names match the
	 * same artist id, and we've been storing a different name mapped to the artistId that the passed in artist name
	 * also maps to. 
	 * 
	 * At least one parameter out of artist and artistId must not be null.
	 * 
	 * @param artist name of the artist
	 * @param artistId yahoo id for the artist
	 * @return YahooArtistResult that represents the artist
	 */
	public YahooArtistResult getYahooArtistResultSync(String artist, String artistId) throws NotFoundException;
}
