package com.dumbhippo.server.applications;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.Digest;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.AppinfoUpload;
import com.dumbhippo.persistence.Application;
import com.dumbhippo.persistence.ApplicationCategory;
import com.dumbhippo.persistence.ApplicationIcon;
import com.dumbhippo.persistence.ApplicationUsage;
import com.dumbhippo.persistence.ApplicationWmClass;
import com.dumbhippo.persistence.UnmatchedApplicationUsage;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.views.UserViewpoint;

@Stateless
public class ApplicationSystemBean implements ApplicationSystem {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(ApplicationSystemBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em; 
	
	@EJB
	private Configuration config;
		
	public void addUpload(Guid uploaderId, Guid uploadId, AppinfoFile appinfoFile) {
		User uploader = em.find(User.class, uploaderId.toString());
		boolean isNew = false;
		
		Application application = em.find(Application.class, appinfoFile.getAppId());
		if (application == null) {
			application = new Application(appinfoFile.getAppId());
			isNew = true;
		}
		
		updateApplication(application, appinfoFile);
		if (isNew)
			em.persist(application);
		
		updateApplicationCollections(application, appinfoFile);
		
		AppinfoUpload upload = new AppinfoUpload(uploader);
		upload.setId(uploadId.toString());
		upload.setApplication(application);
		
		em.persist(upload);
	}
	
	private void updateApplication(Application application, AppinfoFile appinfoFile) {
		application.setName(appinfoFile.getName());
		application.setDescription(appinfoFile.getDescription());
		
		application.setRawCategories(setToString(appinfoFile.getCategories()));
		application.setCategory(computeCategoryFromRaw(appinfoFile.getCategories()));
		application.setTitlePatterns(setToString(appinfoFile.getTitlePatterns()));
	}
	
	private void updateApplicationCollections(Application application, AppinfoFile appinfoFile) {
		updateWmClasses(application, appinfoFile);
		updateIcons(application, appinfoFile);
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

	private void updateWmClasses(Application application, AppinfoFile appinfoFile) {
		em.createQuery("DELETE FROM ApplicationWmClass awc WHERE awc.application= :application")
			.setParameter("application", application)
			.executeUpdate();
		
		for (String wmClass : appinfoFile.getWmClasses()) {
			ApplicationWmClass applicationWmClass = new ApplicationWmClass(application, wmClass);
			em.persist(applicationWmClass);
		}
	}
	
	private void updateIcons(Application application, AppinfoFile appinfoFile) {
		em.createQuery("DELETE FROM ApplicationIcon ai WHERE ai.application= :application")
			.setParameter("application", application)
			.executeUpdate();
	
		persistIcons(application, appinfoFile, getInterestingIcons(appinfoFile));
	}
	
	private Collection<AppinfoIcon> getInterestingIcons(AppinfoFile appinfoFile) {
		Map<Integer, AppinfoIcon> bySize = new HashMap<Integer, AppinfoIcon>();
		
		for (AppinfoIcon icon : appinfoFile.getIcons()) {
			// Only consider PNG icons
			if (!icon.getPath().endsWith(".png"))
				continue;
			
			Integer size = icon.getNominalSize();
			
			if (!bySize.containsKey(size) || betterIcon(icon, bySize.get(size))) {
				bySize.put(size, icon);
			}
		}
		
		return bySize.values();
	}
	
	private boolean betterIcon(AppinfoIcon icon, AppinfoIcon otherIcon) {
		String theme = icon.getTheme();
		String otherTheme = otherIcon.getTheme();
		
		// We consider locolor to be the worst theme, then an unspecified
		// theme. All other themes are sorted alphabetically. The reason
		// for the alphabetical sort is so that we don't mix themes 
		// unnecessarily if a appinfo file contains multiple themes.
		
		if ("locolor".equals(otherTheme)) {
			if (!"locolor".equals(theme))
				return true;
		} else if (otherTheme == null) {
			if (theme != null)
				return true;
		} else {
			if (theme == null)
				return false;
			
			if (theme.compareTo(otherTheme) < 0)
				return true;
		}
		
		return false;
	}

	private void persistIcons(Application application, AppinfoFile appinfoFile, Collection<AppinfoIcon> toPersist) {
		// This isn't actually needed because of the call to clear(), but documents the 
		// need to be careful about the situation here; we have to make sure that 
		// application.icons() is initialized before we create any ApplicationIcon
		// objects.
		application.prepareToModifyIcons();
		application.getIcons().clear();
	
		for (AppinfoIcon icon : toPersist) {
			try {
				int actualWidth, actualHeight;
				
				// Parse the icon to get it's actual dimensions, so we can generate
				// HTML that lays out without waiting for the images. Also has a side-effect
				// of validating the 
				
				InputStream istream = appinfoFile.getIconStream(icon);
				BufferedImage image = ImageIO.read(istream);
				istream.close();

				if (image == null) {
					// Theoretically, we could see if we have a usable icon in a different
					// theme, but ignore that corner case; people shouldn't upload 
					// appinfo files with bad PNGs in them anyways 
					logger.warn("Cannot read application icon {} for {}, skipping", 
								icon.getPath(), application.getId());
					continue;
				}

				actualWidth = image.getWidth();
				actualHeight = image.getHeight();
				
				String key = saveIcon(appinfoFile, icon);
				ApplicationIcon dbIcon = new ApplicationIcon(application, icon.getNominalSize(), key, actualWidth, actualHeight);
				em.persist(dbIcon);
				application.getIcons().add(dbIcon);
			} catch (IOException e) {
				logger.warn("Cannot save application icon {} for {}, skipping: {}", 
							new Object[] { icon.getPath(), application.getId(), e.getMessage() });
			}
		}
	}
	
	private String saveIcon(AppinfoFile appinfoFile, AppinfoIcon icon) throws IOException {
		InputStream istream; 
		byte[] buffer = new byte[16384];
		
		MessageDigest md = Digest.newDigest();
		
		// First compute the hash of the icons contents that we use to identify it
		istream = appinfoFile.getIconStream(icon);
		
		while (true) {
			int count = istream.read(buffer);
			if (count == -1)
				break;
			
			md.update(buffer, 0, count);
		}
		
		istream.close();
		
		String key = Digest.digest(md);
		
		// Now save the icon, if it isn't already there
		
		File saveFile = getIconFile(key);
		if (!saveFile.exists()) {
			istream = appinfoFile.getIconStream(icon);
			
			OutputStream ostream = new FileOutputStream(saveFile);
			
			try {
				while (true) {
					int count = istream.read(buffer);
					if (count == -1)
						break;
					
					ostream.write(buffer, 0, count);
				}
				
				ostream.close();
			} catch (IOException e) {
				ostream.close();
				saveFile.delete();
			} finally {
				istream.close();
			}
		}
		
		return key;
	}
	
	private File getIconFile(String key) {
		return new File(getIconsDir(), key + ".png");
	}
	
	private File getIconsDir() {
		String filesUrl = config.getPropertyFatalIfUnset(HippoProperty.FILES_SAVEURL);
		String saveUrl = filesUrl + Configuration.APPICONS_RELATIVE_PATH;
		
		URI saveUri;
		try {
			saveUri = new URI(saveUrl);
		} catch (URISyntaxException e) {
			throw new RuntimeException("save url busted", e);
		}
		
		return new File(saveUri);
	}

	private ApplicationIconView getIcon(Application application, int desiredSize) throws NotFoundException {
		ApplicationIcon unsizedIcon = null;
		ApplicationIcon sizedIcon = null;
		
		// We try to fine the icon with the nominal size closest
		// to the desired size, except that we always prefer a
		// too big icon to a too small icon.
		for (ApplicationIcon icon : application.getIcons()) {
			int iconSize = icon.getSize();
			if (iconSize == -1) {
				unsizedIcon = icon;
			} else if (sizedIcon == null) {
				sizedIcon = icon;
			} else {
				int oldSize = sizedIcon.getSize();
				if (oldSize < desiredSize && iconSize >= desiredSize)
					sizedIcon = icon;
				else if (Math.abs(oldSize - desiredSize) > Math.abs(iconSize - desiredSize))
					sizedIcon = icon;
			}
		}
		
		ApplicationIcon icon;
		
		if (sizedIcon == null) {
			if (unsizedIcon == null)
				throw new NotFoundException("No icons for application");

			icon = unsizedIcon;
		} else {
			// We really don't want to scale up, so if we're going to
			// use a smaller size, we double check to see if we have a
			// icon without a specified nominal size that is actually
			// larger than the desired size.
			if (sizedIcon.getSize() < desiredSize &&
				unsizedIcon != null &&
				unsizedIcon.getActualHeight() >= desiredSize &&
				unsizedIcon.getActualWidth() >= desiredSize)
				icon = unsizedIcon;
			else
				icon = sizedIcon;
		}
			
		return new ApplicationIconView(icon, desiredSize);
	}

	public ApplicationIconView getIcon(String appId, int desiredSize) throws NotFoundException {
		Application application = em.find(Application.class, appId);
		if (application == null)
			throw new NotFoundException("No such application");
		
		return getIcon(application, desiredSize);
	}

	public void recordApplicationUsage(UserViewpoint viewpoint, Collection<ApplicationUsageProperties> usages) {
		// We currently record usage by looping over each reported application usage one-by-one,
		// but it would probably be more efficient to retrieve all applications that the user
		// used within the last day, compare them with the new usage set, and persist anything
		// new.
		Date now = new Date();
		for (ApplicationUsageProperties props : usages) {
			recordApplicationUsage(viewpoint, props, now);
		}
	}

	private void recordApplicationUsage(UserViewpoint viewpoint, ApplicationUsageProperties props, Date date) {
		if (props.getAppId() != null) {
			Application application = em.find(Application.class, props.getAppId());
			if (application == null) {
				logger.warn("Got application usage report for an unknown application ID '{}', ignoring", props.getAppId());
			}
			recordApplicationUsage(viewpoint, application, date);
		} else if (props.getWmClass() != null) {
			Query q = em.createQuery("SELECT awc.application FROM ApplicationWmClass awc WHERE awc.wmClass = :wmClass")
				.setParameter("wmClass", props.getWmClass());
			
			try {
				Application application = (Application)q.getSingleResult();
				recordApplicationUsage(viewpoint, application, date);
			} catch (NoResultException e) {
				recordUnmatchedApplicationUsage(viewpoint, props, date);
			}
		} else {
			logger.warn("Application usage report doesn't include either an application ID or a window class, ignoring");
		}
	}

	private void recordApplicationUsage(UserViewpoint viewpoint, Application application, Date date) {
		Date oneDayAgo = new Date(date.getTime() - 24 * 60 * 60 * 1000L);
		
		Query q = em.createQuery("SELECT usage FROM ApplicationUsage usage " +
								 "   WHERE usage.user = :user " +
								 "     AND usage.application = :application " +
								 "     AND usage.date > :oneDayAgo")
			.setParameter("user", viewpoint.getViewer())
			.setParameter("application", application)
			.setParameter("oneDayAgo", oneDayAgo);
		
		if (q.getResultList().isEmpty()) {
			ApplicationUsage usage = new ApplicationUsage(viewpoint.getViewer(), application, date);
			em.persist(usage);
		}
	}
	
	private void recordUnmatchedApplicationUsage(UserViewpoint viewpoint, ApplicationUsageProperties props, Date date) {
		Date oneDayAgo = new Date(date.getTime() - 24 * 60 * 60 * 1000L);
		
		Query q = em.createQuery("SELECT usage FROM UnmatchedApplicationUsage usage " +
								 "   WHERE usage.user = :user " +
								 "     AND usage.wmClass = :wmClass " +
								 "     AND usage.date > :oneDayAgo")
			.setParameter("user", viewpoint.getViewer())
			.setParameter("wmClass", props.getWmClass())
			.setParameter("oneDayAgo", oneDayAgo);
		
		if (q.getResultList().isEmpty()) {
			UnmatchedApplicationUsage usage = new UnmatchedApplicationUsage(viewpoint.getViewer(), props.getWmClass(), date);
			em.persist(usage);
		}
	}
	
	public void pagePopularApplications(Date since, int iconSize, ApplicationCategory category, Pageable<ApplicationView> pageable) {
		Query q = em.createNamedQuery("applicationsPopularSince")
			.setParameter("since", since);
		List<?> results = q.getResultList();

		List<ApplicationView> applicationViews = new ArrayList<ApplicationView>();
		
		int rank = 1;
		int pos = 0;
		for (Object o : results) {
			int thisRank = rank++;
			Object[] columns = (Object[])o;
			Application application = (Application)columns[0];
			Number count = (Number)columns[1];

			if (category != null && application.getCategory() != category)
				continue;
			
			if (pos >= pageable.getStart() && pos < pageable.getStart() + pageable.getCount()) {
				ApplicationView applicationView = new ApplicationView(application);
				applicationView.setUsageCount(count.intValue());
				applicationView.setRank(thisRank);
				try {
					applicationView.setIcon(getIcon(application, iconSize));
				} catch (NotFoundException e) {
					// FIXME: Default icon (maybe in getIcon())
				}
			
				applicationViews.add(applicationView);
			}
			
			pos++;
		}

		pageable.setResults(applicationViews);
		pageable.setTotalCount(pos);
	}
	
	public List<CategoryView> getPopularCategories(Date since) {
		Map<ApplicationCategory, Integer> usageCounts = new HashMap<ApplicationCategory, Integer>();
		
		Query q = em.createQuery(" SELECT a.category, COUNT(*) " +
								 "   FROM ApplicationUsage au, Application a " +
								 "   WHERE au.application = a AND au.date > :since " +
								 "GROUP BY a.category")
			.setParameter("since", since);
		
		for (Object o : q.getResultList()) {
			Object[] columns = (Object[])o;
			ApplicationCategory category = (ApplicationCategory)columns[0];
			Number count = (Number)columns[1];
			
			usageCounts.put(category, count.intValue());
		}
		
		List<CategoryView> categoryViews = new ArrayList<CategoryView>();
		for (ApplicationCategory category : ApplicationCategory.values()) {
			int usageCount;
			if (usageCounts.containsKey(category))
				usageCount = usageCounts.get(category);
			else
				usageCount = 0;
			
			CategoryView categoryView = new CategoryView(category);
			categoryView.setUsageCount(usageCount);
			categoryViews.add(categoryView);
		}
		
		return categoryViews;
	}
}
