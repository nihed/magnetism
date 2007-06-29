package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.services.NetflixMovies;
import com.dumbhippo.services.NetflixMoviesView;
import com.dumbhippo.services.NetflixWebServices;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"netflixUserId"})})
public class CachedNetflixMovies extends DBUnique implements CachedItem {

	private static final long serialVersionUID = 1L;
	
	private String netflixUserId;
	private int totalCount;
	private long lastUpdated;
	
	protected CachedNetflixMovies() {
		
	}
	
	public CachedNetflixMovies(String netflixUserId, int totalCount) {
		this.netflixUserId = netflixUserId;
		this.totalCount = totalCount;
	}
	
	public CachedNetflixMovies(String netflixUserId, NetflixMoviesView view) {
		this.netflixUserId = netflixUserId;
		update(view);
	}
	
	static public CachedNetflixMovies newNoResultsMarker(String netflixUserId) {
		return new CachedNetflixMovies(netflixUserId, -1);
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return totalCount < 0;
	}	

	public void update(NetflixMoviesView movies) {
		if (movies == null)
			totalCount = -1; // no results marker
		else
			setTotalCount(movies.getTotal());
	}
	
	public NetflixMoviesView toNetflixMovies() {
		NetflixMovies netflixMovies = new NetflixMovies();
		netflixMovies.setTotal(totalCount);
		return netflixMovies;
	}
	
	@Column(nullable=false, length=NetflixWebServices.MAX_NETFLIX_USER_ID_LENGTH)
	public String getNetflixUserId() {
		return netflixUserId;
	}
	public void setNetflixUserId(String netflixUserId) {
		this.netflixUserId = netflixUserId;
	}
	
	@Column(nullable=false)
	public int getTotalCount() {
		return totalCount;
	}
	
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
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
			return "{netflixUserId=" + netflixUserId + "}";
	}
}