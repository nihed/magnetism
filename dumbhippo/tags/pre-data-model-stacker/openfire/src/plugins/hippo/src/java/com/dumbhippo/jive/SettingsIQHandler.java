package com.dumbhippo.jive;

import javax.ejb.EJB;

import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.persistence.DesktopSetting;
import com.dumbhippo.server.DesktopSettings;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.dm.DesktopSettingDMO;
import com.dumbhippo.server.dm.DesktopSettingKey;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;

/** 
 * IQ handler for getting your desktop settings; this is the new version based on data model.
 * 
 * @author Havoc Pennington
 *
 */
@IQHandler(namespace=SettingsIQHandler.SETTINGS_NAMESPACE)
public class SettingsIQHandler extends AnnotatedIQHandler {
	static final String SETTINGS_NAMESPACE = "http://online.gnome.org/p/settings";
	
	@EJB
	private DesktopSettings settings;
	
	public SettingsIQHandler() {
		super("Hippo settings IQ Handler");
		Log.debug("creating SettingsIQHandler");
	}
	
	/* To get a single setting, use this... but there's probably no need,  
	 * to get all settings for a user, there's a property on UserDMO, which is 
	 * the more efficient approach unless you will truly only need the one setting ever.
	 * Also there's no change notification if you get a single setting.
	 */
	@IQMethod(name="getSetting", type=IQ.Type.get)
	@IQParams({ "key" })
	public DesktopSettingDMO getSetting(UserViewpoint viewpoint, String key) throws IQException {
		DesktopSetting setting;
		try {
			setting = settings.getSettingObject(viewpoint.getViewer(), key);
		} catch (NotFoundException e) {
			throw new IQException(PacketError.Condition.item_not_found, PacketError.Type.cancel, e.getMessage());
		}
		DataModel model = DataService.getModel();
		DMSession session = model.currentSessionRO();

		return session.findUnchecked(DesktopSettingDMO.class, new DesktopSettingKey(setting));
	}
	
	@IQMethod(name="setSetting", type=IQ.Type.set)
	@IQParams({ "key", "value" })
	public void setSetting(UserViewpoint viewpoint, String key, String value) throws IQException, RetryException {				
		settings.setSetting(viewpoint.getViewer(), key, value);
	}
	
	@IQMethod(name="unsetSetting", type=IQ.Type.set)
	@IQParams({ "key" })
	public void unsetSetting(UserViewpoint viewpoint, String key) throws IQException, RetryException {				
		settings.setSetting(viewpoint.getViewer(), key, null);
	}
}
