package com.dumbhippo.jive;

import org.xmpp.packet.IQ;

import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.ContactStatus;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.dm.ContactDMO;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.dm.UserDMO;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;

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
	
	private enum AddressType {
		EMAIL,
		AIM,
		XMPP;

		static AddressType fromString(String s) {
			for (AddressType a : values()) {
				if (s.equals(a.name().toLowerCase()))
					return a;
			}
			return null;
		}
	};
	
	private AddressType parseAddressType(String s) throws IQException {
		AddressType a = AddressType.fromString(s);
		if (a == null)
			throw IQException.createBadRequest("Bad address type " + s);
		else
			return a;
	}
	
	private Resource getResourceFromAddress(IdentitySpider identitySpider, AddressType addressType, String address) throws RetryException, IQException {
		try {
			switch (addressType) {
			case EMAIL:
				return identitySpider.getEmail(address);
			case AIM:
				return identitySpider.getAim(address);
			case XMPP:
				return identitySpider.getXmpp(address);
			}
		} catch (ValidationException e) {
			throw IQException.createBadRequest(e.getMessage());
		}
		throw new RuntimeException("Unhandled AddressType " + addressType);
	}
	
	private Resource lookupResourceFromAddress(IdentitySpider identitySpider, AddressType addressType, String address) throws IQException, NotFoundException {
		switch (addressType) {
		case EMAIL:
			return identitySpider.lookupEmail(address);
		case AIM:
			return identitySpider.lookupAim(address);
		case XMPP:
			return identitySpider.lookupXmpp(address);
		}
		throw new RuntimeException("Unhandled AddressType " + addressType);
	}
	
	@IQMethod(name="addContactAddress", type=IQ.Type.set)
	@IQParams({ "contact", "addressType", "address" })
	public void addContactAddress(UserViewpoint viewpoint, ContactDMO contactDMO, String addressType, String address) throws IQException, RetryException {
		IdentitySpider identitySpider = EJBUtil.defaultLookup(IdentitySpider.class);
		
		Contact contact = identitySpider.lookupContact(contactDMO.getKey());
		if (contact == null)
			throw IQException.createBadRequest("Unknown contact " + contactDMO.getKey());
		
		AddressType a = parseAddressType(addressType);
		Resource resource = getResourceFromAddress(identitySpider, a, address);
		identitySpider.addContactResource(contact, resource);
	}
	
	@IQMethod(name="removeContactAddress", type=IQ.Type.set)
	@IQParams({ "contact", "addressType", "address" })
	public void removeContactAddress(UserViewpoint viewpoint, ContactDMO contactDMO, String addressType, String address) throws IQException {
		IdentitySpider identitySpider = EJBUtil.defaultLookup(IdentitySpider.class);
		
		Contact contact = identitySpider.lookupContact(contactDMO.getKey());
		if (contact == null)
			throw IQException.createBadRequest("Unknown contact " + contactDMO.getKey());
		
		AddressType a = parseAddressType(addressType);
		Resource resource;
		try {
			resource = lookupResourceFromAddress(identitySpider, a, address);
		} catch (NotFoundException e) {
			return; // nothing to do then
		}
		identitySpider.removeContactResource(contact, resource);
	}
	
	@IQMethod(name="createContact", type=IQ.Type.set)
	@IQParams({ "addressType", "address" })
	public void createContact(UserViewpoint viewpoint, String addressType, String address) throws IQException, RetryException {
		IdentitySpider identitySpider = EJBUtil.defaultLookup(IdentitySpider.class);
		
		AddressType a = parseAddressType(addressType);
		Resource resource = getResourceFromAddress(identitySpider, a, address);
		
		Contact contact = identitySpider.createContact(viewpoint.getViewer(), resource);
		// FIXME we aren't allowed to return a value, but it will be hard for the caller to figure out 
		// what contact was created
		
	}
	
	@IQMethod(name="createUserContact", type=IQ.Type.set)
	@IQParams({ "user" })
	public void createUserContact(UserViewpoint viewpoint, UserDMO userDMO) throws IQException, RetryException {
		IdentitySpider identitySpider = EJBUtil.defaultLookup(IdentitySpider.class);
		
		User user = identitySpider.lookupUser(userDMO.getKey());
		Contact contact = identitySpider.createContact(viewpoint.getViewer(), user.getAccount());
		// FIXME we aren't allowed to return a value, but it will be hard for the caller to figure out 
		// what contact was created
	}
	
	@IQMethod(name="deleteContact", type=IQ.Type.set)
	@IQParams({ "contact" })
	public void deleteContact(UserViewpoint viewpoint, ContactDMO contactDMO) throws IQException {
		IdentitySpider identitySpider = EJBUtil.defaultLookup(IdentitySpider.class);
		
		Contact contact = identitySpider.lookupContact(contactDMO.getKey());
		if (contact == null)
			throw IQException.createBadRequest("Unknown contact " + contactDMO.getKey());
	
		identitySpider.deleteContact(viewpoint.getViewer(), contact);
	}
}
