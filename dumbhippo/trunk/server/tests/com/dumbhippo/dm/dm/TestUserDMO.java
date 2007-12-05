package com.dumbhippo.dm.dm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.dm.DMFeed;
import com.dumbhippo.dm.DMFeedItem;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.persistence.TestBlogEntry;
import com.dumbhippo.dm.persistence.TestGroupMember;
import com.dumbhippo.dm.persistence.TestUser;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/test/user", resourceBase="/o/test/user")
public abstract class TestUserDMO extends DMObject<Guid> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(TestUserDMO.class);

	@Inject
	EntityManager em;
	
	@Inject
	DMSession session;

	TestUser user;
	
	protected TestUserDMO(Guid key) {
		super(key);
	}

	@Override
	protected void init() throws NotFoundException {
		user = em.find(TestUser.class, getKey().toString());
		if (user == null)
			throw new NotFoundException("No such user");
	}
	
	@DMProperty(defaultInclude=true)
	public String getName() {
		return user.getName();
	}
	
	@DMProperty
	@DMFilter("!viewer.isEnemy(this)")
	public Set<TestGroupDMO> getGroups() {
		Set<TestGroupDMO> result = new HashSet<TestGroupDMO>();
		
		for (TestGroupMember groupMember : user.getGroupMembers()) {
			if (!groupMember.isRemoved())
				result.add(session.findUnchecked(TestGroupDMO.class, groupMember.getGroup().getGuid()));
		}
		
		return result;
	}
	
	// The small defaultMaxFetch here is because we always fetch *at least* the defaultMaxFetch
	// so a larger one makes testing max= in fetch strings hard.
	@DMProperty(defaultMaxFetch=1)
	public DMFeed<TestBlogEntryDMO> getBlogEntries() {
		return new BlogEntryFeed();
	}
	
	private class BlogEntryFeed implements DMFeed<TestBlogEntryDMO> {
		public Iterator<DMFeedItem<TestBlogEntryDMO>> iterator(int start, int max, long minTimestamp) {
			logger.debug("Querying blog entries from database, start={}, max={}, minTimestamp={}",
					     new Object[] { start, max, minTimestamp });
			
			Query q = em.createQuery(
					"SELECT entry from TestBlogEntry entry " +
					" WHERE entry.user = :user " +
					"   AND entry.timestamp >= :minTimestamp " +
					" ORDER BY entry.timestamp DESC"
					);

			q.setParameter("user", user);
			q.setParameter("minTimestamp", minTimestamp);
			
			q.setFirstResult(start);
			q.setMaxResults(max);
			
			List<DMFeedItem<TestBlogEntryDMO>> items = new ArrayList<DMFeedItem<TestBlogEntryDMO>>(); 
			for (TestBlogEntry entry : TypeUtils.castList(TestBlogEntry.class, q.getResultList())) {
				TestBlogEntryDMO entryDMO = session.findUnchecked(TestBlogEntryDMO.class, new TestBlogEntryKey(entry));
				items.add(new DMFeedItem<TestBlogEntryDMO>(entryDMO, entry.getTimestamp()));
			}

			return items.iterator();
		}
	}
}
