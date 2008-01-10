package com.dumbhippo.persistence;

import javax.persistence.Entity;

import com.dumbhippo.identity20.Guid;


@Entity
public abstract class Person extends GuidPersistable {

	private static final long serialVersionUID = 0L;

	private String nickname;
	
	protected Person() { 
		super();
	}

	protected Person(Guid guid) {
		super(guid);
	}

	@Override
	public String toString() {
		return "{Person " + "guid = " + getId() + " nick = " + getNickname() + "}";
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}
