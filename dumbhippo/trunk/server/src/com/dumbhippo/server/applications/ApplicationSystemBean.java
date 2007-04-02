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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

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
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.AppinfoUpload;
import com.dumbhippo.persistence.Application;
import com.dumbhippo.persistence.ApplicationCategory;
import com.dumbhippo.persistence.ApplicationIcon;
import com.dumbhippo.persistence.ApplicationUsage;
import com.dumbhippo.persistence.ApplicationUserState;
import com.dumbhippo.persistence.ApplicationWmClass;
import com.dumbhippo.persistence.UnmatchedApplicationUsage;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.XmlMethodErrorCode;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class ApplicationSystemBean implements ApplicationSystem {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(ApplicationSystemBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em; 
	
	@EJB
	private Configuration config;
	
	@EJB
	private PersonViewer personViewer;
	
	@EJB
	private TransactionRunner runner;
	
	public boolean canEditApplications(Viewpoint viewpoint) {
		if (viewpoint instanceof UserViewpoint)
			return true;
			// return identitySpider.isAdministrator(((UserViewpoint)viewpoint).getViewer());
		else
			return false;
	}
	
	private Date getDefaultSince() {
		return new Date(System.currentTimeMillis() - 31 * 24 * 60 * 60 * 1000L);
	}
		
	public void addUpload(Guid uploaderId, Guid uploadId, AppinfoFile appinfoFile, String comment) {
		User uploader = em.find(User.class, uploaderId.toString());
		boolean isNew = false;
		
		Application application = em.find(Application.class, appinfoFile.getAppId());
		if (application != null) {
			application.setDeleted(false);
		} else {
			application = new Application(appinfoFile.getAppId());
			isNew = true;
		}
		
		updateApplication(application, appinfoFile);
		if (isNew)
			em.persist(application);
		
		updateApplicationCollections(application, appinfoFile);
		
		AppinfoUpload upload = new AppinfoUpload(uploader, comment);
		upload.setId(uploadId.toString());
		upload.setApplication(application);
		upload.setInitialUpload(isNew);
		
		em.persist(upload);
		
		migrateUnmatchedUsage(application);
	}
	
	public void deleteApplication(UserViewpoint viewpoint, String applicationId, String comment) {
		Application application = em.find(Application.class, applicationId);
		application.setDeleted(true);
		
		// We need to mark the application as deleted instead of actually removing
		// it to retain referential integrity for AppinfoUpload, but we can delete
		// the wmClasses and icons for the application. (We *must* delete the
		// wmClasses to avoid problems with the unique constraints on that table.)
		deleteWmClasses(application);
		deleteIcons(application);
		
		AppinfoUpload upload = new AppinfoUpload(viewpoint.getViewer(), comment);
		upload.setApplication(application);
		upload.setDeleteApplication(true);
		
		em.persist(upload);
	}

	private File getAppinfoLocation(Guid uploadId) {
		final File appinfoDir;
		
		try {
			appinfoDir = new File(config.getPropertyNoDefault(HippoProperty.APPINFO_DIR));
		} catch (PropertyNotFoundException e) {
			throw new RuntimeException("appinfoDir property was not set in super configuration");
		}

		return new File(appinfoDir, uploadId + ".dappinfo");
	}
	
	private AppinfoFile getAppinfoFileInternal(AppinfoUpload upload) throws IOException, ValidationException {
		return new AppinfoFile(getAppinfoLocation(upload.getGuid()));
	}
	
	public AppinfoUpload getCurrentUpload(String applicationId) throws NotFoundException {
		Query q = em.createQuery("SELECT au from AppinfoUpload au " +
                " WHERE au.application.id = :applicationId " +
                " ORDER BY uploadDate DESC")
        .setParameter("applicationId", applicationId)
		.setMaxResults(1);

		try {
			return (AppinfoUpload)q.getSingleResult();
		} catch (NoResultException e) {
			throw new NotFoundException("Application not found");
		}
	}

	public AppinfoUploadView getCurrentUploadView(Viewpoint viewpoint, String applicationId) throws NotFoundException {
		AppinfoUpload upload = getCurrentUpload(applicationId);
		return new AppinfoUploadView(upload, personViewer.getPersonView(viewpoint, upload.getUploader()), true);
	}

	public AppinfoFile getAppinfoFile(Guid uploadId) throws NotFoundException {
		AppinfoUpload upload = em.find(AppinfoUpload.class, uploadId.toString());
		
		if (upload.isDeleteApplication()) {
			throw new NotFoundException("Application was deleted");
		}
		
		try {
			return getAppinfoFileInternal(upload);
		} catch (IOException e) {
			throw new RuntimeException("IO Error reading previously uploaded appinfo file", e);
		} catch (ValidationException e) {
			throw new RuntimeException("Validation error reading previously uploaded appinfo file", e);
		}
	}
	
	public AppinfoUploadView getAppinfoUploadView(Viewpoint viewpoint, Guid uploadId) throws NotFoundException {
		AppinfoUpload upload = em.find(AppinfoUpload.class, uploadId.toString());
		if (upload == null)
			throw new NotFoundException("Upload not found");
		
		AppinfoUpload currentUpload = getCurrentUpload(upload.getApplication().getId());
		
		return new AppinfoUploadView(upload, personViewer.getPersonView(viewpoint, upload.getUploader()),
									 upload == currentUpload);
	}
	
	public AppinfoFile getAppinfoFile(AppinfoUpload upload) throws NotFoundException {
		if (upload.isDeleteApplication()) {
			throw new NotFoundException("Application was deleted");
		}
		
		try {
			return getAppinfoFileInternal(upload);
		} catch (IOException e) {
			throw new RuntimeException("IO Error reading previously uploaded appinfo file", e);
		} catch (ValidationException e) {
			throw new RuntimeException("Validation error reading previously uploaded appinfo file", e);
		}
	}

	public void revertApplication(UserViewpoint viewpoint, String applicationId, Guid uploadId, String comment) throws XmlMethodException {
		if (!canEditApplications(viewpoint))
			throw new XmlMethodException(XmlMethodErrorCode.FORBIDDEN, "you don't have permission to edit applications");
		
		AppinfoUpload oldUpload = em.find(AppinfoUpload.class, uploadId.toString());
		if (oldUpload == null)
			throw new XmlMethodException(XmlMethodErrorCode.NOT_FOUND, "uploadId not found");
			
		if (!oldUpload.getApplication().getId().equals(applicationId))
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "uploadId doesn't refer to the right application");
		
		comment = comment.trim();
		if (comment.equals(""))
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "must provide a non-empty comment");
		
		Guid newUploadId = Guid.createNew();
		
		// In principal, we could just reuse the old saved copy we have of the application information.
		// But since the application save location is tied to the ID of the AppinfoUpload object
		// and we want a new AppinfoUpload object (for a new uploader/date/comment), we load
		// in the application data and save it out again.
		
		File saveLocation = getAppinfoLocation(newUploadId);
		AppinfoFile oldFile;
		try {
			oldFile = getAppinfoFileInternal(oldUpload);
		} catch (IOException e) {
			throw new XmlMethodException(XmlMethodErrorCode.FAILED, "IO error reading in old application data");
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.FAILED, "Old application data failed validation: " + e.getMessage());
		}
		
		OutputStream out = null;
		
		try {
			out = new FileOutputStream(saveLocation);
			oldFile.write(out);
			out.close();
			out = null;
		} catch (IOException e) {
			saveLocation.delete();
			throw new XmlMethodException(XmlMethodErrorCode.FAILED, "couldn't resave application data");
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}

		try {
			addUpload(viewpoint.getViewer().getGuid(), newUploadId, oldFile, comment);
		} catch (RuntimeException e) {
			saveLocation.delete();
			throw(e);
		}
	}

	public void reinstallAllApplications() {
		// We don't actually need to run on commit, just async, but on-commit
		// is an easy way to get async
		runner.runTaskOnTransactionCommit(new Runnable() {
			public void run() {
				final List<AppinfoUpload> uploads = new ArrayList<AppinfoUpload>();
				final Set<String> seenApplications = new HashSet<String>();
				
				logger.info("Getting list of applications to install");
				
				runner.runTaskInNewTransaction(new Runnable() {
					public void run() {
						Query q = em.createQuery("SELECT au from AppinfoUpload au ORDER BY uploadDate DESC");
						uploads.addAll(TypeUtils.castList(AppinfoUpload.class, q.getResultList()));
					}
				});
				
				for (final AppinfoUpload au : uploads) {
					if (seenApplications.contains(au.getApplication().getId()))
						continue;
					
					if (au.isDeleteApplication()) {
						seenApplications.add(au.getApplication().getId());
						continue;
					}
					
					try {
						runner.runTaskInNewTransaction(new Runnable() {
							public void run() {
								// Reattach application
								Application application = em.find(Application.class, au.getApplication().getId());
								if (application == null) {
									logger.warn("Application {} disappeared from database", application.getId());
									return;
								}
	
								logger.info("Reinstalling {}.appinfo for {}", au.getId(), application.getId());
								
								try {
									AppinfoFile appinfoFile = getAppinfoFileInternal(au);
									
									updateApplication(application, appinfoFile);
									updateApplicationCollections(application, appinfoFile);
													
									migrateUnmatchedUsage(application);
									
									seenApplications.add(application.getId());
									
								} catch (IOException e) {
									logger.warn("Couldn't read saved {}.dappinfo file: {}", e.getMessage());
								} catch (ValidationException e) {
									logger.warn("Couldn't validate saved {}.dappinfo file: {}", e.getMessage());
								}
							}
						});
					} catch (RuntimeException e) {
						logger.error("Failed to reinstall this application: " + au.getApplication().getId(), e);
					}
				}
				
				logger.info("Finished reinstalling all applications");
			}
		});
	}
	
	private void updateApplication(Application application, AppinfoFile appinfoFile) {
		application.setName(appinfoFile.getName());
		application.setGenericName(appinfoFile.getGenericName());
		application.setTooltip(appinfoFile.getTooltip());
		application.setDescription(appinfoFile.getDescription());
		
		application.setCategory(appinfoFile.getCategory());
		application.setTitlePatterns(appinfoFile.getTitlePatternsString());
		application.setDesktopNames(appinfoFile.getDesktopNamesString());
		application.setPackageNames(appinfoFile.getPackageNames());
	}
	
	private void updateApplicationCollections(Application application, AppinfoFile appinfoFile) {
		updateWmClasses(application, appinfoFile);
		updateIcons(application, appinfoFile);
	}
	
	private void deleteWmClasses(Application application) {
		em.createQuery("DELETE FROM ApplicationWmClass awc WHERE awc.application= :application")
		.setParameter("application", application)
		.executeUpdate();
	}
	
	private void updateWmClasses(Application application, AppinfoFile appinfoFile) {
		deleteWmClasses(application);
		
		for (String wmClass : appinfoFile.getWmClasses()) {
			ApplicationWmClass applicationWmClass = new ApplicationWmClass(application, wmClass);
			em.persist(applicationWmClass);
		}
	}
	
	private void deleteIcons(Application application) {
//      Mixing bulk updates and the TreeCache doesn't seem to work properly (you get
//      warnings because TreeCache work is done after the transaction is committed).
//      So we need to do this manually rather than with a bulk update
//
//		em.createQuery("DELETE FROM ApplicationIcon ai WHERE ai.application= :application")
//		.setParameter("application", application)
//		.executeUpdate();
//	
		List<ApplicationIcon> icons = new ArrayList<ApplicationIcon>(application.getIcons());
		for (ApplicationIcon icon : icons) {
			application.getIcons().remove(icon);
			em.remove(icon);
		}
	}
	
	private void updateIcons(Application application, AppinfoFile appinfoFile) {
		deleteIcons(application);
		
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
	
	private void migrateUnmatchedUsage(Application application) {
		Query q = em.createQuery("SELECT uau FROM ApplicationWmClass awc, UnmatchedApplicationUsage uau " +
				                 " WHERE awc.application = :application " +
				                 "   AND uau.wmClass = awc.wmClass")
			.setParameter("application", application);
		
		for (UnmatchedApplicationUsage uau : TypeUtils.castList(UnmatchedApplicationUsage.class, q.getResultList())) {
			ApplicationUsage au = new ApplicationUsage(uau.getUser(), application, uau.getDate());
			em.persist(au);
			em.remove(uau);
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
	
	public ApplicationView getApplicationView(String appId, int iconSize) throws NotFoundException {
		Application application = em.find(Application.class, appId);
		if (application == null)
			throw new NotFoundException("No such application");
		
		ApplicationView applicationView = new ApplicationView(application);
		try {
			applicationView.setIcon(getIcon(application, iconSize));
		} catch (NotFoundException e) {
			// FIXME: Default icon (maybe in getIcon())
		}
		
		return applicationView;
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
				return;
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

	public List<AppinfoUploadView> getUploadHistory(Viewpoint viewpoint, Application application) {
		Query q = em.createQuery("SELECT au from AppinfoUpload au " +
                " WHERE au.application = :application " +
                " ORDER BY uploadDate DESC")
                .setParameter("application", application);

		List<AppinfoUploadView> results = new ArrayList<AppinfoUploadView>();
		boolean current = true;
		for (AppinfoUpload upload : TypeUtils.castList(AppinfoUpload.class, q.getResultList())) {
			results.add(new AppinfoUploadView(upload, personViewer.getPersonView(viewpoint, upload.getUploader()), current));
			current = false;
		}
		
		return results;
	}
	
	public List<AppinfoUploadView> getUploadHistory(Viewpoint viewpoint, int maxItems) {
		Query q = em.createQuery("SELECT au from AppinfoUpload au " +
								 " ORDER BY uploadDate DESC");
		
		if (maxItems >= 0)
			q.setMaxResults(maxItems);

		List<AppinfoUploadView> results = new ArrayList<AppinfoUploadView>();
		boolean current = true;
		for (AppinfoUpload upload : TypeUtils.castList(AppinfoUpload.class, q.getResultList())) {
			results.add(new AppinfoUploadView(upload, personViewer.getPersonView(viewpoint, upload.getUploader()), current));
			current = false;
		}
		
		return results;
	}
	
	private void pageApplicationList(List<?> results, int iconSize, ApplicationCategory category, Pageable<ApplicationView> pageable) {
		List<ApplicationView> applicationViews = new ArrayList<ApplicationView>();
		
		int pos = 0;
		for (Object o : results) {
			Object[] columns = (Object[])o;
			Application application = (Application)columns[0];

			if (category != null && application.getCategory() != category)
				continue;
			
			if (pos >= pageable.getStart() && pos < pageable.getStart() + pageable.getCount()) {
				ApplicationView applicationView = new ApplicationView(application);
				//applicationView.setUsageCount(count.intValue());
				setViewIcon(applicationView, iconSize);
			
				applicationViews.add(applicationView);
			}
			
			pos++;
		}

		pageable.setResults(applicationViews);
		pageable.setTotalCount(pos);
	}

	// To make caching effective, we need to keep using the same "since" times for
	// a while. This quantizes to 1-hour intervals (not to "the hour", exactly, 
	// since we don't take into account leap seconds and whatnot, but rather to
	// an arbitrary point within the hour). Since the "since" times we use now
	// are "in the last month", a one-hour quantization has little effect.
	static Date quantizeSince(Date since) {
		long t = since.getTime();
		return new Date(t - t % 3600000L);
	}
	
	// "ORDER by COUNT(*) DESC is valid HQL but not supported by MySQL 4. MySQL 5 
	// handles it fine; since we are still using MySQL 4 on the production servers,
	// we do the sorting of grouped results ourself.
	static List<Object[]> getSortedResults(Query q) {
		List<Object[]> results = TypeUtils.castList(Object[].class,q.getResultList());
		
		Collections.sort(results, new Comparator<Object[]>() {
			public int compare(Object[] a, Object[] b){
				int countA = ((Number)a[1]).intValue();
				int countB = ((Number)b[1]).intValue();
				if (countA > countB)
					return -1;
				else if (countA < countB)
					return 1;
				else
					return 0;
			}
		});
		
		return results;
	}
	
	public void pagePopularApplications(Date since, int iconSize, ApplicationCategory category, Pageable<ApplicationView> pageable) {
		String category_clause;
		
		if (category != null)
			category_clause = "AND a.category = " + category.ordinal();
		else
			category_clause = "";
		
		Query q = em.createQuery("  SELECT a " +
								 "    FROM Application a " +
								 "   WHERE usageCount <> 0 " +
								 category_clause + 
								 "ORDER BY usageCount desc")
			.setFirstResult(pageable.getStart())
			.setMaxResults(pageable.getCount());
		
		List<ApplicationView> applicationViews = new ArrayList<ApplicationView>();
		
		for (Application application : TypeUtils.castList(Application.class, q.getResultList())) {
			ApplicationView applicationView = new ApplicationView(application);
			setViewIcon(applicationView, iconSize);
		
			applicationViews.add(applicationView);
		}
		
		pageable.setResults(applicationViews);

		Query countQuery = em.createQuery("   SELECT count(a)" +
				                          "    FROM Application a " +
										  "   WHERE usageCount <> 0 " +
										  category_clause)
			.setHint("org.hibernate.cacheable", true);

		pageable.setTotalCount(((Number)countQuery.getSingleResult()).intValue());
	}
	
	public void pageRelatedApplications(Application relatedTo, Date since, int iconSize, ApplicationCategory category, Pageable<ApplicationView> pageable) {
		if (since == null)
			since = getDefaultSince();
		Query q = em.createQuery("SELECT au.application, COUNT(*) " +
				 				 "  FROM ApplicationUsage au  " +
				 				 "  WHERE au.date > :since " +
				 				 "    AND EXISTS (SELECT au2 FROM ApplicationUsage au2 " +
				 				 "                 WHERE au2.application.id = :relatedId " +
				 				 "                   AND au2.user = au.user " +
				 				 "                   AND au2.date > :since) " +
				 				 "  GROUP by au.application.id")
			.setParameter("relatedId", relatedTo.getId())
			.setParameter("since", quantizeSince(since))
			.setHint("org.hibernate.cacheable", true);

		pageApplicationList(getSortedResults(q), iconSize, category, pageable);
	}

	public void pageMyApplications(UserViewpoint viewpoint, Date since, int iconSize, ApplicationCategory category, Pageable<ApplicationView> pageable) {
		if (since == null)
			since = getDefaultSince();		
		Query q = em.createQuery("SELECT au.application, COUNT(*) " +
								 "  FROM ApplicationUsage au  " +
								 "  WHERE au.date > :since " +
								 "    AND au.user = :user " +
								 "  GROUP by au.application.id")
		    .setParameter("since", since)
		    .setParameter("user", viewpoint.getViewer());
		
		pageApplicationList(getSortedResults(q), iconSize, category, pageable);
	}
	
	public Date getMyApplicationUsageStart(UserViewpoint viewpoint) {
		try {
			Query q = em.createQuery("SELECT MIN(au.date) from ApplicationUsage au where au.user = :user")
				.setParameter("user", viewpoint.getViewer());
			return (Date) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
	
	public List<CategoryView> getPopularCategories(Date since) {
		if (since == null)
			since = getDefaultSince();		
		Map<ApplicationCategory, Integer> usageCounts = new HashMap<ApplicationCategory, Integer>();
		
		Query q = em.createQuery(" SELECT a.category, COUNT(*) " +
								 "   FROM ApplicationUsage au, Application a " +
								 "   WHERE au.application = a AND au.date > :since " +
								 "GROUP BY a.category")
			.setParameter("since", quantizeSince(since))
			.setHint("org.hibernate.cacheable", true);
		
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

	public List<Application> getApplicationsWithTitlePatterns() {
		Query q = em.createQuery("SELECT a from Application a WHERE a.titlePatterns IS NOT NULL and a.titlePatterns <> ''");
		return TypeUtils.castList(Application.class, q.getResultList());
	}

	public ApplicationUserState getUserState(final User user, final Application app) {
		try {
			return runner.runTaskThrowingConstraintViolation(new Callable<ApplicationUserState>() {
				public ApplicationUserState call() {
					Query q = em.createQuery("FROM ApplicationUserState aus WHERE aus.user = :user "
							+ " AND aus.application = :app");
					q.setParameter("user", user);
					q.setParameter("app", app);
	
					ApplicationUserState res;
					try {
						res = (ApplicationUserState) q.getSingleResult();
					} catch (NoResultException e) {
						res = new ApplicationUserState(user, app);
						em.persist(res);
					}
	
					return res;	
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}	

	public void pinApplicationIds(User user, List<String> applicationIds, boolean pin) {
		for (String appId : applicationIds) {
			Application application = em.find(Application.class, appId);
			if (application == null) {
				logger.warn("Unknown application id '{}'", appId);
				continue;
			}
			ApplicationUserState state = getUserState(user, application);
			state.setPinned(pin);
		}
	}
	
	private void setViewIcon(ApplicationView view, int size) {
		try {
			view.setIcon(getIcon(view.getApplication(), size));
		} catch (NotFoundException e) {
			// FIXME: Default icon (maybe in getIcon())
		}				
	}
	
	public List<ApplicationView> viewApplications(UserViewpoint viewpoint, List<Application> apps, int iconSize) {
		List<ApplicationView> result = new ArrayList<ApplicationView>();
		for (Application app : apps) {
			ApplicationView viewedApp = new ApplicationView(app);
			setViewIcon(viewedApp, iconSize);
			result.add(viewedApp);
		}
		return result;
	}

	public List<Application> getPinnedApplications(User user) {
		Query q = em.createQuery("SELECT aus.application FROM ApplicationUserState aus WHERE aus.user = :user AND aus.pinned = TRUE")
			.setParameter("user", user);
		return TypeUtils.castList(Application.class, q.getResultList());
	}

	public void updateUsages() {
		Set<String> usedApps = new HashSet<String>();
		
		Query q = em.createQuery("SELECT au.application, COUNT(*) " +
				 "  FROM ApplicationUsage au  " +
				 "  WHERE au.date > :since " +
				 "  GROUP by au.application.id")
			.setParameter("since", quantizeSince(getDefaultSince()));

		int lastCount = -1;
		int currentRank = 1;  // paying attention to ties
		int totalRank = 1;    // ignoring ties
		for (Object o : getSortedResults(q)) {
			Object[] columns = (Object[])o;
			Application application = (Application)columns[0];
			int count = ((Number)columns[1]).intValue();
			
			if (count != lastCount)
				currentRank = totalRank;

			application.setRank(currentRank);
			application.setUsageCount(count);
			
			lastCount = count;

			usedApps.add(application.getId());
			
			totalRank++;
		}
		
		// Set the rank for all unused applications
		q = em.createQuery("SELECT a from Application a");
		for (Application application : TypeUtils.castList(Application.class, q.getResultList())) {
			if (!usedApps.contains(application.getId())) {
				application.setRank(totalRank);
				application.setUsageCount(0);
			}
		}
	}
}
