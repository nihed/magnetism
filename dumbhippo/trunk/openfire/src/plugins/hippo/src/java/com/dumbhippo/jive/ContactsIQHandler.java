package com.dumbhippo.jive;

import org.xmpp.packet.IQ;

import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.ContactStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.dm.ContactDMO;
import com.dumbhippo.server.dm.UserDMO;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.UserViewpoint;

@IQHandler(namespace=ContactsIQHandler.CONTACTS_NAMESPACE)
public class ContactsIQHandler extends AnnotatedIQHandler {
	static final String CONTACTS_NAMESPACE = "http://mugshot.org/p/contacts";
	
	protected ContactsIQHandler() {
		super("Mugshot contacts IQ Handler");
	}

	@IQMethod(name="setUserContactStatus", type=IQ.Type.set)
	@IQParams({ "user", "status" })
	public void setUserContactStatus(UserViewpoint viewpoint, UserDMO userDMO, int statusOrdinal) throws IQException {
		IdentitySpider identitySpider = EJBUtil.defaultLookup(IdentitySpider.class);
		
		User user = identitySpider.lookupUser(userDMO.getKey());
		if (user == null)
			throw IQException.createBadRequest("Unknown user " + userDMO.getKey());
		
		ContactStatus status;
		try {
			 status = ContactStatus.values()[statusOrdinal];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw IQException.createBadRequest("Bad contact status " + statusOrdinal); 
		}
		
		identitySpider.setContactStatus(viewpoint, user, status);
	}

	@IQMethod(name="setContactStatus", type=IQ.Type.set)
	@IQParams({ "contact", "status" })
	public void setContactStatus(UserViewpoint viewpoint, ContactDMO contactDMO, int statusOrdinal) throws IQException {
		IdentitySpider identitySpider = EJBUtil.defaultLookup(IdentitySpider.class);
		
		Contact contact = identitySpider.lookupContact(contactDMO.getKey());
		if (contact == null)
			throw IQException.createBadRequest("Unknown contact " + contactDMO.getKey());
		
		ContactStatus status;
		try {
			 status = ContactStatus.values()[statusOrdinal];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw IQException.createBadRequest("Bad contact status " + statusOrdinal); 
		}
		
		identitySpider.setContactStatus(viewpoint, contact, status);
	}
	
}
