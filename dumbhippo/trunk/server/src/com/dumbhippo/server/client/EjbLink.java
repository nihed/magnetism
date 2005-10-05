package com.dumbhippo.server.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.dumbhippo.server.AuthenticationSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;

/**
 * 
 * Class for connecting us up to the app server.
 * 
 * @author hp
 *
 */
class EjbLink {
	private InitialContext namingContext;
	private IdentitySpider identitySpider;
	private AuthenticationSystem authenticationSystem;
	private InvitationSystem invitationSystem;
	
	private Object nameLookup(Class clazz) throws NamingException {
		return namingContext.lookup(clazz.getPackage().getName() + "." + clazz.getSimpleName());
	}
	
	public EjbLink() throws NamingException {
		namingContext = new InitialContext();
		identitySpider = (IdentitySpider) nameLookup(IdentitySpider.class);
		authenticationSystem = (AuthenticationSystem) nameLookup(AuthenticationSystem.class);
		invitationSystem = (InvitationSystem) nameLookup(InvitationSystem.class);
	}
	
	public IdentitySpider getIdentitySpider() {
		return identitySpider;
	}
	
	public AuthenticationSystem getAuthenticationSystem() {
		return authenticationSystem;
	}
	
	public InvitationSystem getInvitationSystem() {
		return invitationSystem;
	}
	
	public static void main(String[] args) {
		EjbLink link = null;
		
		try {
			link = new EjbLink();
		} catch (NamingException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		AuthenticationSystem auth = link.getAuthenticationSystem();
	
		assert auth != null;
	}
}
