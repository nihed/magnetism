package com.dumbhippo.server;

import java.util.Map;

import javax.ejb.Local;

import com.dumbhippo.persistence.User;

@Local
public interface DesktopSettings {

	public Map<String,String> getSettings(User user);
	
	public void setSetting(User user, String key, String value);
	
}
