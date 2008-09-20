package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jboss.annotation.IgnoreDependency;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Thumbnail;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.dm.ReadWriteSession;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.OnlineAccountType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.PicasaUpdater;
import com.dumbhippo.server.YouTubeUpdater;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.dm.ExternalAccountDMO;
import com.dumbhippo.server.dm.ExternalAccountKey;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.OnlineAccountTypeView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.services.FlickrPhotoSize;
import com.dumbhippo.services.FlickrPhotoView;
import com.dumbhippo.services.FlickrPhotosView;
import com.dumbhippo.services.PicasaAlbum;
import com.dumbhippo.services.YouTubeVideo;
import com.dumbhippo.services.caches.CacheFactory;
import com.dumbhippo.services.caches.FlickrUserPhotosCache;
import com.dumbhippo.services.caches.PicasaAlbumsCache;
import com.dumbhippo.services.caches.WebServiceCache;
import com.dumbhippo.services.caches.YouTubeVideosCache;

@Stateless
public class ExternalAccountSystemBean implements ExternalAccountSystem {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(ExternalAccountSystemBean.class);
	
	@EJB
	@IgnoreDependency
	private FacebookSystem facebookSystem;
	
	@EJB
	@IgnoreDependency
	private YouTubeUpdater youTubeUpdater;
	
	@EJB
	@IgnoreDependency
	private PicasaUpdater picasaUpdater;
	
	@EJB
	private Notifier notifier;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em; 
	
	@WebServiceCache
	private FlickrUserPhotosCache flickrUserPhotosCache;
	
	@WebServiceCache
	private YouTubeVideosCache youTubeVideosCache;
	
	@WebServiceCache
	private PicasaAlbumsCache picasaAlbumsCache;
	
	@EJB
	private CacheFactory cacheFactory;	
	
	@PostConstruct
	public void init() {
		cacheFactory.injectCaches(this);
	}
	
	public ExternalAccount getOrCreateExternalAccount(UserViewpoint viewpoint, ExternalAccountType type) {
		Account a = viewpoint.getViewer().getAccount();
		if (!em.contains(a))
			throw new RuntimeException("detached account in getOrCreateExternalAccount");
		
		ExternalAccount external = a.getExternalAccount(type);
		if (external == null) {
			external = new ExternalAccount(type);
			external.setOnlineAccountType(getOnlineAccountType(type));
			external.setMugshotEnabled(true);
			external.setAccount(a);
			em.persist(external);
			a.getExternalAccounts().add(external);			
			
			notifier.onExternalAccountCreated(a.getOwner(), external);
		}
		return external;
	}
	
	public ExternalAccount createExternalAccount(UserViewpoint viewpoint, OnlineAccountType type) {
		Account a = viewpoint.getViewer().getAccount();
		if (!em.contains(a))
			throw new RuntimeException("detached account in getOrCreateExternalAccount");

		ExternalAccount external = new ExternalAccount();
		external.setOnlineAccountType(type);
		external.setAccount(a);
		em.persist(external);
		a.getExternalAccounts().add(external);			
			
		notifier.onExternalAccountCreated(a.getOwner(), external);
		return external;
	}
	
	public ExternalAccount lookupExternalAccount(UserViewpoint viewpoint, String id) throws NotFoundException {
		Account a = viewpoint.getViewer().getAccount();
		if (!em.contains(a))
			throw new RuntimeException("detached account in lookupExternalAccount");
		
		ExternalAccount external = a.getExternalAccount(id);
		if (external == null)
			throw new NotFoundException("ExternalAccount with id " +  id + " was not found for account " + a);
		
		return external;
	}
	
	public ExternalAccount lookupExternalAccount(Viewpoint viewpoint, User user, ExternalAccountType type)
	    throws NotFoundException {
	    if (!em.contains(user.getAccount()))
		    throw new RuntimeException("detached account in lookupExternalAccount()");
	
	    // Right now, external accounts are public, unlike email/aim resources which are friends only...
	    // so we don't need to use the viewpoint. But here in case we want to add it later.
	    ExternalAccount external = user.getAccount().getExternalAccount(type);
	    if (external == null)
		    throw new NotFoundException("No external account of type " + type + " for user " + user);
	    else
	    	return external;
    }
 
    public Set<ExternalAccount> lookupExternalAccounts(Viewpoint viewpoint, User user, OnlineAccountType type) {
	    if (!em.contains(user.getAccount()))
	        throw new RuntimeException("detached account in lookupExternalAccount()");

        // Right now, external accounts are public, unlike email/aim resources which are friends only...
        // so we don't need to use the viewpoint. But here in case we want to add it later.
        return user.getAccount().getExternalAccounts(type);
    }

	public OnlineAccountType getOnlineAccountType(ExternalAccountType accountType) {
		Query q = em.createQuery("SELECT oat FROM OnlineAccountType oat WHERE " +
				                 "oat.accountType = " + accountType.ordinal());
		try {
			return (OnlineAccountType)q.getSingleResult();
		} catch (NoResultException e) {
			throw new RuntimeException("There is an ExternalAccountType for which no matching OnlineAccountType was found: " + accountType.getName());
		}
	}
	
	public List<OnlineAccountType> getAllOnlineAccountTypes() {
		 Query q = em.createQuery("SELECT oat FROM OnlineAccountType oat");						
         return TypeUtils.castList(OnlineAccountType.class, q.getResultList());
	}
	
	public void writeSupportedOnlineAccountTypesToXml(XmlBuilder xml, String lang) {
		Query q = em.createQuery("SELECT oat FROM OnlineAccountType oat WHERE oat.supported = TRUE");		
		List<OnlineAccountType> supportedTypes = TypeUtils.castList(OnlineAccountType.class,
				                                                    q.getResultList());													
		for (OnlineAccountType supportedType : supportedTypes) {
			OnlineAccountTypeView supportedTypeView = new OnlineAccountTypeView(supportedType);
			supportedTypeView.writeToXmlBuilder(xml, lang);
		}
	}
	
	public OnlineAccountType lookupOnlineAccountTypeForName(String name) throws NotFoundException {
		Query q = em.createQuery("SELECT oat FROM OnlineAccountType oat WHERE " +
				                 "LOWER(oat.name) = :name");
		q.setParameter("name", name.toLowerCase());
		
		try {
			return (OnlineAccountType)q.getSingleResult();
		} catch (NoResultException e) {
			throw new NotFoundException("No OnlineAccountType with name " + name);
		}
	}
	
	public OnlineAccountType lookupOnlineAccountTypeForFullName(String fullName) throws NotFoundException {
		Query q = em.createQuery("SELECT oat FROM OnlineAccountType oat WHERE " +
                                 "LOWER(oat.fullName) = :fullName");
        q.setParameter("fullName", fullName.toLowerCase());

        try {
            return (OnlineAccountType)q.getSingleResult();
        } catch (NoResultException e) {
            throw new NotFoundException("No OnlineAccountType with full name " + fullName);
        }
    }

	public OnlineAccountType lookupOnlineAccountTypeForUserInfoType(String userInfoType) throws NotFoundException {
		Query q = em.createQuery("SELECT oat FROM OnlineAccountType oat WHERE " +
                                 "LOWER(oat.userInfoType) = :userInfoType");
        q.setParameter("userInfoType", userInfoType.toLowerCase());

        try {
            return (OnlineAccountType)q.getSingleResult();
        } catch (NoResultException e) {
            throw new NotFoundException("No OnlineAccountType with user info type " + userInfoType);
        }
    }
	
	public List<OnlineAccountType> lookupOnlineAccountTypesForSite(String siteUrl) { 
	    Query q = em.createQuery("SELECT oat FROM OnlineAccountType oat WHERE " +
                                 "LOWER(oat.site) = :site");
        q.setParameter("site", siteUrl);						
        return TypeUtils.castList(OnlineAccountType.class, q.getResultList());
    }

    public OnlineAccountType createOnlineAccountType(UserViewpoint viewpoint, String name, String fullName, String siteName, String siteUrl, String userInfoType) throws ValidationException {
    	OnlineAccountType type = new OnlineAccountType(name, fullName, siteName, siteUrl, userInfoType);
		type.setCreator(viewpoint.getViewer());
		em.persist(type);
		return type;
    }
	
	public Set<ExternalAccountView> getExternalAccountViews(Viewpoint viewpoint, User user) {
		// Right now we ignore the viewpoint, so this method is pretty pointless.
		// but if people use it, future code will work properly.
		
		// be sure the account is attached... the external accounts are lazy-loaded
		if (!em.contains(user.getAccount()))
			throw new RuntimeException("detached account in getExternalAccounts()");
		
		Set<ExternalAccount> accounts = user.getAccount().getMugshotEnabledExternalAccounts();
		//logger.debug("{} external accounts for user {}", accounts.size(), user);
		
		Set<ExternalAccountView> accountViews = new HashSet<ExternalAccountView>();
		for (ExternalAccount account : accounts) {
			if (account.getAccountType() == ExternalAccountType.FACEBOOK && account.isLovedAndEnabled()) {
				accountViews.add(new ExternalAccountView(account, facebookSystem.getProfileLink(account)));
			} else {
				accountViews.add(new ExternalAccountView(account));
			}				
		}

		return accountViews;
	}

	public ExternalAccountView getExternalAccountView(Viewpoint viewpoint, ExternalAccount externalAccount) {
		ExternalAccountView view;
		if (externalAccount.getAccountType() == ExternalAccountType.FACEBOOK) {
			view = new ExternalAccountView(externalAccount, facebookSystem.getProfileLink(externalAccount));
		} else {
			view = new ExternalAccountView(externalAccount);
		}
		
		loadThumbnails(viewpoint, view);
		
		return view;
	}
	
	public ExternalAccountView getExternalAccountView(Viewpoint viewpoint, User user, ExternalAccountType externalAccountType) throws NotFoundException {
		ExternalAccount externalAccount = lookupExternalAccount(viewpoint, user, externalAccountType);
		return getExternalAccountView(viewpoint, externalAccount);
	}

	private FlickrPhotosView getFlickrPhotosView(ExternalAccount account) {
		if (account.getAccountType() != ExternalAccountType.FLICKR)
			throw new IllegalArgumentException("should be a flickr account here");
	
		if (account.getSentiment() != Sentiment.LOVE)
			throw new IllegalArgumentException("Flickr account is unloved");
		
		if (account.getHandle() == null)
			return null;
		
		return flickrUserPhotosCache.getSync(account.getHandle());
	}
	
	private List<? extends FlickrPhotoView> getFlickrThumbnails(ExternalAccount account) {
		FlickrPhotosView photos = getFlickrPhotosView(account);
		if (photos == null) {
			logger.debug("No public photos for {}", account);
			return null;
		}
		
		return photos.getPhotos();
	}

	private void loadFlickrThumbnails(Viewpoint viewpoint, ExternalAccountView accountView) {
		ExternalAccount account = accountView.getExternalAccount();
		FlickrPhotosView photos = getFlickrPhotosView(account);
		if (photos == null) {
			logger.debug("No public photos for {}", account);
			return;
		}
		
		accountView.setThumbnailsData(TypeUtils.castList(Thumbnail.class, photos.getPhotos()), photos.getTotal(), 
					FlickrPhotoSize.SMALL_SQUARE.getPixels(), FlickrPhotoSize.SMALL_SQUARE.getPixels());
	}
	

	private List<? extends YouTubeVideo> getYouTubeThumbnails(ExternalAccount account) {
		if (account.getAccountType() != ExternalAccountType.YOUTUBE)
			throw new IllegalArgumentException("should be a YouTube account here");
	
		if (account.getSentiment() != Sentiment.LOVE)
			throw new IllegalArgumentException("YouTube account is unloved =(");
		
		if (account.getHandle() == null)
			return null;
		
		try {
			youTubeUpdater.getCachedStatus(account);
		} catch (NotFoundException e) {
			logger.debug("No cached YouTube status for {}", account);
			return null;
		}
		
		List<? extends YouTubeVideo> videos = youTubeVideosCache.getSync(account.getHandle());
		if (videos.isEmpty()) {
			logger.debug("Empty list of videos for {}", account);
			return null;
		}
		
		return videos;
	}	
	
	private void loadYouTubeThumbnails(Viewpoint viewpoint, ExternalAccountView accountView) {
		ExternalAccount externalAccount = accountView.getExternalAccount();
		
		List<? extends YouTubeVideo> videos = getYouTubeThumbnails(externalAccount);
		if (videos == null)
			return;
		
		accountView.setThumbnailsData(TypeUtils.castList(Thumbnail.class, videos), videos.size(), 
			     	                  videos.get(0).getThumbnailWidth(), videos.get(0).getThumbnailHeight());
	}

	private List<? extends PicasaAlbum> getPicasaThumbnails(ExternalAccount account) {
		if (account.getAccountType() != ExternalAccountType.PICASA)
			throw new IllegalArgumentException("should be a Picasa account here");
	
		if (account.getSentiment() != Sentiment.LOVE)
			throw new IllegalArgumentException("Picasa account is unloved =(");
		
		if (account.getHandle() == null)
			return null;
		
		try {
			picasaUpdater.getCachedStatus(account);
		} catch (NotFoundException e) {
			logger.debug("No cached Picasa status for {}", account);
			return null;
		}
		
		List<? extends PicasaAlbum> albums = picasaAlbumsCache.getSync(account.getHandle());
		if (albums.isEmpty()) {
			logger.debug("Empty list of albums for {}", account);
			return null;
		}
		
		return albums;
	}		
	
	private void loadPicasaThumbnails(Viewpoint viewpoint, ExternalAccountView accountView) {
		ExternalAccount externalAccount = accountView.getExternalAccount();
		
		List<? extends PicasaAlbum> albums = getPicasaThumbnails(externalAccount);	
		if (albums == null)
			return;
		
		accountView.setThumbnailsData(TypeUtils.castList(Thumbnail.class, albums), albums.size(), 
	             			          albums.get(0).getThumbnailWidth(), albums.get(0).getThumbnailHeight());
	}

	public List<? extends Thumbnail> getThumbnails(ExternalAccount externalAccount) {
		ExternalAccountType type = externalAccount.getAccountType();
		// you only have thumbnails for accounts you like
		if (externalAccount.getSentiment() != Sentiment.LOVE)
			return null;
		
		switch (type) {
		case FLICKR:
			return getFlickrThumbnails(externalAccount);
		case YOUTUBE:
			return getYouTubeThumbnails(externalAccount);
		case PICASA:
			return getPicasaThumbnails(externalAccount);
		default:
			// most accounts lack thumbnails
			return null;
		}
	}

	private void loadThumbnails(Viewpoint viewpoint, ExternalAccountView externalAccountView) {
		ExternalAccount externalAccount = externalAccountView.getExternalAccount();
		ExternalAccountType type = externalAccount.getAccountType();
		// you only have thumbnails for accounts you like
		if (externalAccount.getSentiment() != Sentiment.LOVE)
			return;
		
		switch (type) {
		case FLICKR:
			loadFlickrThumbnails(viewpoint, externalAccountView);
			break;
		case YOUTUBE:
			loadYouTubeThumbnails(viewpoint, externalAccountView);
			break;
		case PICASA:
			loadPicasaThumbnails(viewpoint, externalAccountView);
			break;
		default:
			// most accounts lack thumbnails
			break;
		}
	}
	
	public void loadThumbnails(Viewpoint viewpoint, Set<ExternalAccountView> accountViews) {
		for (ExternalAccountView externalView : accountViews) {
			loadThumbnails(viewpoint, externalView);
		}
	}

	public void setSentiment(ExternalAccount externalAccount, Sentiment sentiment) {
		if ((sentiment == Sentiment.LOVE) && !externalAccount.hasAccountInfo()) {
			throw new RuntimeException("Trying to set a love sentiment on account with no valid account info");
		}
		
		externalAccount.setSentiment(sentiment);
		
		ExternalAccountKey key = new ExternalAccountKey(externalAccount);
		ReadWriteSession session = DataService.currentSessionRW();
		session.changed(ExternalAccountDMO.class, key, "sentiment");
		session.changed(ExternalAccountDMO.class, key, "quip"); // Quip is only present for HATE
		
		notifier.onExternalAccountLovedAndEnabledMaybeChanged(externalAccount.getAccount().getOwner(), externalAccount);
	}

	public boolean getExternalAccountExistsLovedAndEnabled(Viewpoint viewpoint, User user, ExternalAccountType accountType) {
		try {
			ExternalAccount external = lookupExternalAccount(viewpoint, user, accountType);
			return external.isLovedAndEnabled();
		} catch (NotFoundException e) {
			return false;
		}
	}
	
	public void onAccountDisabledToggled(Account account) {
		for (ExternalAccount external : account.getExternalAccounts()) {
			// this is why we have "maybe changed" since we really don't know.
			notifier.onExternalAccountLovedAndEnabledMaybeChanged(account.getOwner(), external);
		}
	}

	public void onAccountAdminDisabledToggled(Account account) {
		for (ExternalAccount external : account.getExternalAccounts()) {
			// this is why we have "maybe changed" since we really don't know.
			notifier.onExternalAccountLovedAndEnabledMaybeChanged(account.getOwner(), external);
		}		
	}

	public void onMusicSharingToggled(UserViewpoint viewpoint) {
		// We aren't interested in this, just part of a listener iface we're using
	}
	
	public void onApplicationUsageToggled(UserViewpoint viewpoint) {
		// We aren't interested in this, just part of a listener iface we're using
	}

	public void onFacebookApplicationEnabledToggled(UserViewpoint viewpoint) {
		// We aren't interested in this, just part of a listener iface we're using
	}
	
	public void validateAll() {
		logger.info("Administrator kicked off validation of all ExternalAccount rows in the database");
		Query q = em.createQuery("SELECT ea.id, ea.accountType, ea.handle, ea.extra FROM ExternalAccount ea");
		List<Object[]> results = TypeUtils.castList(Object[].class, q.getResultList());
		
		logger.info("There are {} ExternalAccount rows to validate", results.size());
		
		List<Long> invalidHandles = new ArrayList<Long>();
		List<Long> invalidExtras = new ArrayList<Long>();
		
		for (Object[] row : results) {
			//logger.debug("row is: {}", Arrays.toString(row));
			Long id = (Long) row[0];
			ExternalAccountType accountType = (ExternalAccountType) row[1];
			String handle = (String) row[2];
			String extra = (String) row[3];
			if (accountType == null) {
				logger.info("Row has null accountType: {}", Arrays.toString(row));
			} else {
				try {
					String c = accountType.canonicalizeHandle(handle);
					if (handle != null && !c.equals(handle)) {
						logger.info("Row is not canonicalized: {}: '{}' vs. '{}'",
								new Object[] { Arrays.toString(row), handle, c });
					}
				} catch (ValidationException e) {
					logger.info("Row had invalid 'handle': {}: {}", Arrays.toString(row), e.getMessage());
					invalidHandles.add(id);
				}
				try {
					String c = accountType.canonicalizeExtra(extra);
					if (extra != null && !c.equals(extra)) {
						logger.info("Row is not canonicalized: {}: '{}' vs. '{}'",
								new Object[] { Arrays.toString(row), extra, c });
					}
				} catch (ValidationException e) {
					logger.info("Row had invalid 'extra': {}: {}", Arrays.toString(row), e.getMessage());
					invalidExtras.add(id);
				}
			}
		}
		
		if (!invalidHandles.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append("UPDATE ExternalAccount SET handle = NULL WHERE id IN (");
			for (Long id : invalidHandles) {
				sb.append(id);
				sb.append(",");
			}
			sb.setLength(sb.length() - 1); // chop comma
			sb.append(");");
			logger.info("Possible query to null invalid handles: {}", sb);
		}

		if (!invalidExtras.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append("UPDATE ExternalAccount SET extra = NULL WHERE id IN (");
			for (Long id : invalidHandles) {
				sb.append(id);
				sb.append(",");
			}
			sb.setLength(sb.length() - 1); // chop comma
			sb.append(");");
			logger.info("Possible query to null invalid extras: {}", sb);
		}
				
		logger.info("ExternalAccount validation complete, {} invalid handles {} invalid extras", invalidHandles.size(), invalidExtras.size());
	}
}
