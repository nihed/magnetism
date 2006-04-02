/**
 * 
 */
package com.dumbhippo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * FullName represents someone's full name. Basically 
 * a convenience wrapper around a List<String>.
 * 
 * It is fully generic and not first/middle/last because
 * I have two middle names, dammit.
 * 
 * This doesn't define a database table, it's just an implementation
 * field in other classes.
 * 
 * The object is immutable; no post-construct setters please.
 * If you added those you'd have to go through the code adding
 * defensive copies all over the place.
 * 
 * @author hp
 *
 */
final public class FullName implements Serializable {
	

	private static final long serialVersionUID = 0L;
	private List<String> names;
	
	public FullName(String... names) {
		setNames(Arrays.asList(names));
	}
	
	public FullName(List<String> names) {
		setNames(names);
	}
	
	public FullName(FullName name) {
		if (name.names != null)
			setNames(name.names);
	}

	/**
	 * This is private because FullName is an immutable class.
	 * @param names
	 */
	private void setNames(List<String> names) {
		if (names == null || names.isEmpty())
			throw new IllegalArgumentException("Names can't be null or empty");
		// defensive copy
		this.names = new ArrayList<String>(names);
	}

	public List<String> getNames() {
		return Collections.unmodifiableList(names);
	}
	
	public String getFirstName() {
		if (names.isEmpty()) {
			return "";
		}
		return names.get(0);
	}
	
	public String getLastName() {
		// if there's only one name, it's considered the first name
		if (names.size() < 2) {
			return "";
		}
		return names.get(names.size() - 1);
	}
	
	public String getMiddleName() {
		// This returns all names that aren't first or last
		if (names.size() < 3) {
			return "";
		} else if (names.size() == 3) {
			return names.get(1);
		} else {
			return concatNames(1, names.size() - 1, false);
		}
	}
	
	public String getFullName() {
		return concatNames(0, names.size(), false);
	}
	
	@Override
	public String toString() {
		return getFullName();
	}
	
	/**
	 * Check whether a name is empty
	 * 
	 * @return true if the name has no components, or only one empty component.
	 */
	public boolean isEmpty() {
		return names.size() == 0 || (names.size() == 1 && names.get(0).equals(""));
	}
	
	/**
	 * Convert the name into an encoded string for storing
	 * in the database as a single string.
	 * 
	 * @return the database string
	 */
	public String getDatabaseString() {
		// TODO actually implement this in a non-broken way 
		// (has to escape the separator char)
		return concatNames(0, names.size(), true);
	}
	
	/**
	 * Parse the format we use to store strings in the database.
	 * 
	 * @param dbString
	 */
	public static FullName parseDatabaseString(String dbString) {
		// FIXME parse from database format (unescape)
		String[] split = dbString.split(" ");
		return new FullName(Arrays.asList(split));
	}
	
	public static FullName parseHumanString(String humanEnteredName) {
		// FIXME we could try to be intelligent or something
		String[] split = humanEnteredName.split(" ");
		return new FullName(Arrays.asList(split));
	}
	
	private String concatNames(int start, int end, boolean dbEncoding) {
		StringBuilder builder = new StringBuilder();
		for (int i = start; i < end; ++i) {
			builder.append(names.get(i));
			if (dbEncoding)
				builder.append(" "); // TODO something smarter
			else
				builder.append(" ");
		}
		// kill last space
		builder.deleteCharAt(builder.length() - 1);
		
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof FullName))
			return false;
		FullName otherName = (FullName) other;
		
		if (names == otherName.names) // catches "both null"
			return true;
		else if (names == null)
			return false;
		else if (otherName.names == null)
			return false;
		
		return names.equals(otherName.names);
	}
	
	@Override
	public int hashCode() {
		if (names == null)
			return 0;
		else
			return names.hashCode();
	}
}
