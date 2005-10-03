package com.dumbhippo.server;

import com.dumbhippo.persistence.Invitation;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.Storage.SessionWrapper;

public class InvitationSystemBeanTest extends SpiderUsingTest {

	private InvitationSystem invite;
	private Invitation testInvitation1;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		invite = new InvitationSystemBean();
	}

	@Override
	protected void tearDown() throws Exception {
		invite = null;
		super.tearDown();
	}
	
	protected Invitation getTestInvitation1() {
		if (testInvitation1 == null)
			testInvitation1 = invite.createGetInvitation(getTestPerson1(), getNoPersonEmail());
		return testInvitation1;
	}	
	
	/*
	 * Test method for 'com.dumbhippo.server.InvitationSystemBean.createGetInvitation(Person, Person)'
	 */
	public void testCreateGetInvitation() {
		SessionWrapper sess = getSession();
		sess.beginTransaction();
		
		Person p1 = getTestPerson1();
		getTestPerson2(); // Ensure it's created to better test db
		Resource invitee = getNoPersonEmail();
		
		Invitation i = getTestInvitation1();
		assertNotNull(i);
		
		sess.commitCloseBeginTransaction();
		
		assertEquals(i.getInvitee().getGuid(), invitee.getGuid());
		assertEquals(i.getInviters().size(), 1);
		assertTrue(i.getInviters().contains(p1));

		sess.commitTransaction();
	}

	public void testLookupInvitation() {
		SessionWrapper sess = getSession();
		sess.beginTransaction();

		Invitation i1 = getTestInvitation1();
		getTestPerson2(); // Ensure it's created to better test db		
		String authKey = i1.getAuthKey();
		
		sess.commitCloseBeginTransaction();
		
		Invitation i2 = invite.lookupInvitationByKey(authKey);
		assertEquals(i2.getAuthKey(), authKey);
		assertEquals(i2.getId(), i1.getId());
		assertEquals(i2.getInvitee().getGuid(), i1.getInvitee().getGuid());
		assertEquals(i2.getInvitee(), i1.getInvitee());

		sess.commitTransaction();
	}
	
	
	public void testInvitationFromTheMan() {
		SessionWrapper sess = getSession();
		sess.beginTransaction();
		
		Person man = spider.getTheMan();
		Resource invitee = getNoPersonEmail();
		Invitation i = invite.createGetInvitation(man, invitee);
		String authKey = i.getAuthKey();
		
		sess.commitCloseBeginTransaction();
		
		i = invite.lookupInvitationByKey(authKey);

		assertEquals(invitee, i.getInvitee());
		assertEquals(invitee.getHumanReadableString(), i.getInvitee().getHumanReadableString());
		assertEquals(1, i.getInviters().size());
		assertTrue(i.getInviters().contains(man));
	}
		
}
