package com.dumbhippo.server.dm;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/group", resourceBase="/o/group")
@DMFilter("viewer.canSeeGroup(this)")
public abstract class GroupDMO extends DMObject<Guid> {
	@Inject
	private EntityManager em;
	
	private Group group;
	
	protected GroupDMO(Guid key) {
		super(key);
	}

	@Override
	protected void init() throws NotFoundException {
		group = em.find(Group.class, getKey().toString());
		if (group == null)
			throw new NotFoundException("No such group");
	}
	
	@DMProperty(defaultInclude=true)
	public String getName() {
		return group.getName();
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getPhotoUrl() {
		return group.getPhotoUrl();
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getHomeUrl() {
		return "/group?who=" + group.getId();
	}
}
