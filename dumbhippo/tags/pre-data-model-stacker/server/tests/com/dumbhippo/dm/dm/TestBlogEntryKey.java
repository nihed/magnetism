package com.dumbhippo.dm.dm;

import com.dumbhippo.dm.BadIdException;
import com.dumbhippo.dm.DMKey;
import com.dumbhippo.dm.persistence.TestBlogEntry;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;

public class TestBlogEntryKey implements DMKey {
	private static final long serialVersionUID = -5724889995159821821L;

	private Guid userId;
	private long serial;
	
	public TestBlogEntryKey(Guid groupId, long serial) {
		this.userId = groupId;
		this.serial = serial;
	}
	
	public TestBlogEntryKey(String keyString) throws BadIdException {
		String[] strings = keyString.split("\\.");
		if (strings.length != 2)
			throw new BadIdException("Invalid blog entry key: " + keyString);
		
		try {
			this.userId = new Guid(strings[0]);
		} catch (ParseException e) {
			throw new BadIdException("Invalid GUID in blog entry key");
		}
		
		try {
			this.serial = Long.parseLong(strings[1]);
		} catch (NumberFormatException e) {
			throw new BadIdException("Invalid serial in blog entry key");
		}
	}

	public TestBlogEntryKey(TestBlogEntry blogEntry) {
		this.userId = blogEntry.getUser().getGuid();
		this.serial = blogEntry.getSerial();
	}
	

	public Guid getUserId() {
		return userId;
	}

	public long getSerial() {
		return serial;
	}

	@Override
	public TestBlogEntryKey clone() {
		return this; // Immutable, nothing session-specific
	}
	
	@Override
	public int hashCode() {
		return (int)(userId.hashCode() * 11 + serial * 17);  
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TestBlogEntryKey))
			return false;
		
		TestBlogEntryKey other = (TestBlogEntryKey)o;
		return other.userId.equals(userId) && other.serial == serial;
		
	}

	@Override
	public String toString() {
		return userId.toString() + "." + Long.toString(serial);
	}
}
