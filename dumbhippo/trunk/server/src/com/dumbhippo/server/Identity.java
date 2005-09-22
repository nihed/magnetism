/**
 * Identity class represents a group or 1 or more accounts or publications that
 * belonging to a person or organization on the internet
 */
package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hp
 * 
 * A group or 1 or more accounts or publications belonging to a person or
 * organization on the internet
 */
final class Identity {

	/**
	 * @author hp
	 * 
	 * Enum describing each property an identity can have.
	 */
	public enum Property {
		GUID {
			public boolean validate(Object o) {
				return o instanceof Guid;
			}

			public String valueToString(Object value) {
				return ((Guid) value).toString();
			}

			public Object valueFromString(String s)
					throws IllegalArgumentException {
				return new Guid(s);
			}
		},

		FULLNAME {
			public boolean validate(Object o) {
				return o instanceof String;
			}

			public String valueToString(Object value) {
				return (String) value;
			}

			public Object valueFromString(String s)
					throws IllegalArgumentException {
				return new String(s);
			}
		},

		EMAILS {
			
			public boolean validate(Object o) {
				if (o instanceof java.util.List) {
					for (Object email : (java.util.List) o) {
						if (!(email instanceof String))
							return false;
					}
				} else {
					return false;
				}
					
				return true;
			}

			public String valueToString(Object value) {
				StringBuilder builder = new StringBuilder();
				for (Object email : (java.util.List) value) {
					builder.append((String) email);
					builder.append(",");
				}
				// chop comma
				if (builder.length() > 0)
					builder.deleteCharAt(builder.length() - 1);
				return builder.toString();
			}

			public Object valueFromString(String s)
					throws IllegalArgumentException {

				String[] tokens = s.split(",");

				List<String> value = Arrays.asList(tokens);

				return value;
			}

			public Object defensiveCopyAndValidate(Object value)
					throws IllegalArgumentException {
				List<String> copy = new ArrayList<String>();
				if (value instanceof java.util.List) {
					for (Object email : (java.util.List) value) {
						if (!(email instanceof String))
							throw new IllegalArgumentException(
									"Non-string in the list of email addresses");
						// strings are immutable so OK not to copy them right?
						copy.add((String) email);
					}
				} else {
					throw new IllegalArgumentException(
							"Must provide a list of strings where each string is an email address");
				}
				
				return copy;
			}
		};

		protected abstract boolean validate(Object o);

		public abstract String valueToString(Object value);

		public abstract Object valueFromString(String s)
				throws IllegalArgumentException;

		/**
		 * Returns the value, or a defensive copy of it if appropriate, after
		 * validating the value. Throws IllegalArgumentException if the value
		 * doesn't validate.
		 * 
		 * "value" isn't validated yet when this is called since we want to
		 * validate the copy instead (Effective Java p. 123)
		 * 
		 * Default implementation calls validate(value) and then returns the
		 * value uncopied. This is only OK if the value is immutable.
		 * 
		 * When implementing, avoid using clone() on a value that could be an
		 * untrusted/unknown subclass.
		 * 
		 * @throws IllegalArgumentException
		 *             if value does not validate
		 * @param value
		 *            the value to validate and potentially copy
		 * @returns the value or a copy of it
		 */
		public Object defensiveCopyAndValidate(Object value)
				throws IllegalArgumentException {
			if (!validate(value))
				throw new IllegalArgumentException();
			return value;
		}

	}

	private Identity() {
		properties = new HashMap<Property, Object>();
	}

	/**
	 * Creates a new identity with a newly-generated GUID and no other
	 * properties.
	 * 
	 * @return the new identity
	 */
	public static Identity createNew() {
		Identity id = new Identity();
		id.properties.put(Property.GUID, Guid.createNew());
		return id;
	}

	/**
	 * Creates a new identity with the given properties; the properties must
	 * include a GUID property.
	 * 
	 * @param keyValuePairs
	 * @return the new identity
	 * @throws IllegalArgumentException
	 *             if the values are broken
	 */
	public static Identity createFromProperties(
			Map<Property, Object> keyValuePairs)
			throws IllegalArgumentException {
		Identity id = new Identity();

		if (!keyValuePairs.containsKey(Property.GUID)) {
			throw new IllegalArgumentException(
					"GUID property is required when creating an Identity");
		}

		for (Map.Entry<Property, Object> e : keyValuePairs.entrySet()) {
			id.setProperty(e.getKey(), e.getValue());
		}
		return id;
	}

	public boolean hasProperty(Property property) {
		return properties.containsKey(property);
	}

	/** 
	 * Gets the given property; returns null if the property 
	 * doesn't exist (or if the property value is null...).
	 * Type of the return value depends on the property.
	 * 
	 * @param property
	 * @return the property value or null
	 */
	public Object getProperty(Property property) {
		return properties.get(property);
	}

	public void setProperty(Property property, Object value)
			throws IllegalArgumentException {
		Object copy = property.defensiveCopyAndValidate(value);
		properties.put(property, copy);
	}

	public void setPropertyAsString(Property property, String value)
			throws IllegalArgumentException {
		Object o = property.valueFromString(value);

		// don't call setProperty() since we don't need the copy
		// and validate
		properties.put(property, o);
	}

	public String getPropertyAsString(Property property) {
		return property.valueToString(properties.get(property));
	}

	public List<Property> listProperties() {
		List<Property> list = new ArrayList<Property>();
		for (Property p : properties.keySet()) {
			list.add(p);
		}
		return list;
	}
	
	private Map<Property, Object> properties;
}
