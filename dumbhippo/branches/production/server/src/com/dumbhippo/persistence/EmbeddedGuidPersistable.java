package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;

/**
 * Like GuidPersistable, but just provides implementation, doesn't 
 * put the subclass in a table with all other GuidPersistable objects.
 * 
 * Probably GuidPersistable could derive from this? But it seems likely to result
 * in weirdness, and the long-term plan would probably be to migrate from 
 * GuidPersistable to always using EmbeddedGuidPersistable so we can just kind 
 * of phase out GuidPersistable instead of rationalizing it.
 * 
 * @author Havoc Pennington
 *
 */
@MappedSuperclass
public abstract class EmbeddedGuidPersistable {
	private Guid guid;
	
	protected EmbeddedGuidPersistable() {
		setGuid(Guid.createNew());
	}
	
	protected EmbeddedGuidPersistable(Guid guid) {
		setGuid(guid);
	}
	
	protected void setGuid(Guid guid) {
		// no copy since Guid is immutable
		this.guid = guid;
	}
	
	@Transient
	public Guid getGuid() {
		if (guid == null)
			setGuid(Guid.createNew());
		return guid;
	}

	/* Should be final, except this makes Hibernate CGLIB enhancement barf */
	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof GuidPersistable))
			return false;
		if (arg0 == this)
			return true;
		// If the argument is not actually the same object, and
		// we haven't generated a guid yet, they cannot be equal
		if (guid == null)
			return false;
		return ((GuidPersistable) arg0).getGuid().equals(getGuid());
	}

	/* Should be final, except this makes Hibernate CGLIB enhancement barf */	
	@Override
	public int hashCode() {
		return getGuid().hashCode();
	}

	/** 
	 * For hibernate to use as the ID column. 
	 * Should return guid.toString() generally.
	 * 
	 * @return the hex string form of the GUID
	 */
	@Id
	@Column(length = Guid.STRING_LENGTH, nullable = false)
	public String getId() {
		String s = getGuid().toString();
		assert s.length() == Guid.STRING_LENGTH;
		return s;		
	}

	/** 
	 * If anyone other than Hibernate calls this it's 
	 * probably bad and evil.
	 * 
	 * @param hexId the hex GUID to set
	 */
	public void setId(String hexId) {
		try {
			setGuid(new Guid(hexId));
		} catch (ParseException e) {
			// this really, really should not happen; because 
			// setId should only be called by hibernate and we should not 
			// have an invalid Guid in the database
			throw new IllegalStateException("Invalid Guid " + hexId + " in the database????", e);
		}
	}
}
