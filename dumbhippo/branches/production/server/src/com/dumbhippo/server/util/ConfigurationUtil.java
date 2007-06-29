package com.dumbhippo.server.util;

import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;

public class ConfigurationUtil {

	/**
	 * Returns a value for the specified property or null if the property is 
	 * not found or empty.
	 * 
	 * @param config
	 * @param property
	 * @return a value for the specified property
	 */
	static public String loadProperty(Configuration config, HippoProperty property) {
		String propertyValue;
		try {
			propertyValue = config.getPropertyNoDefault(property);
			if (propertyValue.trim().length() == 0)
				propertyValue = null;
		} catch (PropertyNotFoundException e) {
			propertyValue = null;
		}
		return propertyValue;
	}
}
