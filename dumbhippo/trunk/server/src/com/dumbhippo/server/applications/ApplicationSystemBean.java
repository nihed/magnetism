package com.dumbhippo.server.applications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.AppinfoUpload;
import com.dumbhippo.persistence.Application;
import com.dumbhippo.persistence.ApplicationCategory;
import com.dumbhippo.persistence.ApplicationWmClass;
import com.dumbhippo.persistence.User;

@Stateless
public class ApplicationSystemBean implements ApplicationSystem {
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em; 
		
	public void addUpload(Guid uploaderId, Guid uploadId, AppinfoFile appinfoFile) {
		User uploader = em.find(User.class, uploaderId.toString()); 
		
		Application application = findOrCreateApplication(appinfoFile.getAppId());

		updateApplication(application, appinfoFile);
		
		AppinfoUpload upload = new AppinfoUpload(uploader);
		upload.setId(uploadId.toString());
		upload.setApplication(application);
		
		em.persist(upload);
	}
	
	private void updateApplication(Application application, AppinfoFile appinfoFile) {
		application.setName(appinfoFile.getName());
		application.setDescription(appinfoFile.getDescription());
		
		StringBuilder builder = new StringBuilder();
		List<String> sortedTitlePatterns = new ArrayList<String>(appinfoFile.getTitlePatterns());
		Collections.sort(sortedTitlePatterns);
		
		for (String t : sortedTitlePatterns) {
			if (builder.length() > 0)
				builder.append(";");
			builder.append(t);
		}
		
		application.setRawCategories(setToString(appinfoFile.getCategories()));
		application.setCategory(computeCategoryFromRaw(appinfoFile.getCategories()));
		application.setTitlePatterns(setToString(appinfoFile.getTitlePatterns()));
		
		em.createQuery("DELETE FROM ApplicationWmClass WHERE application= :application")
			.setParameter("application", application)
			.executeUpdate();
		
		for (String wmClass : appinfoFile.getWmClasses()) {
			ApplicationWmClass applicationWmClass = new ApplicationWmClass(application, wmClass);
			em.persist(applicationWmClass);
		}
	}
	
	private ApplicationCategory computeCategoryFromRaw(Set<String> rawCategories) {
		for (ApplicationCategory category : ApplicationCategory.values()) {
			boolean found = false;
			boolean foundNot = false;
			
			for (String rc : category.getRawCategories()) {
				if (rc.charAt(0) == '!' && rawCategories.contains(rc.substring(1)))
					foundNot = true;
				else if (rawCategories.contains(rc))
					found = true;
			}
			
			if (found && !foundNot)
				return category;
		}
		
		return ApplicationCategory.OTHER;
	}
	
	private String setToString(Set<String> set) {
		StringBuilder builder = new StringBuilder();
		List<String> sortedElements = new ArrayList<String>(set);
		Collections.sort(sortedElements);
		
		for (String t : sortedElements) {
			if (builder.length() > 0)
				builder.append(";");
			builder.append(t);
		}
		
		return builder.toString();
	}

	private Application findOrCreateApplication(String appId) {
		Application application = em.find(Application.class, appId);
		if (application == null) {
			application = new Application(appId);
			em.persist(application);
		}
		
		return application;
	}
}
