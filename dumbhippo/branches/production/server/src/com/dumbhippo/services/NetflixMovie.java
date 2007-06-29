package com.dumbhippo.services;

public class NetflixMovie implements NetflixMovieView {
	
    private int priority;
	private String title;
	private String description;
	private String url;
	
	public NetflixMovie() {
		priority = -1;		
	}

	public NetflixMovie(int priority, String title, String url, String description) {
		this.priority = priority;
		this.title = title;
		this.url = url;
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
