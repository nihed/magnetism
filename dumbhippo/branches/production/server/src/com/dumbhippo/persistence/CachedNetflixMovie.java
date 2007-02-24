package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.services.NetflixMovie;
import com.dumbhippo.services.NetflixMovieView;
import com.dumbhippo.services.NetflixWebServices;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"netflixUserId", "priority"})})
public class CachedNetflixMovie extends DBUnique implements CachedListItem {
    private String netflixUserId;
    private int priority;
	private String title;
	private String description;
	private String url;
	private long lastUpdated;
	
	// for hibernate
	protected CachedNetflixMovie() {
		
	}
	
	public CachedNetflixMovie(String netflixUserId, int priority, String title, String description, String url) {
		this.netflixUserId = netflixUserId;
		this.priority = priority;
		this.title = title;
		this.description = description;
		this.url = url;
	}	
	
	static public CachedNetflixMovie newNoResultsMarker(String netflixUserId) {
		return new CachedNetflixMovie(netflixUserId, -1, "", "", "");
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return priority == -1;
	}
	
	public CachedNetflixMovie(String netflixUserId, NetflixMovieView movie) {
		this(netflixUserId, movie.getPriority(), movie.getTitle(), movie.getDescription(), movie.getUrl());
	}
	
	public NetflixMovieView toNetflixMovie() {
		NetflixMovie movie = new NetflixMovie();
		movie.setPriority(priority);
		movie.setTitle(title);
		movie.setDescription(description);
		movie.setUrl(url);
		return movie;
	}
	
	@Column(nullable=false, length=NetflixWebServices.MAX_NETFLIX_USER_ID_LENGTH)
	public String getNetflixUserId() {
		return netflixUserId;
	}
	public void setNetflixUserId(String netflixUserId) {
		this.netflixUserId = netflixUserId;
	}

	@Column(nullable=false)
	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	@Column(nullable=false)
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	@Column(nullable=false)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column(nullable=false, length=NetflixWebServices.MAX_NETFLIX_MOVIE_URL_LENGTH)
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	@Column(nullable=false)
	public Date getLastUpdated() {
		return new Date(lastUpdated);
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated.getTime();
	}	
	
	@Override
	public String toString() {
		if (isNoResultsMarker())
			return "{CachedNetflixMovie:NoResultsMarker}";
		else
			return "{netflixUserId=" + netflixUserId + " priority=" + priority + " title='" + title + "'}";
	}
	
}
