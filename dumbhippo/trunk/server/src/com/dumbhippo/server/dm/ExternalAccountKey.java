package com.dumbhippo.server.dm;

import com.dumbhippo.dm.BadIdException;
import com.dumbhippo.dm.DMKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;

public class ExternalAccountKey implements DMKey {
	private long id = -1;
	private Guid userId;
	private ExternalAccountType type;
	
	public ExternalAccountKey(String keyString) throws BadIdException {
		if (keyString.length() > 15 && keyString.charAt(14) == '.') {
			try {
				userId = new Guid(keyString.substring(0, 14));
			} catch (ParseException e) {
				throw new BadIdException("Bad GUID type in external account ID", e);
			}
			
			try {
				type = ExternalAccountType.valueOf(keyString.substring(15));
			} catch (IllegalArgumentException e) {
				throw new BadIdException("Bad external account type in ID", e);
			}
		} else {
			throw new BadIdException("Bad external account resource ID");
		}
	}
	
	public ExternalAccountKey(ExternalAccount externalAccount) {
		id = externalAccount.getId();
		userId = externalAccount.getAccount().getOwner().getGuid();
		type = externalAccount.getAccountType();
	}
	
	public ExternalAccountKey(Guid userId, ExternalAccountType type) {
		this.userId = userId;
		this.type = type;
	}
	
	public long getId() {
		return id;
	}

	public ExternalAccountType getType() {
		return type;
	}

	public Guid getUserId() {
		return userId;
	}
	
	@Override
	public ExternalAccountKey clone() {
		// We never delete an ExternalAccount object, so the id field can be kept
		// around permanently, and will save a little effort on future lookups.
		
		return this;
	}
	
	@Override
	public int hashCode() {
		return userId.hashCode() + type.ordinal();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ExternalAccountKey))
			return false;
		
		ExternalAccountKey other = (ExternalAccountKey)o;
		
		return type == other.type && userId.equals(other.userId);
	}

	@Override
	public String toString() {
		return userId.toString() + "." + type.name();
	}
}
