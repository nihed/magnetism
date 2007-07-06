package com.dumbhippo.server.dm;

import javax.ejb.EJB;
import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.persistence.DesktopSetting;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.DesktopSettings;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/setting", resourceBase="/o/setting")
public abstract class DesktopSettingDMO extends DMObject<DesktopSettingKey> {
	private DesktopSetting setting;
	
	@EJB
	private DesktopSettings settings;
	
	@Inject
	private EntityManager em;
			
	public DesktopSettingDMO(DesktopSettingKey key) {
		super(key);
	}

	@Override
	protected void init() throws NotFoundException {
		DesktopSettingKey key = getKey();
		
		long id = key.getId();
		if (id >= 0) {
			// this would fail if setting was deleted, not sure if that is possible
			// but afaik there is no machinery to invalidate the id cached in the key
			setting = em.find(DesktopSetting.class, id);
		}
		if (setting == null) {
			User user = em.find(User.class, key.getUserId().toString());
			if (user == null)
				throw new NotFoundException("No such user");
			
			// throws NotFoundException if appropriate
			setting = settings.getSettingObject(user, key.getKeyName());
		}
	}
		
	@DMProperty(defaultInclude=true)
	public String getKeyName() {
		return setting.getKeyName();
	}

	@DMProperty(defaultInclude=true)
	public String getValue() {
		return setting.getValue();
	}
}
