package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import com.dumbhippo.StringUtils;

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

	    /* According to one possibly-wrong web page, AIM screen name is
	     * "between 3 and 16 characters long, using letters,
	     * numbers, and spaces and must begin with a letter"
	     * We now accept ICQ ids for AIM screen names, so those begin with a number, not a letter.
	     * It's also now possible to register your e-mail as an AIM screen name, though
	     * those users can't see our bot for some reason, so can't add it to Mugshot.
	     * But we shouldn't be checking for a 16 character maximum, in case e-mail screen names start 
	     * getting added.
	     */		
		str = str.trim(); // gets tabs and stuff not just spaces
		str = str.replaceAll(" ", ""); // but suck spaces out of middle of string, not just ends
		
		if (str.length() < 3)
			throw new ValidationException("'" + str + "' is too short to be an AIM screen name");
		
	    str = str.toLowerCase();
	    
	    if (!StringUtils.isAlphanumericOrInSet(str, "@_-+."))
	        throw new ValidationException("Possibly invalid AIM screen name: '" + str + "')");
	    
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
