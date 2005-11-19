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
	 * @throws IllegalArgumentException
	 */
	private String filterName(String str) throws IllegalArgumentException {
		if (str == null)
			return null;
		
		str = str.replaceAll(" ", "");
		
		if (str.length() > 16)
			throw new IllegalArgumentException("AIM name too long: " + str);
		if (str.length() < 3)
			throw new IllegalArgumentException("AIM name too short: " + str);
		
	    str = str.toLowerCase();
	    
	    /* According to one possibly-wrong web page,
	     * "between 3 and 16 characters long, using letters,
	     * numbers, and spaces and must begin with a letter"
	     */
	    char[] chars = str.toCharArray();
	    for (char c : chars) {
	    	// FIXME I bet AOL only allows ASCII, but this checks unicode
	    	if (!Character.isLetterOrDigit(c))
	    		throw new IllegalArgumentException("Invalid char in AIM name: " + str);
	    }
	    if (!Character.isLetter(str.charAt(0)))
	    	throw new IllegalArgumentException("AIM address starts with nonletter: " + str);
	    
	    return str;
	}
	
	protected AimResource() {}

	public AimResource(String screenName) {
		setScreenName(screenName);
	}
	
	@Column(unique=true, nullable=false)
	public String getScreenName() {
		return screenName;
	}

	protected void setScreenName(String screenName) {
		if (screenName != null)
			screenName = filterName(screenName);
		this.screenName = screenName;
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
