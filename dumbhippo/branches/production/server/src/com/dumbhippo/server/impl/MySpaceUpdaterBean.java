package com.dumbhippo.server.impl;

import java.util.List;
import java.util.concurrent.Callable;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.PollingTaskEntry;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.polling.PollResult;
import com.dumbhippo.polling.PollingTask;
import com.dumbhippo.polling.PollingTaskFamily;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.MySpaceUpdater;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PollingTaskPersistence;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.MySpaceScraper;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxUtils;

@Stateless
public class MySpaceUpdaterBean implements MySpaceUpdater {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(MySpaceUpdaterBean.class);	
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private PollingTaskPersistence pollingPersistence;
	
	@EJB
	private FeedSystem feedSystem;
	
	private static class MySpaceTaskFamily implements PollingTaskFamily {

		public long getDefaultPeriodicitySeconds() {
			return  60 * 60; // 1 hour
		}

		public String getName() {
			return PollingTaskFamilyType.MYSPACE.name();
		}
		
		public long rescheduleSeconds(long suggestedSeconds) {
			return suggestedSeconds;
		}
	}
	
	private static PollingTaskFamily family = new MySpaceTaskFamily();
	
	private static class MySpaceTask extends PollingTask {
		private ExternalAccount external; // detached
		
		public MySpaceTask(ExternalAccount external) {
			this.external = external;
		}

		@Override
		protected PollResult execute() throws Exception {			
			try {
			    TxUtils.runInTransaction(new Callable<Object>() {
					public Object call() throws XmlMethodException, RetryException {
						final MySpaceUpdater mySpaceUpdater = EJBUtil.defaultLookup(MySpaceUpdater.class);
						mySpaceUpdater.createFeedForMySpaceBlog(external);
						return null;
					}
				});
				return new PollResult(true, true);
			} catch (XmlMethodException e) {
				// nothing to do, MySpace account is probably still private
			}				
			return new PollResult(false, false);
		}

		@Override
		public PollingTaskFamily getFamily() {
			return family;
		}

		@Override
		public String getIdentifier() {
			return String.valueOf(external.getId());
		}
	}

	public PollingTask loadTask(PollingTaskEntry entry) throws NotFoundException {
		String externalAccountId = entry.getTaskId();
		ExternalAccount external = em.find(ExternalAccount.class, Long.parseLong(externalAccountId));
		return new MySpaceTask(external);
	}

	public void createFeedForMySpaceBlog(ExternalAccount external) throws XmlMethodException, RetryException {
	    // the external account passed in here is detached
		// external.getExtra() for MySpace is friendId
		Feed feed = feedSystem.scrapeFeedFromUrl(MySpaceScraper.getBlogURLFromFriendId(external.getExtra()));
		EJBUtil.forceInitialization(feed.getAccounts());
		ExternalAccount attachedExternal = em.find(ExternalAccount.class, external.getId());
		attachedExternal.setFeed(feed);
		feed.getAccounts().add(attachedExternal);
	}
	
	public void migrateTasks() {
		// get all loved MySpace accounts with a NULL feed
		Query q = em.createQuery("FROM ExternalAccount ea WHERE " +
				"  ea.accountType = " + ExternalAccountType.MYSPACE.ordinal() + 
				"  AND ea.sentiment = " + Sentiment.LOVE.ordinal() + 
				"  AND ea.feed IS NULL");
		List<ExternalAccount> mySpaceAccounts = TypeUtils.castList(ExternalAccount.class, q.getResultList());
		logger.debug("Creating polling tasks for {} MySpace accounts with no feeds", mySpaceAccounts.size());
	
		for (ExternalAccount external : mySpaceAccounts) {
			pollingPersistence.createTaskIdempotent(PollingTaskFamilyType.MYSPACE, String.valueOf(external.getId()));
		}
	}
}
