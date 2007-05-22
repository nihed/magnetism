package com.dumbhippo.dm;

import java.util.Collection;
import java.util.Collections;

import com.dumbhippo.identity20.Guid;

public class TestViewpoint implements DMViewpoint {
	private Guid viewerId;
	private Collection<Guid> buddies;
	private Collection<Guid> enemies;
	
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
}
