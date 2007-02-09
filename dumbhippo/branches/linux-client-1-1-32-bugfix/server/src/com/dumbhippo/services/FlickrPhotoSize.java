package com.dumbhippo.services;

public enum FlickrPhotoSize {
	// See http://www.flickr.com/services/api/misc.urls.html
	SMALL_SQUARE(75, 's'), // this is square, the others are rectangular
	THUMBNAIL(100, 't'),
	SMALL(240, 'm'),
	MEDIUM(500, '\0'), // omit the url code
	LARGE(1024, 'b'); // doesn't exist for all images, only big ones
	// you can also get "original" size but you have to know the original format
	
	private int pixels;
	private char urlCode;
	
	FlickrPhotoSize(int pixels, char urlCode) {
		this.pixels = pixels;
		this.urlCode = urlCode;
	}
	
	public int getPixels() {
		return pixels;
	}
	
	public char getUrlCode() {
		return urlCode;
	}
}
