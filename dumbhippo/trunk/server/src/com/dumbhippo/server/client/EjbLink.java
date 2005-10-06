package com.dumbhippo.server.client;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.server.AuthenticationSystemRemote;
import com.dumbhippo.server.IdentitySpiderRemote;
import com.dumbhippo.server.InvitationSystemRemote;
import com.dumbhippo.server.MessengerGlueRemote;

/**
 * 
 * Class for connecting us up to the app server.
 * 
 * @author hp
 *
 */
public class EjbLink {
	private InitialContext namingContext;
	private IdentitySpiderRemote identitySpider;
	private AuthenticationSystemRemote authenticationSystem;
	private InvitationSystemRemote invitationSystem;
	private MessengerGlueRemote messengerGlue;
	
	private Object nameLookup(Class clazz) throws NamingException {
		String name = clazz.getPackage().getName() + "." + clazz.getSimpleName();
		System.out.println("Looking up '" + name + "'");
		return namingContext.lookup(name);
	}
	
	public EjbLink() throws NamingException {
		namingContext = new InitialContext();
		
		// we construct these up front so the getters are threadsafe and don't throw NamingException
		identitySpider = (IdentitySpiderRemote) nameLookup(IdentitySpiderRemote.class);
		authenticationSystem = (AuthenticationSystemRemote) nameLookup(AuthenticationSystemRemote.class);
		invitationSystem = (InvitationSystemRemote) nameLookup(InvitationSystemRemote.class);
		messengerGlue = (MessengerGlueRemote) nameLookup(MessengerGlueRemote.class);
	}
	
	public IdentitySpiderRemote getIdentitySpider() {
		return identitySpider;
	}
	
	public AuthenticationSystemRemote getAuthenticationSystem() {
		return authenticationSystem;
	}
	
	public InvitationSystemRemote getInvitationSystem() {
		return invitationSystem;
	}
	
	public MessengerGlueRemote getMessengerGlue() {
		return messengerGlue;
	}
	
	public static void main(String[] args) {
		EjbLink link = null;
		
		try {
			link = new EjbLink();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Some JNDI lookup error... ouch");
			System.exit(1);
		}
		
		IdentitySpiderRemote spider = link.getIdentitySpider();
		EmailResource email = spider.getEmail("foo@example.com");
		System.out.println("Email email = " + email.getEmail() + " guid = " + email.getGuid());
	}
}
