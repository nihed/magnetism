package com.dumbhippo.server;

import com.dumbhippo.persistence.Storage;
import com.dumbhippo.persistence.Storage.SessionWrapper;

public class InvitationSystemBeanTest extends SpiderUsingTest {

	private InvitationSystemBean invite;
	
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
	
	/*
	 * Test method for 'com.dumbhippo.server.InvitationSystemBean.createGetInvitation(Person, Person)'
	 */
	public void testCreateGetInvitation() {
		SessionWrapper sess = Storage.getGlobalPerThreadSession();
		sess.beginTransaction();
		
		Person p1 = getTestPerson1();
		Resource invitee = getNoPersonEmail();
		
		Invitation i = invite.createGetInvitation(p1, invitee);
		assertNotNull(i);
		
		sess.commitCloseBeginTransaction();
		
		assertEquals(i.getInvitee().getGuid(), invitee.getGuid());
		assertEquals(i.getInviters().size(), 1);
		assertTrue(i.getInviters().contains(p1));

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
