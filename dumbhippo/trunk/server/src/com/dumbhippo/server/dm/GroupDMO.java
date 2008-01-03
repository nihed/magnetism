package com.dumbhippo.server.dm;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/group", resourceBase="/o/group")
@DMFilter("viewer.canSeeGroup(this)")
public abstract class GroupDMO extends DMObject<Guid> {
	@Inject
	private EntityManager em;
	
	@Inject
	private DMSession session;
	
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
	
	@DMProperty
	public boolean isPublic() {
		return group.isPublic();
	}

	// for visibility purposes; this will include members that have removed themselves, so
	// it shouldn't be used for display purposes.
	@DMProperty
	public Set<UserDMO> getCanSeeMembers() {
		Set<UserDMO> result = new HashSet<UserDMO>();
		
		for (GroupMember gm : group.getMembers()) {
			if (gm.getStatus().getCanSeeSecretGroup()) {
				AccountClaim accountClaim = gm.getMember().getAccountClaim();
				if (accountClaim != null)
					result.add(session.findUnchecked(UserDMO.class, accountClaim.getOwner().getGuid()));
			}
		}
		
		return result;
	}
}
