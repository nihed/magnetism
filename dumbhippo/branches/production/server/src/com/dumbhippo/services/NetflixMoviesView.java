package com.dumbhippo.services;

import java.util.List;

// currently, this is only used for the Netflix Queue,
// but we could possibly use it for movies at home, and other
// lists available to us
public interface NetflixMoviesView {

	public List<? extends NetflixMovieView> getMovies();

	public int getTotal();

	public void setMovies(List<? extends NetflixMovieView> movies);
}