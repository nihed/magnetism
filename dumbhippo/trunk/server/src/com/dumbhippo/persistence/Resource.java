/**
 * 
 */
package com.dumbhippo.persistence;

import java.util.Collections;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.dumbhippo.identity20.Guid;

/**
 * A Resource is some object that has an associated GUID
 * and everyone can have a different opinion about its owner.
 * i.e. make something a Resource rather than GuidPersistable
 * if you want it to be subject to AccountClaim or ContactClaim
 * (see also PersonView)
 * 
 * @author hp
 *
 */
@Entity
public abstract class Resource extends GuidPersistable {
	
	private Set<AccountClaim> accountClaims;
	
	public Resource() {
	}
	
	public Resource(Guid guid) {
		super(guid);
	}

	/**
	 * Return a human-readable string form of this object.
	 * @return a readable string, or null if none
	 */
	@Transient
	public abstract String getHumanReadableString();
	
	
	/**
	 * Get a default nickname derived from this resource.
	 * @return the derived nickname
	 */
	@Transient
	public abstract String getDerivedNickname();
	
	// We use OneToMany here rather than OneToOne, since OneToOne inverse
	// relationships are not cached by hibernate; then we wrap the field
	// with a transient getter that returns the single value 
	@OneToMany(fetch=FetchType.LAZY, mappedBy="resource")
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	protected Set<AccountClaim> getAccountClaims() {
		return accountClaims;		
	}
	
	protected void setAccountClaims(Set<AccountClaim> accountClaims) {
		this.accountClaims = accountClaims;
	}
	
	@Transient
	public AccountClaim getAccountClaim() {
		Set<AccountClaim> claims = getAccountClaims();
		if (claims == null || claims.size() == 0)
			return null;
		
		return claims.iterator().next();
	}
	
	@Transient
	public void setAccountClaim(AccountClaim accountClaim) {
		this.accountClaims = Collections.singleton(accountClaim);
	}
	
	@Override
	public String toString() {
		// this is for debug spew, getHumanReadableString() is for showing to humans
		// (the quotes are really just so we notice if we mess that up)
		return "'" + getHumanReadableString() + "'";
	}
}
