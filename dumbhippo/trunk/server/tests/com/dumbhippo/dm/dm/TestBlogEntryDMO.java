package com.dumbhippo.dm.dm;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.persistence.TestBlogEntry;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/test/blogEntry", resourceBase="/o/test/blogEntry")
public abstract class TestBlogEntryDMO extends DMObject<TestBlogEntryKey> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(TestBlogEntryDMO.class);

	@Inject
	private EntityManager em;
	
	@Inject
	private DMSession session;
	
	private TestBlogEntry blogEntry;
	
	protected TestBlogEntryDMO(TestBlogEntryKey key) {
		super(key);
	}

	@Override
	protected void init() throws NotFoundException {
		Query q = em.createQuery(
				"SELECT entry from TestBlogEntry entry " +
				" WHERE entry.user.id = :userId " +
				"   AND entry.serial = :serial "
				);
		
		q.setParameter("userId", getKey().getUserId().toString());
		q.setParameter("serial", getKey().getSerial());
		
		blogEntry = (TestBlogEntry)q.getSingleResult();
	}
	
	@DMProperty(defaultInclude=true)
	public long getTimestamp() {
		return blogEntry.getTimestamp();
	}
	
	@DMProperty(defaultInclude=true)
	public String getTitle() {
		return blogEntry.getTitle();
	}
	
	@DMProperty
	public TestUserDMO getUser() {
		return session.findUnchecked(TestUserDMO.class, blogEntry.getUser().getGuid());
	}
}
