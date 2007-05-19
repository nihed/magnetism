package com.dumbhippo.dm.dm;

import javax.ejb.NoSuchEntityException;
import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.persistence.TestGroupMember;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/test/groupMember", resourceBase="/o/test/groupMember")
public abstract class TestGroupMemberDMO extends DMObject<TestGroupMemberKey> {
	@Inject
	EntityManager em;
	
	@Inject
	DMSession session;

	TestGroupMember groupMember;
	
	protected TestGroupMemberDMO(TestGroupMemberKey key) {
		super(key);
	}

	@Override
	protected void init() throws NotFoundException {
		try {
			groupMember = (TestGroupMember)
				em.createQuery("SELECT gm from TestGroupMember gm WHERE gm.group.id = :groupId AND gm.member.id = :memberId")
					.setParameter("groupId", getKey().getGroupId().toString())
					.setParameter("memberId", getKey().getMemberId().toString())
					.getSingleResult();
		} catch (NoSuchEntityException e) {
			throw new NotFoundException("No such group member");
		}
	}
	
	@DMProperty
	public TestGroupDMO getGroup() {
		return session.findMustExist(TestGroupDMO.class, groupMember.getGroup().getGuid());
	}

	@DMProperty(defaultChildren="+")
	public TestUserDMO getMember() {
		return session.findMustExist(TestUserDMO.class, groupMember.getMember().getGuid());
	}
	
	@DMProperty
	public boolean isRemoved() {
		return groupMember.isRemoved(); 
	}
}
