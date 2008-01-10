package com.dumbhippo.server.dm;

import com.dumbhippo.dm.BadIdException;
import com.dumbhippo.dm.DMKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;

public class NetflixMovieKey implements DMKey {
	private static final long serialVersionUID = 2258099192938259545L;
	
	private transient Object object;
	private Guid userId;
	private String extra;
	
	public NetflixMovieKey(String keyString) throws BadIdException {
		if (keyString.length() > 15 && keyString.charAt(14) == '.') {
			try {
				userId = new Guid(keyString.substring(0, 14));
			} catch (ParseException e) {
				throw new BadIdException("Bad GUID type in external account ID", e);
			}
		} else {
			throw new BadIdException("Bad thumbnail resource ID");
		}

		extra = keyString.substring(15);
	}
	
	public NetflixMovieKey(Guid userId, String extra) {
		this.userId = userId;
		this.extra = extra;
	}
	
	public NetflixMovieKey(Guid userId, String extra, Object object) {
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
	public NetflixMovieKey clone() {
		if (object != null)
			return new NetflixMovieKey(userId, extra);
		else
			return this;
	}
	
	@Override
	public int hashCode() {
		return userId.hashCode() * 11 + extra.hashCode() * 17;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof NetflixMovieKey))
			return false;
		
		NetflixMovieKey other = (NetflixMovieKey)o;
		
		return userId.equals(other.userId) && extra.equals(other.extra);
	}

	@Override
	public String toString() {
		return userId.toString() + "." + extra;
	}
}
