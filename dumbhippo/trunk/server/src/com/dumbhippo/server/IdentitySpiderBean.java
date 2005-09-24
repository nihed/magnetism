package com.dumbhippo.server;

import org.hibernate.Session;

import com.dumbhippo.persistence.Storage;
import com.dumbhippo.persistence.Storage.SessionWrapper;
/*
 * An implementation of the Identity Spider.  It sucks your blood.
 * @author walters
 */
public class IdentitySpiderBean implements IdentitySpider {
	
	private static final String BASE_EMAIL_QUERY = "select p from Person p, OwnershipClaim c where p.guid = c.claimedOwner";

	public Person lookupPersonByEmail(String email) {
		Session hsession = Storage.getGlobalPerThreadSession().getSession();		
		return (Person) hsession.createQuery(BASE_EMAIL_QUERY + "and c.assertedBy is null").uniqueResult();
	}
	
	public Person lookupPersonByEmail(Person viewpoint, String email) {
		Session hsession = Storage.getGlobalPerThreadSession().getSession();		
		return (Person) hsession.createQuery(BASE_EMAIL_QUERY + "and (c.assertedBy.guid = :viewpointguid or c.assertedBy.guid is null)")
		.setString("viewpointguid", viewpoint.getId());
	}
	
	protected EmailResource lookupCreateEmail(String email) {
		SessionWrapper sess = Storage.getGlobalPerThreadSession();
		Session hsession = sess.getSession();
		
		EmailResource res = (EmailResource) hsession.createQuery("from EmailResource e where e.address = :addr")
		.setString("addr", email).uniqueResult();
		if (res == null) {
			res = new EmailResource(email);
			hsession.save(res);
		}
		
		return res;
	}
	
	public Person addPersonWithEmail(String email) {
		SessionWrapper sess = Storage.getGlobalPerThreadSession();
		Session hsession = sess.getSession();		
		Person p = new Person();
		hsession.save(p);
		EmailResource res = lookupCreateEmail(email);
		PersonOwnershipClaim claim = new PersonOwnershipClaim(p, res);
		hsession.save(claim);
		return p;
	}	
	
	public Person lookupPersonByAim(String email) {
		// TODO Auto-generated method stub
		return null;
	}

	public Person lookupPersonByAim(Person viewpoint, String email) {
		// TODO Auto-generated method stub
		return null;
	}


}

