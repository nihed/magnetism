package com.dumbhippo.server.impl;

import java.util.Collections;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jboss.annotation.ejb.Service;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.SubscriptionStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.XmppResource;
import com.dumbhippo.persistence.XmppSubscription;
import com.dumbhippo.server.XmppMessageSender;
import com.dumbhippo.server.XmppMessageSenderProvider;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxRunnable;
import com.dumbhippo.tx.TxUtils;

@Service
public class XmppMessageSenderBean implements XmppMessageSender {
	XmppMessageSenderProvider provider;

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	public void sendLocalMessage(Guid to, String payload) {
		sendLocalMessage(Collections.singleton(to), payload);
	}

	public void sendLocalMessage(Set<Guid> to, String payload) {
		if (provider != null)
			provider.sendMessage(to, payload);
	}

	public void sendNewPostMessage(User recipient, Post post) {
		if (provider != null)
			provider.sendNewPostMessage(recipient, post);
	}
	
	// We do sendAdminMessage and sendAdminPresence onCommit because we have concrete
	// cases where retrying transactions will cause duplicate messages. There really
	// isn't any conceptual difference between these and the methods above, however.

	public void sendAdminMessage(final String to, final String from, final String body, final String htmlBody) {
		if (provider != null)
			TxUtils.runOnCommit(new Runnable() {
				public void run() {
					provider.sendAdminMessage(to, from, body, htmlBody);
				}
			});
	}

	public void sendAdminPresence(final String to, final String from, final String type) {
		if (provider != null)
			TxUtils.runOnCommit(new Runnable() {
				public void run() {
					provider.sendAdminPresence(to, from, type);
				}
			});
	}

	public void setProvider(XmppMessageSenderProvider provider) {
		this.provider = provider;
	}

	private XmppSubscription getSubscription(String localJid, XmppResource remoteResource) throws NoResultException {
		Query q = em.createQuery("SELECT s FROM XmppSubscription s WHERE s.localJid = :local AND s.remoteResource = :remote");
		q.setParameter("local", localJid);
		q.setParameter("remote", remoteResource);
		
		return (XmppSubscription)q.getSingleResult();
	}
	
	public SubscriptionStatus getSubscriptionStatus(String localJid, XmppResource remoteResource) {
		try {
			XmppSubscription subscription = getSubscription(localJid, remoteResource);
			return subscription.getStatus();
		} catch (NoResultException e) {
			return SubscriptionStatus.NONE;
		}
	}
	
	public void setSubscriptionStatus(final String localJid, final XmppResource remoteResource, final SubscriptionStatus status) throws RetryException {
		TxUtils.runNeedsRetry(new TxRunnable() {
			public void run() {
				XmppSubscription subscription;
				
				try {
					subscription = getSubscription(localJid, remoteResource);
				} catch (NoResultException e) {
					subscription = new XmppSubscription(localJid, remoteResource);
					em.persist(subscription);
				}
	
				subscription.setStatus(status);
			}
		});
	}
}
