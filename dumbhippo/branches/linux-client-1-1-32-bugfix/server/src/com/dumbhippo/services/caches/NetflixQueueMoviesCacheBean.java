package com.dumbhippo.services.caches;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedNetflixMovie;
import com.dumbhippo.persistence.CachedNetflixMovies;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.NetflixMovieView;
import com.dumbhippo.services.NetflixMovies;
import com.dumbhippo.services.NetflixMoviesView;
import com.dumbhippo.services.NetflixWebServices;

//@Stateless // for now, these cache beans are our own special kind of bean and not EJBs due to a jboss bug
public class NetflixQueueMoviesCacheBean extends AbstractBasicCacheBean<String,NetflixMoviesView> implements
		NetflixQueueMoviesCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(NetflixQueueMoviesCacheBean.class);

	// set the expiration time to two weeks, so that we never update the queue while it is 
	// displayed with a given block, we will force an update when we want to have a current
	// queue to display
	static private final long NETFLIX_QUEUE_MOVIES_EXPIRATION = 1000 * 60 * 60 * 24 * 14;
	
	private BasicCacheStorage<String,NetflixMoviesView,CachedNetflixMovies> summaryStorage;
	private ListCacheStorage<String,NetflixMovieView,CachedNetflixMovie> movieListStorage;
	
	public NetflixQueueMoviesCacheBean() {
		super(Request.NETFLIX_QUEUE_MOVIES, NetflixQueueMoviesCache.class, NETFLIX_QUEUE_MOVIES_EXPIRATION);
	}

	@PostConstruct
	public void init() {
		BasicCacheStorageMapper<String,NetflixMoviesView,CachedNetflixMovies> summaryMapper =
			new BasicCacheStorageMapper<String,NetflixMoviesView,CachedNetflixMovies>() {

				public CachedNetflixMovies newNoResultsMarker(String key) {
					EJBUtil.assertHaveTransaction();
					
					return CachedNetflixMovies.newNoResultsMarker(key);
				}

				public CachedNetflixMovies queryExisting(String key) {
					EJBUtil.assertHaveTransaction();
					
					Query q = em.createQuery("SELECT movies FROM CachedNetflixMovies movies WHERE movies.netflixUserId = :netflixUserId");
					q.setParameter("netflixUserId", key);
					
					try {
						return (CachedNetflixMovies) q.getSingleResult();
					} catch (NoResultException e) {
						return null;
					}
				}

				public NetflixMoviesView resultFromEntity(CachedNetflixMovies entity) {
					return entity.toNetflixMovies();
				}

				public CachedNetflixMovies entityFromResult(String key, NetflixMoviesView result) {
					return new CachedNetflixMovies(key, result);
				}

				public void updateEntityFromResult(String key, NetflixMoviesView result, CachedNetflixMovies entity) {
					entity.update(result);
				}
			
		};
		
		ListCacheStorageMapper<String,NetflixMovieView,CachedNetflixMovie> movieListMapper =
			new ListCacheStorageMapper<String,NetflixMovieView,CachedNetflixMovie>() {
			
			public List<CachedNetflixMovie> queryExisting(String key) {
				Query q = em.createQuery("SELECT movie FROM CachedNetflixMovie movie WHERE movie.netflixUserId = :netflixUserId");
				q.setParameter("netflixUserId", key);
				
				List<CachedNetflixMovie> results = TypeUtils.castList(CachedNetflixMovie.class, q.getResultList());
				return results;
			}

			public void setAllLastUpdatedToZero(String key) {
				EJBUtil.prepareUpdate(em, CachedNetflixMovie.class);
				
				Query q = em.createQuery("UPDATE CachedNetflixMovie c" + 
						" SET c.lastUpdated = '1970-01-01 00:00:00' " + 
						" WHERE c.netflixUserId = :netflixUserId");
				q.setParameter("netflixUserId", key);
				int updated = q.executeUpdate();
				logger.debug("{} cached items expired", updated);
			}				

			public NetflixMovieView resultFromEntity(CachedNetflixMovie entity) {
				return entity.toNetflixMovie();
			}

			public CachedNetflixMovie entityFromResult(String key, NetflixMovieView result) {
				return new CachedNetflixMovie(key, result);
			}
			
			public CachedNetflixMovie newNoResultsMarker(String key) {
				return CachedNetflixMovie.newNoResultsMarker(key);
			}
		};
		
		summaryStorage = new BasicCacheStorage<String,NetflixMoviesView,CachedNetflixMovies>(em, getExpirationTime(), summaryMapper);
		movieListStorage = new ListCacheStorage<String,NetflixMovieView,CachedNetflixMovie>(em, getExpirationTime(), NetflixMovieView.class, movieListMapper);
	}	
	

	@Override
	protected NetflixMoviesView fetchFromNetImpl(String key) {
		NetflixMovies movies = NetflixWebServices.getQueuedMoviesForUser(key);
		return movies;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public NetflixMoviesView checkCache(String key) throws NotCachedException {
		NetflixMoviesView summary = summaryStorage.checkCache(key);
		List<? extends NetflixMovieView> movieList = movieListStorage.checkCache(key);
		summary.setMovies(movieList);
		return summary;
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public NetflixMoviesView saveInCacheInsideExistingTransaction(String key, NetflixMoviesView data, Date now, boolean refetchedWithoutCheckingCache) {		
        // since we always try to store the same number of movies from the queue, there is no quick way to tell if 
		// what we already have is likely to be current
		NetflixMoviesView summary = summaryStorage.saveInCacheInsideExistingTransaction(key, data, now, refetchedWithoutCheckingCache);
		if (summary != null) {
			List<? extends NetflixMovieView> movieList = movieListStorage.saveInCacheInsideExistingTransaction(key, data != null ? data.getMovies() : null, now, refetchedWithoutCheckingCache);
			summary.setMovies(movieList);
		}	
		return summary;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override
	public void expireCache(String key) {
		EJBUtil.assertHaveTransaction();
		
		summaryStorage.expireCache(key);
		movieListStorage.expireCache(key);
	}
}