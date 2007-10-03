package com.dumbhippo.jive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJB;

import org.xmpp.packet.IQ;

import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.persistence.ApplicationCategory;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.server.applications.ApplicationView;
import com.dumbhippo.server.dm.ApplicationDMO;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;

@IQHandler(namespace=ApplicationsIQHandler.APPLICATIONS_NAMESPACE)
public class ApplicationsIQHandler extends AnnotatedIQHandler {
	static final String APPLICATIONS_NAMESPACE = "http://online.gnome.org/p/application";
	
	@EJB
	private ApplicationSystem applicationSystem;
	
	public ApplicationsIQHandler() {
		super("Hippo application usage IQ Handler");
	}	

	/**
	 * Set whether a single app ID is pinned
	 *   
	 * @param viewpoint
	 * @param appId
	 * @param pinned
	 * @throws IQException
	 * @throws RetryException 
	 */
	@IQMethod(name="setPinned", type=IQ.Type.set)
	@IQParams({ "appId", "pinned" })
	public void setPinned(UserViewpoint viewpoint, String appId, boolean pinned) throws IQException, RetryException {
		applicationSystem.pinApplicationId(viewpoint.getViewer(), appId, pinned);
	}
	
	private List<ApplicationDMO> getResults(Pageable<ApplicationView> pageable) {
		// FIXME this is wasteful; we create ApplicationView then just get the IDs from 
		// the views and use them to create an ApplicationDMO, which itself has an ApplicationView
		// internally
		
		DataModel model = DataService.getModel();
		DMSession session = model.currentSessionRO();
		
		List<ApplicationDMO> result = new ArrayList<ApplicationDMO>();
		
		for (ApplicationView appView : pageable.getResults()) {
			result.add(session.findUnchecked(ApplicationDMO.class, appView.getApplication().getId()));
		}
		
		return result;		
	}
	
	private List<ApplicationDMO> getResults(Collection<String> appIds) {
		DataModel model = DataService.getModel();
		DMSession session = model.currentSessionRO();
		
		List<ApplicationDMO> result = new ArrayList<ApplicationDMO>();
		
		for (String id : appIds) {
			result.add(session.findUnchecked(ApplicationDMO.class, id));
		}
		
		return result;		
	}
	
	/** 
	 * Gets globally popular applications (the per-user popular apps are a property on UserDMO)
	 * 
	 * @param viewpoint
	 * @param start
	 * @return
	 * @throws IQException
	 */
	@IQMethod(name="getPopularApplications", type=IQ.Type.get)
	@IQParams({ "start", "category", "distribution", "lang" })
	public Collection<ApplicationDMO> getPopularApplications(UserViewpoint viewpoint, int start, String category, String distribution, String lang) throws IQException {
		if (start < 0 || start > 4096) // Arbitrary but reasonable limit
			start = 0;
				
		Pageable<ApplicationView> pageable = new Pageable<ApplicationView>("applications");
		pageable.setPosition(start);
		pageable.setInitialPerPage(30);
		
		ApplicationCategory cat = null;
		if (category != null) {
			for (ApplicationCategory c : ApplicationCategory.values()) {
				if (c.getDisplayName().equals(category)) {
					cat = c;
					break;
				}
			}
			if (cat == null)
				cat = ApplicationCategory.fromRaw(Collections.singleton(category));
		}
		applicationSystem.pagePopularApplications(null, -1, cat, pageable);
		
		return getResults(pageable);
	}
	
	/** 
	 * Searches applications globally (not per-user)
	 * 
	 * @param viewpoint
	 * @param start
	 * @return
	 * @throws IQException
	 */
	@IQMethod(name="searchApplications", type=IQ.Type.get)
	@IQParams({ "start", "search", "distribution", "lang" })
	public Collection<ApplicationDMO> searchApplications(UserViewpoint viewpoint, int start, String search, String distribution, String lang) throws IQException {
		if (start < 0 || start > 4096) // Arbitrary but reasonable limit
			start = 0;

		Pageable<ApplicationView> pageable = new Pageable<ApplicationView>("applications");
		pageable.setPosition(start);
		pageable.setInitialPerPage(30);		
		applicationSystem.search(search, -1, null, pageable);
		
		return getResults(pageable);
	}
	
	/**
	 * Gets all known application objects
	 * 
	 * @param viewpoint
	 * @param distribution
	 * @param lang
	 * @return
	 * @throws IQException
	 */
	@IQMethod(name="getAllApplications", type=IQ.Type.get)
	@IQParams({ "distribution", "lang" })
	public Collection<ApplicationDMO> getAllApplications(UserViewpoint viewpoint, String distribution, String lang) throws IQException {

		Collection<String> appIds = applicationSystem.getAllApplicationIds(distribution, lang);
		
		return getResults(appIds);
	}
}
