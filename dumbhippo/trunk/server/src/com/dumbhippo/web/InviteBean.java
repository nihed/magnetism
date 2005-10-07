package com.dumbhippo.web;

import com.dumbhippo.server.*;
import com.dumbhippo.*;
import java.util.*;
import javax.naming.*;
	
/**
 * InviteBean corresponds to the invite JSF page.
 * 
 * @author dff
 *
 */

public class InviteBean {
   private String fullName;
   private String email;

   // PROPERTY: fullName
   public String getFullName() { return fullName; }
   public void setFullName(String newValue) { fullName = newValue; }

   // PROPERTY: email
   public String getEmail() { return email; }
   public void setEmail(String newValue) { email = newValue; }
   
   // action handler for form submit
   public String doInvite() throws NamingException {
		   // TODO: create an invitation in database and get an authkey
		   // TODO: send out the email, etc
	
		   InitialContext ctx = new InitialContext();

		   // This is handy if you want to see what jboss is naming things
		   NamingEnumeration ne = ctx.list("");
		   System.out.println("naming information");
		   while (ne.hasMore()) { 
			   	NameClassPair p = (NameClassPair)ne.next();
			   	System.out.println(p);
		   }
		   

		   /*
		   // TODO: colin stuff to adapt / hook up
		  
		   InvitationSystem invitationSystem = 
			   	(InvitationSystem) ctx.lookup("com.dumbhippo.server.InvitationSystem");
		   IdentitySpider identitySpider = 
			   	(IdentitySpider) ctx.lookup("com.dumbhippo.server.IdentitySpider");

		   AccountSystem accounts = (AccountSystem) new InitialContext().lookup("com.dumbhippo.server.AccountSystem");
		   Resource inviterEmail = identitySpider.getEmail(request.getParameter("inviterEmail"));
		   HippoAccount acct = accounts.createAccountFromResource(inviterEmail);
		   Person inviter = acct.getOwner();
	
		   String email = request.getParameter("emailaddr");
		     
		   // FIXME need to validate email param != null and that it is valid rfc822
		   EmailResource res = identitySpider.getEmail(email);
		   Invitation invite = invitationSystem.createGetInvitation(inviter, res);
		   	   
		   */
		   
		   return "invitesent";
   }
}