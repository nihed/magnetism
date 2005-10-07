package com.dumbhippo.web;

import com.dumbhippo.persistence.*;

/**
 * InviteBean corresponds to the verify account page, the page that
 * a user arrives at from their account confirmation email.
 * 
 * @author dff
 */

public class VerifyBean {
   private String authKey;
   private String invitedBy;	// TODO: should be a collection of some sort

   // PROPERTY: authKey
   public String getAuthKey() { return authKey; }
   public void setAuthKey(String newValue) { authKey = newValue; }
   
   // PROPERTY: invitedBy
   public String getInvitedBy() { return invitedBy; }
   public void setInvitedBy(String newValue) { invitedBy = newValue; }
   
   // called to verify user
   public String doVerify() {
	   try {
		   // TODO: look up the authKey in our database and claim the
		   //  resource or whatever
		   
		   /*
		   // TODO: stuff from colin, to be hooked up
		
		   InvitationSystem invitationSystem = 
			   	(InvitationSystem) ctx.lookup("com.dumbhippo.server.InvitationSystem");
		
		   Invitation invite = invitationSystem.lookupInvitationByKey(authKey);
		   HippoAccount account = invitationSystem.viewInvitation(invite);
		   Person invitee = account.getOwner();
		   PersonView viewedInvitee = spider.getSystemViewpoint(invitee);
		   
	       Iterator it = invite.getInviters();
	       while (it.hasNext()) {
	         Person inviter = it.next();
	         PersonView view = spider.getSystemViewpoint(inviter);
	         // add view.getHumanReadableName() to the list of invitees
	       }
		   */
		   
		   return "mainpage";
	   } catch (Exception e) {
		   // didn't work for some reason, reload this page and show some errors
		   //  or something
		   return null;
	   }	
   }
}
   