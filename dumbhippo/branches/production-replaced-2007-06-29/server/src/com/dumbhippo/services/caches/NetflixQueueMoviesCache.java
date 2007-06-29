package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.NetflixMoviesView;

@Local
public interface NetflixQueueMoviesCache extends Cache<String,NetflixMoviesView> {

}
