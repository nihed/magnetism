package com.dumbhippo.server.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.live.DesktopSettingChangedEvent;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.DesktopSetting;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.DesktopSettings;
import com.dumbhippo.server.TransactionRunner;

@Stateless
public class DesktopSettingsBean implements DesktopSettings {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(DesktopSettingsBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;
	
	public Map<String, String> getSettings(User user) {
		Query q = em.createQuery("SELECT ds FROM DesktopSetting ds WHERE ds.user = :user");
		q.setParameter("user", user);
		
		List<?> settings = q.getResultList();
		
		Map<String,String> map = new HashMap<String,String>();
		
		for (DesktopSetting ds : TypeUtils.castList(DesktopSetting.class, settings)) {
			map.put(ds.getKeyName(), ds.getValue());
		}
		
		return map;
	}

	// null value means unset
	public void setSetting(final User user, final String key, final String value) {
		runner.runTaskRetryingOnConstraintViolation(new Runnable() {

			public void run() {
				Query q = em.createQuery("SELECT ds FROM DesktopSetting ds WHERE ds.user = :user AND ds.keyName = :key");
				q.setParameter("user", user);
				q.setParameter("key", key);
				DesktopSetting ds;
				try {
					ds = (DesktopSetting) q.getSingleResult();
					if (value != null)
						ds.setValue(value);
					else
						em.remove(ds);
				} catch (NoResultException e) {
					if (value != null) {
						ds = new DesktopSetting(user, key, value);
						em.persist(ds);
					}
				}
				LiveState.getInstance().queueUpdate(new DesktopSettingChangedEvent(user.getGuid(), key, value));
			}
			
		});
	}

	public String getSetting(User user, String key) {
		Query q = em.createQuery("SELECT ds FROM DesktopSetting ds WHERE ds.user = :user AND ds.keyName = :key");
		q.setParameter("user", user);
		q.setParameter("key", key);
		DesktopSetting ds;
		try {
			ds = (DesktopSetting) q.getSingleResult();
			return ds.getValue();
		} catch (NoResultException e) {
			return null;
		}
	}
}
