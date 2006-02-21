package com.dumbhippo.server.impl;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Token;
import com.dumbhippo.server.TokenExpiredException;
import com.dumbhippo.server.TokenSystem;
import com.dumbhippo.server.TokenUnknownException;

@Stateless
public class TokenSystemBean implements TokenSystem {

	private static final Logger logger = GlobalSetup.getLogger(TokenSystemBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	public Token getTokenByKey(String authKey) throws TokenExpiredException, TokenUnknownException {
		Token ret;
		try {
			ret = (Token) em.createQuery(
				"SELECT t FROM Token t WHERE t.authKey = :key")
				.setParameter("key", authKey).getSingleResult();
		} catch (EntityNotFoundException e) {
			logger.debug("Token key {} not found in db", authKey);
			throw new TokenUnknownException(authKey, e);
		} catch (Exception e) {
			// FIXME !  needed because an org.hibernate. exception gets thrown
			// probably a jboss bug
			// hp: not sure this is a jboss bug - it happens if you throw an exception
			// from the entity beans somehow, which can be our fault
			logger.warn("Token key {} exception loading: {}", authKey, e.getMessage());
			throw new TokenUnknownException(authKey, e);
		}
		
		// if we ever care about deleted tokens on this level, could change
		// ret.isExpired() to !ret.isValid() and rename the exception appropriately
		// we probably should not remove tokens that were marked as deleted though
		if (ret != null && ret.isExpired()) {
			// em.remove(ret);	// FIXME is this a good idea? probably it should just be in a cron job
			throw new TokenExpiredException(ret.getClass()); 
		}
		
		assert ret != null;
		return ret;
	}
}
