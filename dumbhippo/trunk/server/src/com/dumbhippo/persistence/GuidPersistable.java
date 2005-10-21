
package com.dumbhippo.persistence;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratorType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class GuidPersistable implements Serializable {
	private Guid guid;
	
	protected GuidPersistable() {
		setGuid(Guid.createNew());
	}
	
	protected GuidPersistable(Guid guid) {
		setGuid(guid);
	}
	
	protected void setGuid(Guid guid) {
		// no copy since Guid is immutable
		this.guid = guid;
	}
	
	@Transient
	public Guid getGuid() {
		assert guid != null;
		return guid;
	}

	/* Should be final, except this makes Hibernate CGLIB enhancement barf */
	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof GuidPersistable))
			return false;
		return ((GuidPersistable) arg0).getGuid().equals(guid);
	}

	/* Should be final, except this makes Hibernate CGLIB enhancement barf */	
	@Override
	public int hashCode() {
		return guid.hashCode();
	}

	/** 
	 * For hibernate to use as the ID column. 
	 * Should return guid.toString() generally.
	 * 
	 * @return the hex string form of the GUID
	 */
	@Id(generate = GeneratorType.NONE)
	@Column(length = 48, nullable = false)
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