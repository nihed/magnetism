package com.dumbhippo.server.dm;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.GroupFeed;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/feed", resourceBase="/o/feed")
@DMFilter("viewer.canSeeFeed(this)")
public abstract class FeedDMO extends DMObject<Guid> {
	private GroupFeed groupFeed;
	
	@Inject
	private EntityManager em;
	
	@Inject
	private DMSession session;
	
	protected FeedDMO(Guid key) {
		super(key);
	}

	@Override
	public void init() throws NotFoundException {
		groupFeed = em.find(GroupFeed.class, getKey().toString());
		if (groupFeed == null)
			throw new NotFoundException("No such group feed");
	}
	
	@DMProperty(defaultInclude=true)
	public String getName() {
		return groupFeed.getFeed().getTitle();
	}

	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getHomeUrl() {
		return groupFeed.getFeed().getLink().getUrl();
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getPhotoUrl() {
		return groupFeed.getGroup().getPhotoUrl();
	}

	@DMProperty
	public StoreKey<?,?> getVisibilityDelegate() {
		return session.findUnchecked(GroupDMO.class, groupFeed.getGroup().getGuid()).getStoreKey();
	}
}
