/*
 * Copyright 2005 Sean Owen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.planetj.taste.ejb;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.recommender.ItemFilter;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.List;

/**
 * <p>Recommender EJB bean implementation.</p>
 *
 * <p>This class exposes a subset of the {@link Recommender} API. In particular it
 * does not support {@link Recommender#getDataModel()}
 * since it doesn't make sense to access this via an EJB component.</p>
 *
 * @author Sean Owen
 */
public class RecommenderEJBBean implements SessionBean {

	private static final long serialVersionUID = 91665786278476333L;
	
	private Recommender recommender;

	public List<RecommendedItem> recommend(final Object userID, final int howMany) throws TasteException {
		return recommender.recommend(userID, howMany);
	}

	public List<RecommendedItem> recommend(final Object userID, final int howMany, final ItemFilter filter) 
		throws TasteException {
		return recommender.recommend(userID, howMany, filter);
	}

	public double estimatePreference(final Object userID, final Object itemID) throws TasteException {
		return recommender.estimatePreference(userID, itemID);
	}

	public void setPreference(final Object userID, final Object itemID, final double value) throws TasteException {
		recommender.setPreference(userID, itemID, value);
	}

	public void refresh() {
		recommender.refresh();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSessionContext(final SessionContext sessionContext) {
		// Do nothing
	}

	public void ejbCreate() throws CreateException {
		Context ctx = null;
		try {

			ctx = new InitialContext();
			final String recommenderClassName = (String) ctx.lookup("java:comp/env/recommender-class");
			if (recommenderClassName == null) {
				final String recommenderJNDIName = (String) ctx.lookup("java:comp/env/recommender-jndi-name");
				if (recommenderJNDIName == null) {
					throw new CreateException("recommender-class and recommender-jndi-name env-entry not defined");
				}
				recommender = (Recommender) ctx.lookup("java:comp/env/" + recommenderJNDIName);
			} else {
				recommender = Class.forName(recommenderClassName).asSubclass(Recommender.class).newInstance();
			}

		} catch (NamingException ne) {
			throw new CreateException(ne.toString());
		} catch (ClassNotFoundException cnfe) {
			throw new CreateException(cnfe.toString());
		} catch (InstantiationException ie) {
			throw new CreateException(ie.toString());
		} catch (IllegalAccessException iae) {
			throw new CreateException(iae.toString());
		} finally {
			if (ctx != null) {
				try {
					ctx.close();
				} catch (NamingException ne) {
					throw new CreateException(ne.toString());
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void ejbRemove() {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	public void ejbActivate() {
		// Do nothing: stateless session beans are not passivated/activated
	}

	/**
	 * {@inheritDoc}
	 */
	public void ejbPassivate() {
		// Do nothing: stateless session beans are not passivated/activated
	}

}
