package com.dumbhippo.server.impl;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.CurrentTrack;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class MusicSystemBean implements MusicSystem {

	@SuppressWarnings("unused")
	static private final Log logger = GlobalSetup.getLog(MusicSystemBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private IdentitySpider identitySpider;
	
	public Track getTrack(Map<String, String> properties) {
		
		final Track key = new Track(properties);
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<Track>() {
				
				public Track call() throws Exception {
					Query q;
					
					q = em.createQuery("from Track t where t.digest = :digest");
					q.setParameter("digest", key.getDigest());
					
					Track res;
					try {
						res = (Track) q.getSingleResult();
					} catch (EntityNotFoundException e) {
						res = key;
						em.persist(res);
					}
					
					return res;	
				}			
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
	}

	public void setCurrentTrack(final User user, final Track track) {
		try {
			runner.runTaskRetryingOnConstraintViolation(new Callable<CurrentTrack>() {
				
				public CurrentTrack call() throws Exception {
					Query q;
					
					q = em.createQuery("from CurrentTrack ct where ct.user = :user");
					q.setParameter("user", user);
					
					CurrentTrack res;
					try {
						res = (CurrentTrack) q.getSingleResult();
						res.setTrack(track);
						res.setLastUpdated(new Date());
					} catch (EntityNotFoundException e) {
						res = new CurrentTrack(user, track);
						res.setLastUpdated(new Date());
						em.persist(res);
					}
					
					return res;
				}
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			// not reached
		}
	}
	 
	public void setCurrentTrack(User user, Map<String,String> properties) {
		// empty properties means "not listening to any track" - we always
		// keep the latest track with content, we don't set CurrentTrack to null
		if (properties.size() == 0)
			return;
		
		Track track = getTrack(properties);
		setCurrentTrack(user, track);
	}
	
	public CurrentTrack getCurrentTrack(Viewpoint viewpoint, User user) throws NotFoundException {
		if (!identitySpider.isViewerFriendOf(viewpoint, user))
			throw new NotFoundException("Not allowed to see this user's current track");

		Query q;
		
		q = em.createQuery("from CurrentTrack ct where ct.user = :user");
		q.setParameter("user", user);
		
		CurrentTrack res;
		try {
			res = (CurrentTrack) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			res = null;
		}

		// note that getTrack() has the side effect of ensuring 
		// we load the track...
		if (res == null || res.getTrack() == null)
			throw new NotFoundException("User has no current track");
		else
			return res;
	}
}
