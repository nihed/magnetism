package com.dumbhippo.persistence;

import java.net.MalformedURLException;
import java.net.URL;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class OnlineAccountType extends DBUnique {
		
	private static final long serialVersionUID = 1L;
	
	private ExternalAccountType accountType;
	private String name; // must not have spaces
	private String fullName;
	private String siteName;
	private String site;
	private String userInfoType;
	private User creator;
	// account types that should not show up on online.gnome.org should be marked as not supported
	private boolean supported;
	
	
	protected OnlineAccountType() {
		supported = true;
	}
	
	public OnlineAccountType(String name, String fullName, String siteName, String site, String userInfoType) throws ValidationException {
		this();
		setName(name);
		setFullName(fullName);
		setSiteName(siteName);
		setSite(site);
		setUserInfoType(userInfoType);
	}
	
	@Column(nullable=true, unique=true)
	public ExternalAccountType getAccountType() {
		return accountType;
	}
	
	public void setAccountType(ExternalAccountType type) {
		this.accountType = type;
	}
	
	@Column(nullable=false, unique=true)
	public String getName() {
		return name;
	}
	
	public void setName(String name) throws ValidationException {
		name = name.trim();
    	if (name.length() == 0) 
    		throw new ValidationException("The name can not be empty");
    	
    	for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (!((c >= 'a' && c <= 'z') || (c == '_')))
				throw new ValidationException("Account type name can only contain lower case characters and an underscore.");
		}
    	
    	// the following rules ensure uniqueness when we create DOM ids based on the names by capitalizing first character and each 
    	// character that is preceded by an underscore 
    	if (name.startsWith("_") || name.endsWith("_"))
    		throw new ValidationException("Account type name can't start or end with and underscore.");
    	
    	if (name.indexOf("__") >= 0) 
    		throw new ValidationException("Account type name can't contain multiple underscores in a row.");
    	
		this.name = name;
	}

	@Column(nullable=false, unique=true)
	public String getFullName() {
		return fullName;
	}
	
	public void setFullName(String fullName) throws ValidationException {
		fullName = fullName.trim();
    	if (fullName.length() == 0) 
    		throw new ValidationException("The full name can not be empty");
		this.fullName = fullName;
	}

	@Column(nullable=false, unique=false)
	public String getSiteName() {
		return siteName;
	}
	
	public void setSiteName(String siteName) throws ValidationException {
		siteName = siteName.trim();
    	if (siteName.length() == 0) 
    		throw new ValidationException("The site name can not be empty");
    	
		this.siteName = siteName;
	}
	
	@Column(nullable = false)
	public String getSite() {
		return site;
	}
	
	public void setSite(String site) throws ValidationException {
		site = site.trim();
		
		if (!site.startsWith("http"))
			site = "http://" + site;	
  	
		try {
			@SuppressWarnings("unused")
			URL url = new URL(site);
		} catch (MalformedURLException e) {
			throw new ValidationException("site url " + site + " is malformed");
		}
		
		this.site = site;
	}

	@Column(nullable=false, unique=true)
	public String getUserInfoType() {
		return userInfoType;
	}
	
	public void setUserInfoType(String userInfoType) throws ValidationException {
        userInfoType = userInfoType.trim();
		if (userInfoType.length() == 0) 
    		throw new ValidationException("The site user info type can not be empty");
    	
		this.userInfoType = userInfoType;
	}
	
	@ManyToOne
	@JoinColumn(nullable = true)
	public User getCreator() {
		return creator;
	}
	
	public void setCreator(User creator) {
		this.creator = creator;
	}

	@Column(nullable=false)
	public boolean isSupported() {
		return supported;
	}

	public void setSupported(boolean supported) {
		this.supported = supported;
	}
	
	@Override
	public String toString() {
		return "OnlineAccountType " + name;
	}
}
