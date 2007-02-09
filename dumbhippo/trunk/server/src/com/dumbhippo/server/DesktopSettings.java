package com.dumbhippo.server;

import java.util.Map;

import javax.ejb.Local;

import com.dumbhippo.persistence.User;

/** 
 * A "desktop setting" is state stored for a user's desktop(s).
 * For Linux, typically we might stuff some gconf-like key-value 
 * pairs in here.
 * 
 * @author Havoc Pennington
 *
 */
@Local
public interface DesktopSettings {

	/** 
	 * Gets all settings for a given user
	 * @param user
	 * @return
	 */
	public Map<String,String> getSettings(User user);
	
	/**
	 * Gets one setting
	 * @param user
	 * @param key
	 * @return the value, or null if unset
	 */
	public String getSetting(User user, String key);
	
	/** 
	 * Sets a new value for a setting, or unsets if value is null
	 * @param user whose setting is it
	 * @param key the setting name
	 * @param value new value, or null to unset
	 */
	public void setSetting(User user, String key, String value);
	
}
