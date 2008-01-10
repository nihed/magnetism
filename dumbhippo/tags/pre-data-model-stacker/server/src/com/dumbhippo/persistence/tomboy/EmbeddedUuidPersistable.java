package com.dumbhippo.persistence.tomboy;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.dumbhippo.persistence.DBUnique;

@MappedSuperclass
public class EmbeddedUuidPersistable extends DBUnique {
	// 32 hex digits plus 4 hyphens
	public static final int UUID_STRING_LENGTH = 36;

	private UUID uuid;
	
	@Transient
	public UUID getUuidObject() {
		if (uuid == null)
			setUuidObject(UUID.randomUUID());
		return uuid;
	}
	
	private void setUuidObject(UUID uuid) {
		this.uuid = uuid;
	}
	
	@Column(length = UUID_STRING_LENGTH, nullable = false)
	public String getUuid() {
		String s = getUuidObject().toString();
		if (s.length() != UUID_STRING_LENGTH)
			throw new RuntimeException("UUID has wrong length string representation '" + s + "' length " + s.length());
		return s;		
	}
	
	public void setUuid(String s) {
		UUID uuid = UUID.fromString(s);
		setUuidObject(uuid);
	}
}
