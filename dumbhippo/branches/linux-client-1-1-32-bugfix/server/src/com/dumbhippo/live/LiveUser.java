package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * The LiveUser object represents current information about a
 * user. The set of information potentially exposed here includes 
 * both information that can be computed from the database, and 
 * information that is a function of the transient state of the
 * server. (Such as whether the user is available.)
 * 
 * @author otaylor
 */
public class LiveUser extends LiveObject {
	private int groupCount;
	private int sentPostsCount;
	private int contactsCount;
	private int userContactsCount;
	
	LiveUser(Guid userId) {
		super(userId);
		this.groupCount = 0;
		this.sentPostsCount = 0;
		this.contactsCount = 0;
		this.userContactsCount = 0;
	}
		
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	public int getGroupCount() {
		return groupCount;
	}

	public void setGroupCount(int groupCount) {
		this.groupCount = groupCount;
	}

	public int getSentPostsCount() {
		return sentPostsCount;
	}

	public void setSentPostsCount(int sentPosts) {
		this.sentPostsCount = sentPosts;
	}
	
	public int getContactsCount() {
		return contactsCount;
	}

	public void setContactsCount(int contactsCount) {
		this.contactsCount = contactsCount;
	}

	public int getUserContactsCount() {
		return userContactsCount;
	}

	public void setUserContactsCount(int userContactsCount) {
		this.userContactsCount = userContactsCount;
	}
}
