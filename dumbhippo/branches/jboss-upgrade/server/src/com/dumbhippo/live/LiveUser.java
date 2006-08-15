package com.dumbhippo.live;

import java.util.Collections;
import java.util.Set;

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
	private Set<Guid> contactResources; 

	private int groupCount;
	private int sentPostsCount;

	LiveUser(Guid userId) {
		super(userId);
		this.groupCount = 0;
		this.sentPostsCount = 0;
	}
		
	public void setContactResources(Set<Guid> resources) {
		this.contactResources = resources;
	}
	
	public Set<Guid> getContactResources() {
		return Collections.unmodifiableSet(contactResources);
	}
	
	public boolean hasContactResource(Guid resourceId) {
		return contactResources.contains(resourceId);
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
}
