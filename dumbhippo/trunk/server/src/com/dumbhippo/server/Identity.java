/**
 * Identity class represents a group or 1 or more accounts or publications that
 * belonging to a person or organization on the internet
 */
package com.dumbhippo.server;

import java.security.SecureRandom;
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
	 * Guid is a globally-unique identifier for the Identity, designed to not
	 * require coordination (i.e. you can make up the GUID without asking the
	 * server for the next counter value or something)
	 * 
	 * This class is immutable with the advantages that entails.
	 * 
	 * @author hp
	 * 
	 */
	public static class Guid {

		private static java.util.Random rng1;
		private static java.util.Random rng2;

		private static final int NUM_COMPONENTS = 3;

		public static Guid createNew() {
			long random1, random2;
			synchronized (Guid.class) {
				if (rng1 == null) {
					rng1 = new SecureRandom();
					assert(rng2 == null);
					rng2 = new SecureRandom();
				}
				random1 = rng1.nextLong();
				if (random1 < 0)
					random1 = -random1;
				random2 = rng2.nextLong();
				if (random2 < 0)
					random2 = -random2;
			}
			long time = System.currentTimeMillis();

			return new Guid(time, random1, random2);
		}

		/**
		 * Internal constructor for copying/creating by component
		 * 
		 */
		private Guid(long first, long second, long third) {
			assert (first >= 0);
			assert (second >= 0);
			assert (third >= 0);
			components = new long[NUM_COMPONENTS];
			components[0] = first;
			components[1] = second;
			components[2] = third;
		}

		public Guid(Guid source) {
			assert (source.components[0] >= 0);
			assert (source.components[1] >= 0);
			assert (source.components[2] >= 0);
			components = source.components.clone();
		}

		public Guid(String string) throws IllegalArgumentException {
			String[] elements = string.split("-");
			if (elements.length != NUM_COMPONENTS)
				throw new IllegalArgumentException(
						"String form of GUID must be three tokens separated by hyphen");

			components = new long[NUM_COMPONENTS];
			for (int i = 0; i < NUM_COMPONENTS; ++i) {
				try {
					components[i] = Long.parseLong(elements[i], 16);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(
							"Could not parse one of the numbers in the GUID", e);
				}
				if (components[i] < 0)
					throw new IllegalArgumentException(
							"Negative numbers in GUID");
			}
		}

		public String toString() {
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < NUM_COMPONENTS; ++i) {
				builder.append(Long.toHexString(components[i]));
				builder.append("-");
			}
			// kill extra hyphen
			builder.deleteCharAt(builder.length() - 1);
			return builder.toString();
		}

		public int hashCode() {
			int result = 17;
			for (int i = 0; i < NUM_COMPONENTS; ++i) {
				int c = (int) (components[i] ^ (components[i] >>> 32));
				result = 37 * result + c;
			}
			return result;
		}

		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (!(other instanceof Guid))
				return false;
			Guid otherGuid = (Guid) other;
			for (int i = 0; i < NUM_COMPONENTS; ++i) {
				if (components[i] != otherGuid.components[i])
					return false;
			}
			return true;
		}

		private long[] components;
	}

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
