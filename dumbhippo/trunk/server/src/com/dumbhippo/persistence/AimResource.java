package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
public class AimResource extends Resource {
	private static final long serialVersionUID = 0L;
	
	private String screenName;
	
	/**
	 * AIM addresses have this idea of "normalization" where you can 
	 * type them with spaces and caps, but they get normalized before 
	 * sending the IM. We probably have to figure out how to keep the 
	 * non-normalized form for display, eventually.
	 * 
	 * @param str the name to validate and normalize
	 * @return normalized version
	 * @throws ValidationException
	 */
	public static String canonicalize(String str) throws ValidationException {
		if (str == null)
			return null;
		
		str = str.trim(); // gets tabs and stuff not just spaces
		str = str.replaceAll(" ", ""); // but suck spaces out of middle of string, not just ends
		
		if (str.length() > 16)
			throw new ValidationException("'" + str + "' is too long to be an AIM screen name");
		if (str.length() < 3)
			throw new ValidationException("'" + str + "' is too short to be an AIM screen name");
		
	    str = str.toLowerCase();
	    
	    /* According to one possibly-wrong web page,
	     * "between 3 and 16 characters long, using letters,
	     * numbers, and spaces and must begin with a letter"
	     */
	    char[] chars = str.toCharArray();
	    for (char c : chars) {
	    	// FIXME I bet AOL only allows ASCII, but this checks unicode
	    	if (!Character.isLetterOrDigit(c))
	    		throw new ValidationException("'" + c + "' isn't allowed in an AIM screen name (you typed '" + str + "')");
	    }
	    if (!Character.isLetter(str.charAt(0)))
	    	throw new ValidationException("AIM screen names have to start with a letter (you typed '" + str + "')");
	    
	    return str;
	}
	
	protected AimResource() {}

	public AimResource(String screenName) throws ValidationException {
		internalSetScreenName(screenName);
	}
	
	@Column(unique=true, nullable=false)
	public String getScreenName() {
		return screenName;
	}

	private void internalSetScreenName(String screenName) throws ValidationException {
		if (screenName != null) {
			screenName = canonicalize(screenName);
		}
		this.screenName = screenName;
	}
	
	protected void setScreenName(String screenName) {
		try {
			internalSetScreenName(screenName);
		} catch (ValidationException e) {
			throw new RuntimeException("Database contained invalid screen name", e);
		}
	}

	@Override
	@Transient
	public String getHumanReadableString() {
		return screenName;
	}
	
	@Override
	@Transient
	public String getDerivedNickname() {
		return getHumanReadableString();
	}
}
