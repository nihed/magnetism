package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.List;

public final class NetflixMovies implements NetflixMoviesView {
	
	private int total;
	private List<NetflixMovieView> movies;
	
	public NetflixMovies() {
		total = -1;
		movies = new ArrayList<NetflixMovieView>();
	}
	
	public List<? extends NetflixMovieView> getMovies() {
		return movies;
	}
	
	void addMovie(NetflixMovie movie) {
		movies.add(movie);
		// clear the stored total
		total = -1;
	}

	// when we get movies from the web service we just count them to get the total, 
	// but when restoring from cache we potentially have the total stored
	public int getTotal() {
		if (total >= 0)
			return total;
		else
			return movies.size();
	}
	
	public void setTotal(int total) {
		this.total = total;
	}
	
	public void setMovies(List<? extends NetflixMovieView> movies) {
		this.movies.clear();
		this.movies.addAll(movies);
		// clear the stored total
		total = -1;
	} 
	
	@Override
	public String toString() {
		return "{NetflixMovies count=" + getTotal() + "}";
	}
}