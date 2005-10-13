package com.dumbhippo.server.client;

import javax.naming.NamingException;

import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.server.AbstractEjbLink;
import com.dumbhippo.server.AuthenticationSystemRemote;
import com.dumbhippo.server.IdentitySpiderRemote;
import com.dumbhippo.server.InvitationSystemRemote;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.TestGlueRemote;

/**
 * 
 * Class for connecting us up to the app server.
 * 
 * @author hp
 *
 */
public class EjbLink extends AbstractEjbLink {

	private IdentitySpiderRemote identitySpider;
	private AuthenticationSystemRemote authenticationSystem;
	private InvitationSystemRemote invitationSystem;
	private MessengerGlueRemote messengerGlue;
	private TestGlueRemote testGlue;
		
	private void loadBeans() throws NamingException {			
		// we construct these up front so the getters are threadsafe and don't throw NamingException
		identitySpider = nameLookup(IdentitySpiderRemote.class);
		authenticationSystem = nameLookup(AuthenticationSystemRemote.class);
		invitationSystem = nameLookup(InvitationSystemRemote.class);
		messengerGlue = nameLookup(MessengerGlueRemote.class);
		testGlue = nameLookup(TestGlueRemote.class);
	}
	
	public EjbLink() throws NamingException {
		super();
		loadBeans();
	}
	
	public EjbLink(boolean verbose) throws NamingException {
		super(verbose);
		loadBeans();
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
	
	public TestGlueRemote getTestGlue() {
		return testGlue;
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
