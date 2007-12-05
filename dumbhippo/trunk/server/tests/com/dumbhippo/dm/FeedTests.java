package com.dumbhippo.dm;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.dm.TestBlogEntryDMO;
import com.dumbhippo.dm.dm.TestUserDMO;
import com.dumbhippo.dm.persistence.TestBlogEntry;
import com.dumbhippo.dm.persistence.TestUser;
import com.dumbhippo.identity20.Guid;

public class FeedTests extends AbstractFetchTests {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FeedTests.class);

	private static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
	
	private static Date parseDate(String str) {
		try {
			return DATE_FORMAT.parse(str);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Date DATE1 = parseDate("2006-06-23 16:01:32 -0400");
	private static Date DATE2 = parseDate("2006-11-14 15:31:52 -0400");
	private static Date DATE3 = parseDate("2007-03-23 09:44:04 -0400");
	
	public FeedTests() {
		super("feed-fetch-tests.xml");
	}

	private void createData(Guid bobId) {
		EntityManager em = support.beginTransaction();

		TestUser bob = new TestUser("Bob");
		bob.setId(bobId.toString());
		em.persist(bob);
		
		TestBlogEntry entry1 = new TestBlogEntry(bob, 1, DATE1);
		entry1.setTitle("My Life");
		em.persist(entry1);

		TestBlogEntry entry2 = new TestBlogEntry(bob, 2, DATE2);
		entry2.setTitle("Stupid Alligator Tricks");
		em.persist(entry2);
		
		em.getTransaction().commit();
	}
	
	private void addBlogEntry(Guid bobId) {
		EntityManager em = support.beginSessionRW(new TestViewpoint(bobId));

		TestUser bob = em.find(TestUser.class, bobId.toString());
		
		TestBlogEntry entry3 = new TestBlogEntry(bob, 3, DATE3);
		entry3.setTitle("My 9-fingered Life");
		em.persist(entry3);
		
		support.currentSessionRW().feedChanged(TestUserDMO.class, bob.getGuid(), "blogEntries", DATE3.getTime());
		
		em.getTransaction().commit();
	}
	
	public void testBasicFeed() throws Exception {
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		EntityManager em;
		ReadOnlySession session;
		
		/////////////////////////////////////////////////
		// Setup

		Guid bobId = Guid.createNew();
		
		createData(bobId);
		
		//////////////////////////////////////////////////
		
		em = support.beginSessionRO(viewpoint);
		session = support.currentSessionRO();
		
		TestUserDMO bobDMO = session.find(TestUserDMO.class, bobId);
		assertEquals("Bob", bobDMO.getName());
		
		DMFeed<TestBlogEntryDMO> blogEntries = bobDMO.getBlogEntries();
		
		Iterator<DMFeedItem<TestBlogEntryDMO>> iter;

		// Fetch just one item
		iter = blogEntries.iterator(0, 1, -1);
		assertEquals(iter.next().getValue().getTimestamp(), DATE2.getTime());
		assertFalse(iter.hasNext());
		
		// Fetch all the items, the first will be cached, the second newly fetched
		iter = blogEntries.iterator(0, 10, -1);
		assertEquals(iter.next().getValue().getTimestamp(), DATE2.getTime());
		assertEquals(iter.next().getValue().getTimestamp(), DATE1.getTime());
		assertFalse(iter.hasNext());

		em.getTransaction().commit();
		
		//////////////////////////////////////////////////
		
		// Try adding another entry, check if we see the proper new contents
		
		addBlogEntry(bobId);

		em = support.beginSessionRO(viewpoint);
		session = support.currentSessionRO();
		
		bobDMO = session.find(TestUserDMO.class, bobId);
		assertEquals("Bob", bobDMO.getName());
		
		blogEntries = bobDMO.getBlogEntries();
		
		iter = blogEntries.iterator(0, 10, -1);
		assertEquals(iter.next().getValue().getTimestamp(), DATE3.getTime());
		assertEquals(iter.next().getValue().getTimestamp(), DATE2.getTime());
		assertEquals(iter.next().getValue().getTimestamp(), DATE1.getTime());
		assertFalse(iter.hasNext());
		
		em.getTransaction().commit();
	}
	
	public void testFeedFetch() throws Exception {
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		TestDMClient client = new TestDMClient(support.getModel(), viewpoint.getViewerId());
		EntityManager em;
		ReadOnlySession session;
		
		/////////////////////////////////////////////////
		// Setup

		Guid bobId = Guid.createNew();
		
		createData(bobId);
		
		//////////////////////////////////////////////////
		
		em = support.beginSessionRO(client);
		session = support.currentSessionRO();
		
		TestUserDMO bobDMO = session.find(TestUserDMO.class, bobId);
		assertEquals("Bob", bobDMO.getName());
		
		// Fetch just one item (the defaultMaxFetch for this property)
		doFetchTest(Guid.class, TestUserDMO.class, bobDMO, "blogEntries title", "bobsFirstFeed",
					"bob", bobId.toString());
		
		// Fetch all available items
		doFetchTest(Guid.class, TestUserDMO.class, bobDMO, "blogEntries(max=10) title", "bobsOlderFeed",
					"bob", bobId.toString());
		
		// Fetching again with the same maximum should give us nothing. 
		doFetchTest(Guid.class, TestUserDMO.class, bobDMO, "blogEntries(max=10) title", "bobsAlreadyFetchedFeed",
					"bob", bobId.toString());

		em.getTransaction().commit();
		
		
		// Add an entry, see if we get the right notification
		
		addBlogEntry(bobId);

		support.getModel().waitForAllNotifications();
		
		assertNotNull(client.getLastNotification());
		
		logger.debug("Notification from addition of feed entry is {}", client.getLastNotification());
		
		FetchResult expected = getExpected("bobsFeedNotification", "bob", bobId.toString());
		client.getLastNotification().validateAgainst(expected);
	}
}
