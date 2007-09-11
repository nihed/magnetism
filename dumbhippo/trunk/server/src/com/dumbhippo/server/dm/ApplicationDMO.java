package com.dumbhippo.server.dm;

import javax.ejb.EJB;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.server.applications.ApplicationView;

/* The defaultInclude for the app object is based on the idea that to just display an icon 
 * and launch the app you can use the default props, but for app browsing/installing/usage-tracking you 
 * need to get more props.
 */
@DMO(classId="http://online.gnome.org/p/o/application", resourceBase="/o/application")
public abstract class ApplicationDMO extends DMObject<String> {
	private ApplicationView applicationView;
	
	@EJB
	private ApplicationSystem appSystem;
	
	protected ApplicationDMO(String key) {
		super(key);
	}
	
	@Override
	protected void init() throws NotFoundException {
		applicationView = appSystem.getApplicationView(getKey(), -1);
		if (applicationView == null)
			throw new NotFoundException("No such application");
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getIconUrl() {
		return applicationView.getIconUrl();
	}
	
	@DMProperty(defaultInclude=true)
	public String getName() {
		return applicationView.getApplication().getName();
	}
	
	@DMProperty(defaultInclude=true)
	public String getGenericName() {
		return applicationView.getApplication().getGenericName();
	}

	@DMProperty(defaultInclude=true)
	public String getTooltip() {
		return applicationView.getApplication().getTooltip();
	}
	
	@DMProperty(defaultInclude=false)
	public String getDescription() {
		return applicationView.getApplication().getDescription();
	}
	
	@DMProperty(defaultInclude=false)
	public String getCategory() {
		// getName() is enumvalue.name().toLowerCase()
		return applicationView.getApplication().getCategory().getName();
	}
	
	@DMProperty(defaultInclude=false)
	public String getCategoryDisplayName() {
		return applicationView.getApplication().getCategory().getDisplayName();
	}
	
	@DMProperty(defaultInclude=false)
	public String getTitlePatterns() {
		return applicationView.getApplication().getTitlePatterns();
	}
	
	@DMProperty(defaultInclude=true)
	public String getDesktopNames() {
		return applicationView.getApplication().getDesktopNames();
	}
	
	@DMProperty(defaultInclude=true)
	public int getRank() {
		return applicationView.getApplication().getRank();
	}
	
	@DMProperty(defaultInclude=true)
	public int getUsageCount() {
		return applicationView.getApplication().getUsageCount();
	}
}

