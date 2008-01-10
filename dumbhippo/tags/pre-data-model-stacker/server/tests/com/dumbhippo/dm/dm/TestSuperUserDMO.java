package com.dumbhippo.dm.dm;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.persistence.TestSuperUser;
import com.dumbhippo.identity20.Guid;

// FIXME: This is not actually valid inheritance since it must be possible to
// determine the class in the inheritance heirarchy from the key; but it works
// for what we are testing at the moment because we explicitly 
// em.find(TestSuperUser.DMO.class)

@DMO(classId="http://mugshot.org/p/o/test/superUser")
public abstract class TestSuperUserDMO extends TestUserDMO {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(TestSuperUserDMO.class);

	protected TestSuperUserDMO(Guid key) {
		super(key);
	}

	@DMProperty(defaultInclude=true)
	public String getSuperPower() {
		return ((TestSuperUser)user).getSuperPower();
	}
	
	@Override
	public String getName() {
		return "*" + super.getName() + "*";
	}
}
