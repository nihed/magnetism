package com.dumbhippo.server;

import com.dumbhippo.persistence.Storage;
import com.dumbhippo.persistence.Storage.SessionWrapper;

public class InvitationSystemBeanTest extends SpiderUsingTest {

	private InvitationSystemBean invite;
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
		SessionWrapper sess = Storage.getGlobalPerThreadSession();
		sess.beginTransaction();
		
		Person p1 = getTestPerson1();
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
		SessionWrapper sess = Storage.getGlobalPerThreadSession();
		sess.beginTransaction();

		Invitation i1 = getTestInvitation1();
		String authKey = i1.getAuthKey();
		
		sess.commitCloseBeginTransaction();
		
		Invitation i2 = invite.lookupInvitationByKey(authKey);
		assertEquals(i2.getAuthKey(), authKey);
		assertEquals(i2.getId(), i1.getId());
		assertEquals(i2.getInvitee(), i1.getInvitee());

		sess.commitTransaction();
	}

	/*
	 * Test method for 'com.dumbhippo.server.InvitationSystemBean.sendEmailNotification(IdentitySpider, Invitation, Person)'
	 */
	public void testSendEmailNotification() {
		SessionWrapper sess = Storage.getGlobalPerThreadSession();
		sess.beginTransaction();
		
		sess.commitCloseBeginTransaction();
		
		sess.commitTransaction();
	}

}
