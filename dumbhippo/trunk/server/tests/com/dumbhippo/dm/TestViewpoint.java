package com.dumbhippo.dm;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.dumbhippo.dm.dm.TestGroupDMO;
import com.dumbhippo.dm.dm.TestUserDMO;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

public class TestViewpoint implements DMViewpoint {
	private Guid viewerId;
	private Collection<Guid> buddies;
	private Collection<Guid> enemies;
	private DMSession session;
	
	TestViewpoint(Guid viewerId) {
		this.viewerId = viewerId;
		this.buddies = Collections.emptySet();
		this.enemies = Collections.emptySet();
	}
	
	TestViewpoint(Guid viewerId, Collection<Guid> buddies) {
		this.viewerId = viewerId;
		this.buddies = buddies;
		this.enemies = Collections.emptySet();
	}
	
	TestViewpoint(Guid viewerId, Collection<Guid> buddies, Collection<Guid> enemies) {
		this.viewerId = viewerId;
		this.buddies = buddies;
		this.enemies = enemies;
	}

	public Guid getViewerId() {
		return viewerId;
	}
	
	public boolean sameAs(Guid id) {
		return viewerId.equals(id);
	}
	
	public boolean isBuddy(Guid id) {
		return buddies.contains(id);
	}
	
	public boolean isEnemy(Guid id) {
		return enemies.contains(id);
	}

	public boolean canSeeGroup(Guid groupId) {
		if (viewerId == null) // system viewpoint
			return true;
		
		// First if check if the group is secret. We use getRawProperty() because we
		// we can't create a TestGroupDMO object when checking the visibility rule
		// for that same object!
		try {
			if (!(Boolean)session.getRawProperty(TestGroupDMO.class, groupId, "secret"))
				return true;
		} catch (NotFoundException e) {
			return false;
		}
		
		// It's a secret group, we now have to check whether we are a member
		// We must again use getRawProperty() because creating TestGroupDMO objects
		// agains causes filtering involving canSeeGroup.
		try {
			@SuppressWarnings("unchecked")
			Set<Guid> groupIds = (Set<Guid>)session.getRawProperty(TestUserDMO.class, viewerId, "groups");
			
			return groupIds.contains(groupId);
		} catch (NotFoundException e) {
			return false;
		}
	}
	
	public void setSession(DMSession session) {
		// FIXME Our tests reuse the same viewpoints across multiple sessions 
//		if (this.session != null)
//			throw new RuntimeException("Session for viewpoint already set");
		
		this.session = session;
	}
}
