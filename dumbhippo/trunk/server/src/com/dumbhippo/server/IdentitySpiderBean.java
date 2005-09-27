package com.dumbhippo.server;

import org.hibernate.Query;
import org.hibernate.Session;

import com.dumbhippo.persistence.Storage;
import com.dumbhippo.persistence.Storage.SessionWrapper;
/*
 * An implementation of the Identity Spider.  It sucks your blood.
 * @author walters
 */
public class IdentitySpiderBean implements IdentitySpider {
	
	private static final String BASE_LOOKUP_PERSON_EMAIL_QUERY = "select p from Person p, PersonOwnershipClaim c where p.id = c.claimedOwner ";

	public Person lookupPersonByEmail(String email) {
		Session hsession = Storage.getGlobalPerThreadSession().getSession();		
		return (Person) hsession.createQuery(BASE_LOOKUP_PERSON_EMAIL_QUERY + "and c.assertedBy is null").uniqueResult();
	}
	
	public Person lookupPersonByEmail(Person viewpoint, String email) {
		Session hsession = Storage.getGlobalPerThreadSession().getSession();		
		return (Person) hsession.createQuery(BASE_LOOKUP_PERSON_EMAIL_QUERY + "and (c.assertedBy.id = :viewpointguid or c.assertedBy.id is null)")
		.setString("viewpointguid", viewpoint.getId());
	}
	
	/**
	 * Look up the resource corresponding to an email address, from
	 * the perspective of a particular person. 
	 * 
	 * FIXME - should this really be uniqueResult()?  If so we need
	 * to enforce that somehow.
	 * 
	 * @param perspective the viewpoint
	 * @param email the email address to look up
	 * @return the corresponding resource, or null if none
	 */
	protected EmailResource lookupEmail(Person perspective, String email) {
		Session hsession = Storage.getGlobalPerThreadSession().getSession();		
		return (EmailResource) hsession.createQuery("from EmailResource e where e.email = :addr")
		.setString("addr", email).uniqueResult();
	}
	
	protected EmailResource lookupCreateEmail(String email) {
		SessionWrapper sess = Storage.getGlobalPerThreadSession();
		Session hsession = sess.getSession();
		
		EmailResource res = lookupEmail(null, email);
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

	public String getHumanReadableId(Person inviter) {
		// TODO look up full name too
		return getEmailAddress(inviter);
	}
	
	private static final String BASE_LOOKUP_EMAIL_QUERY 
		= "select e from EmailResource e, PersonOwnershipClaim c where e.id = c.resource and c.claimedOwner = :personid and (c.assertedBy.id is null ";	

	public String getEmailAddress(Person viewpoint, Person p) {
		EmailResource res;
		Session hsession = Storage.getGlobalPerThreadSession().getSession();
		Query q;
		if (viewpoint == null) {
			q = hsession.createQuery(BASE_LOOKUP_EMAIL_QUERY + ")");
		} else {
			q = hsession.createQuery(BASE_LOOKUP_EMAIL_QUERY + "or c.assertedBy.id = :viewpointguid)").setString("viewpointguid", viewpoint.getId());
		}
		q.setParameter("personid", p.getId());
		res = (EmailResource) q.uniqueResult();

		if (res == null)
			return null;
		else
			return res.getEmail();
	}

	public String getEmailAddress(Person p) {
		return getEmailAddress(null, p);
	}


}

