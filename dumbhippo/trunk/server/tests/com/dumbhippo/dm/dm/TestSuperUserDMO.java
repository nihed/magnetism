package com.dumbhippo.dm.dm;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.persistence.TestSuperUser;
import com.dumbhippo.identity20.Guid;

@DMO(classId="http://mugshot.org/p/o/test/superUser", resourceBase="/o/test/superUser")
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
