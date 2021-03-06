package com.dumbhippo.server.impl;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.dm.ReadWriteSession;
import com.dumbhippo.persistence.EmailDetails;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.OnlineDesktopSystem;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.dm.UserDMO;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxCallable;
import com.dumbhippo.tx.TxUtils;

@Stateless
public class OnlineDesktopSystemBean implements OnlineDesktopSystem {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(OnlineDesktopSystemBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	private EmailDetails getEmailDetails(final EmailResource email) throws RetryException {
		return TxUtils.runNeedsRetry(new TxCallable<EmailDetails>() {
			public EmailDetails call() {
				Query q;

				q = em.createQuery("from EmailDetails e where e.id = :email");
				q.setParameter("email", email.getId());

				EmailDetails res;
				try {
					res = (EmailDetails) q.getSingleResult();
				} catch (NoResultException e) {
					res = new EmailDetails(email);
					em.persist(res);
				}

				return res;
			}
		});
	}
	
	public List<EmailResource> getGoogleEnabledEmails(Viewpoint viewpoint,
			User user) {
		if (!viewpoint.isOfUser(user)) {
			throw new RuntimeException("can only get your own enabled emails");
		}
		return TypeUtils.castList(EmailResource.class,
			em.createQuery("SELECT er FROM AccountClaim ac, EmailResource er, EmailDetails ed " +
			    		   "WHERE ac.owner = :user AND ac.resource.id = er.id AND er.id = ed.id AND ed.googleServicesEnabled = true")
			    		   .setParameter("user", user).getResultList());
	}

	public void setGoogleServicedEmail(Viewpoint viewpoint, User user, EmailResource email, boolean enabled) throws RetryException {
		if (!viewpoint.isOfUser(user))
			throw new RuntimeException("can only get your own enabled emails");
		if (email.getAccountClaim().getOwner() != user)
			throw new RuntimeException("can only set Google state for emails you own");
		EmailDetails ed = getEmailDetails(email);
		ed.setGoogleServicesEnabled(enabled);
		ReadWriteSession session = DataService.currentSessionRW();
		session.changed(UserDMO.class, user.getGuid(), "googleEnabledEmails");		
	} 
}
