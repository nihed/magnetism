package com.dumbhippo.server.dm;

import com.dumbhippo.dm.BadIdException;
import com.dumbhippo.dm.DMKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;

/**
 * ThumbnailKey is used for all subclasses of ThumbnailDMO; since 
 * ThumbnailDMO is an abstract class rather than an interface, all its
 * subclasses need to have the same key type. (And we don't support
 * datamodel properties on interfaces currently.) The form of a 
 * ThumnailKey is a pair of a userId and a string, where the exact
 * interpretation of the string is up to the subclass.
 * 
 * We also support storing the Thumbnail object in the key transiently
 * within a single session. This is so that converting a list of Thumbnail
 * objects to a list of ThumbnailDMO doesn't require looking up every
 * thumbnail from the database again.
 */
public class ThumbnailKey implements DMKey {
	private static final long serialVersionUID = 2258099192938259545L;
	
	private transient Object object;
	private Guid userId;
	private String extra;
	
	public ThumbnailKey(String keyString) throws BadIdException {
		if (keyString.length() > 15 && keyString.charAt(14) == '.') {
			try {
				userId = new Guid(keyString.substring(0, 14));
			} catch (ParseException e) {
				throw new BadIdException("Bad GUID type in external account ID", e);
			}
			
			extra = keyString.substring(15);
		} else {
			throw new BadIdException("Bad thumbnail resource ID");
		}
	}
	
	public ThumbnailKey(Guid userId, String extra) {
		this.userId = userId;
		this.extra = extra;
	}
	
	public ThumbnailKey(Guid userId, String extra, Object object) {
		this.userId = userId;
		this.extra = extra;
		this.object = object;	
	}
	
	public Object getObject() {
		return object;
	}

	public String getExtra() {
		return extra;
	}

	public Guid getUserId() {
		return userId;
	}
	
	@Override
	public ThumbnailKey clone() {
		if (object != null)
			return new ThumbnailKey(userId, extra);
		else
			return this;
	}
	
	@Override
	public int hashCode() {
		return userId.hashCode() * 11 + extra.hashCode() * 17;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ThumbnailKey))
			return false;
		
		ThumbnailKey other = (ThumbnailKey)o;
		
		return userId.equals(other.userId) && extra.equals(other.extra);
	}

	@Override
	public String toString() {
		return userId.toString() + "." + extra;
	}
}
