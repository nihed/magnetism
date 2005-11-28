package com.dumbhippo.server.impl;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.Token;
import com.dumbhippo.server.TokenExpiredException;
import com.dumbhippo.server.TokenSystem;
import com.dumbhippo.server.TokenUnknownException;

@Stateless
public class TokenSystemBean implements TokenSystem {

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	public Token getTokenByKey(String authKey) throws TokenExpiredException, TokenUnknownException {
		Token ret;
		try {
			ret = (Token) em.createQuery(
				"from Token as t where t.authKey = :key")
				.setParameter("key", authKey).getSingleResult();
		} catch (EntityNotFoundException e) {
			throw new TokenUnknownException(authKey, e);
		} catch (Exception e) { // FIXME !  needed because an org.hibernate. exception gets thrown
			                    // probably a jboss bug
			throw new TokenUnknownException(authKey, e);
		}
		
		if (ret != null && ret.isExpired()) {
			// em.remove(ret);	// FIXME is this a good idea? probably it should just be in a cron job
			throw new TokenExpiredException(ret.getClass()); 
		}
		
		assert ret != null;
		return ret;
	}
}
