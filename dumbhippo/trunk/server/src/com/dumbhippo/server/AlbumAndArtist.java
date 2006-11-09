package com.dumbhippo.server;

/** Immutable object representing album and artist pair, used to pass 
 * one arg instead of two to certain  methods
 * 
 * @author Havoc Pennington
 */
public class AlbumAndArtist {
	private String album;
	private String artist;

	public AlbumAndArtist(String album, String artist) {
		this.album = album;
		this.artist = artist;
	}
	public String getAlbum() {
		return album;
	}

	public String getArtist() {
		return artist;
	}

	@Override
	public String toString() {
		return "{album='" + album + "' artist='" + artist + "'}";
	}
	
	static boolean compareStrings(String s1, String s2) {
		if (s1 == s2) // handles two null
			return true;
		if (s1 != null && s2 == null)
			return false;
		if (s1 == null && s2 != null)
			return false;
		return s1.equals(s2);
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof AlbumAndArtist))
			return false;
		AlbumAndArtist otherAA = (AlbumAndArtist) other;
		return compareStrings(this.artist, otherAA.artist) && 
		compareStrings(this.album, otherAA.album);
	}
	
	@Override
	public int hashCode() {
		int code = 17;
		if (this.album != null)
			code += this.album.hashCode();
		if (this.artist != null)
			code += this.artist.hashCode();
		return code;
	}
}
