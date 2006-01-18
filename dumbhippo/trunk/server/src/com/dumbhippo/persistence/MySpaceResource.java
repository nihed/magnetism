package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
public class MySpaceResource extends Resource {
	private static final long serialVersionUID = 1L;
	private String mySpaceName;
	
	protected MySpaceResource() {}
	
	@Override
	@Transient
	public String getHumanReadableString() {
		// TODO would it be useful to implement this?
		return null;
	}

	@Override
	@Transient
	public String getDerivedNickname() {
		return mySpaceName;
	}
	
	public MySpaceResource(String mySpaceName) {
		setMySpaceName(mySpaceName);
	}
	
	@Column(unique=true, nullable=false)
	public String getMySpaceName() {
		return mySpaceName;
	}

	public void setMySpaceName(String name) {
		// Last ditch exceptions
		if (name.length() > 40)
			throw new IllegalArgumentException("MySpace name too long: " + name);
		if (!name.matches("^[\\p{Alnum}]+$"))
			throw new IllegalArgumentException("Invalid MySpace name");
		// Can't use em.merge as it throws an exception...		
		this.mySpaceName = name;
	}
}
