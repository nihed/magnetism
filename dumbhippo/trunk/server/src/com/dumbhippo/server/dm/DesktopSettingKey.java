package com.dumbhippo.server.dm;

import com.dumbhippo.StringUtils;
import com.dumbhippo.dm.BadIdException;
import com.dumbhippo.dm.DMKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.DesktopSetting;

public class DesktopSettingKey implements DMKey {
	private static final long serialVersionUID = 0L;

	// this is just a cache
	private transient long id = -1;
	
	private Guid userId;
	private String keyName;
	
	public DesktopSettingKey(String keyString) throws BadIdException {
		if (keyString.length() > 15 && keyString.charAt(14) == '.') {
			try {
				userId = new Guid(keyString.substring(0, 14));
			} catch (ParseException e) {
				throw new BadIdException("Bad GUID type in setting ID", e);
			}
			
			String keyNameEscaped = keyString.substring(15);
			
			try {
				keyName = StringUtils.urlDecode(keyNameEscaped);
			} catch (IllegalArgumentException e) {
				throw new BadIdException("Bad key name escaping in setting ID", e);
			}
		} else {
			throw new BadIdException("Bad external account resource ID");
		}
	}
	
	public DesktopSettingKey(DesktopSetting setting) {
		userId = setting.getUser().getGuid();
		keyName = setting.getKeyName();
		id = setting.getId();
	}
	
	public DesktopSettingKey(Guid userId, String keyName) {
		this.userId = userId;
		this.keyName = keyName;
	}

	public long getId() {
		return id;
	}
	
	public String getKeyName() {
		return keyName;
	}

	public Guid getUserId() {
		return userId;
	}
	
	@Override
	public DesktopSettingKey clone() {
		try {	
			DesktopSettingKey clone = (DesktopSettingKey) super.clone();
			// the "transient" on the id field may make this unnecessary?
			if (clone.id >= 0)
				clone.id = -1;
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public int hashCode() {
		return userId.hashCode() + keyName.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DesktopSettingKey))
			return false;
		
		DesktopSettingKey other = (DesktopSettingKey)o;
		
		return keyName.equals(other.keyName) && userId.equals(other.userId);
	}

	@Override
	public String toString() {
		return userId.toString() + "." + StringUtils.urlEncode(keyName);
	}
}
