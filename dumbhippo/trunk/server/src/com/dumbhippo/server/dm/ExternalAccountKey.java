package com.dumbhippo.server.dm;

import com.dumbhippo.dm.BadIdException;
import com.dumbhippo.dm.DMKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.OnlineAccountType;

public class ExternalAccountKey implements DMKey {
	
	private static final long serialVersionUID = 7179756386307688402L;
	
	private transient long id = -1; // doesn't really need to be transient, 
	                                // but we do it this way for consistency
	                                // with the string form
	private Guid userId;
	private String type;
	
	public ExternalAccountKey(String keyString) throws BadIdException {
		if (keyString.length() > 15 && keyString.charAt(14) == '.') {
			try {
				userId = new Guid(keyString.substring(0, 14));
			} catch (ParseException e) {
				throw new BadIdException("Bad GUID type in external account ID", e);
			}
			
			try {
			    type = keyString.substring(15, keyString.indexOf(".", 15));
			} catch (IndexOutOfBoundsException e) {
				throw new BadIdException("The key " + keyString + " did not contain a second period separating the type from id", e);
			}
			
			try {
			    id = Long.parseLong(keyString.substring(keyString.indexOf(".", 15) + 1));
			    
			} catch (NumberFormatException e) {
				throw new BadIdException("Bad external account id in key " + keyString, e);
			} catch (IndexOutOfBoundsException e) {
				throw new BadIdException("Missing external account id in key " + keyString, e);	
			}	
		} else {
			throw new BadIdException("Bad external account resource id in key " + keyString);
		}
	}
	
	public ExternalAccountKey(ExternalAccount externalAccount) {
		id = externalAccount.getId();
		userId = externalAccount.getAccount().getOwner().getGuid();
		type = externalAccount.getOnlineAccountType().getName();
	}
	
	public ExternalAccountKey(Guid userId, OnlineAccountType type, long id) {
		this.userId = userId;
		this.type = type.getName();
		this.id = id;
	}
	
	public long getId() {
		return id;
	}

	public String getType() {
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
		// casting a long to an int should just chop off the bits if the long is too large, so it's
		// ok to do that here
		return userId.hashCode() + type.hashCode() + (int)id;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ExternalAccountKey))
			return false;
		
		ExternalAccountKey other = (ExternalAccountKey)o;
		
		return type.equals(other.type) && userId.equals(other.userId) && (id == other.id);
	}

	@Override
	public String toString() {
		return userId.toString() + "." + type + "." + String.valueOf(id);
	}
}
