package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.Site;
import com.dumbhippo.dm.DMFeed;
import com.dumbhippo.dm.DMFeedItem;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMInit;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.ContactStatus;
import com.dumbhippo.persistence.DesktopSetting;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.FacebookResource;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.persistence.XmppResource;
import com.dumbhippo.server.DesktopSettings;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.OnlineDesktopSystem;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.server.views.AnonymousViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@DMO(classId="http://mugshot.org/p/o/user", resourceBase="/o/user")
public abstract class UserDMO extends DMObject<Guid> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(UserDMO.class);
	
	private User user;
//	private String email;
//	private String aim;
//	private boolean contactOfViewer;
	
	private static final int CONTACTERS_GROUP = 1;
	private static final int CURRENT_TRACK_GROUP = 2; 
	
	private TrackHistory currentTrack;

	// people who have any opinion of this UserDMO except blocked
	private Set<UserDMO> contacters; /* Includes hot/medium/cold contacters */
	// people who have pinned this UserDMO in their "top N"
	private Set<UserDMO> hotContacters;
	// people who never want this UserDMO in their pinned "top N"
	private Set<UserDMO> coldContacters;
	// people who have blocked this UserDMO
	private Set<UserDMO> blockingContacters;
	
	@Inject
	private EntityManager em;
	
	@Inject
	private DMSession session;
	
	@Inject
	private Viewpoint viewpoint;

	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private DesktopSettings settings;
	
	@EJB
	private ApplicationSystem applicationSystem;
	
	@EJB
	private MusicSystem musicSystem;
	
	@EJB
	private OnlineDesktopSystem onlineDesktopSystem;
	
	@EJB
	private Stacker stacker;

	protected UserDMO(Guid key) {
		super(key);
	}
	
	@Override
	protected void init() throws NotFoundException {
		user = em.find(User.class, getKey().toString());
		if (user == null)
			throw new NotFoundException("No such user");
	}
	
	@DMProperty(defaultInclude=true)
	public String getName() {
		return user.getNickname();
	}

	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getHomeUrl() {
		return "/person?who=" + user.getId();
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getPhotoUrl() {
		return user.getPhotoUrl();
	}
	
	@DMProperty
	public List<ExternalAccountDMO> getLovedAccounts() {
		List<ExternalAccountDMO> result = new ArrayList<ExternalAccountDMO>();
		
		for (ExternalAccount externalAccount : user.getAccount().getExternalAccounts()) {
			if (externalAccount.isLovedAndEnabled())
				result.add(session.findUnchecked(ExternalAccountDMO.class, new ExternalAccountKey(externalAccount)));
		}
		
		return result;
	}
	
	@DMProperty
	@DMFilter("viewer.canSeeFriendsOnly(this)") // friends can see contacts that are users, but not other contacts
	public Set<UserDMO> getUserContacts() {
		Set<UserDMO> result = new HashSet<UserDMO>();
		
		for (Guid guid : identitySpider.computeUserContacts(user.getGuid()))
			result.add(session.findUnchecked(UserDMO.class, guid));
		
		return result;
	}
	
	@DMProperty
	@DMFilter("viewer.canSeePrivate(this)") // friends can't see contacts that are not users with accounts
	public Set<ContactDMO> getContacts() {
		Set<ContactDMO> result = new HashSet<ContactDMO>();
		
		for (Guid guid : identitySpider.computeContacts(user.getGuid()))
			result.add(session.findUnchecked(ContactDMO.class, guid));
		
		return result;
	}
	
	@DMInit(group=CONTACTERS_GROUP, initMain=false)
	public void initContacters() {
		blockingContacters = new HashSet<UserDMO>();
		contacters = new HashSet<UserDMO>();
		coldContacters = new HashSet<UserDMO>();
		hotContacters = new HashSet<UserDMO>();

		for (Pair<Guid,ContactStatus> pair : identitySpider.computeContactersWithStatus(getKey())) {
			Guid guid = pair.getFirst();
			ContactStatus status = pair.getSecond();
			
			UserDMO contacter = session.findUnchecked(UserDMO.class, guid);
		
			switch (status) {
			case NONCONTACT:
				break;
			case BLOCKED:
				blockingContacters.add(contacter);
				break;
			case COLD:
				coldContacters.add(contacter);
				contacters.add(contacter);
				break;
			case MEDIUM:
				contacters.add(contacter);
				break;
			case HOT:
				hotContacters.add(contacter);
				contacters.add(contacter);
				break;
			}
		}
	}
	
	@DMProperty(group=CONTACTERS_GROUP)
	@DMFilter("viewer.canSeePrivate(this)")
	public Set<UserDMO> getContacters() {
		return contacters;
	}
	
	/* The @DMFilter("false") below may seem odd, but we don't want even the 
	 * user themselves to be able to see how other people have tagged them.
	 * These properties are here only for caching. 
	 */
	
	@DMProperty(group=CONTACTERS_GROUP)
	@DMFilter("false")
	public Set<UserDMO> getBlockingContacters() {
		return blockingContacters;
	}
	
	@DMProperty(group=CONTACTERS_GROUP)
	@DMFilter("false")
	public Set<UserDMO> getColdContacters() {
		return coldContacters;
	}

	@DMProperty(group=CONTACTERS_GROUP)
	@DMFilter("false")
	public Set<UserDMO> getHotContacters() {
		return hotContacters;
	}
	
	private boolean viewerIsInContacterSet(String propertyName) {
		if (viewpoint instanceof UserViewpoint) {
			try {
				@SuppressWarnings("unchecked")
				Set<Guid> contacterIds = (Set<Guid>)session.getRawProperty(UserDMO.class, getKey(), propertyName);
				
				Guid userId = ((UserViewpoint)viewpoint).getViewerId();
				
				return contacterIds.contains(userId);
			} catch (NotFoundException e) {
				return false;
			}
		} else {
			return false;
		}
	}
	
	/* Gets the ContactStatus the viewer has applied to this user */
	@DMProperty(cached=false)
	public int getContactStatus() {
		ContactStatus status;
		
		if (viewerIsInContacterSet("blockingContacters"))
			status = ContactStatus.BLOCKED;
		else if (!viewerIsInContacterSet("contacters"))
			status = ContactStatus.NONCONTACT;
		else if (viewerIsInContacterSet("hotContacters"))
			status = ContactStatus.HOT;
		else if (viewerIsInContacterSet("coldContacters"))
			status = ContactStatus.COLD;
		else
			status = ContactStatus.MEDIUM;
		
		return status.ordinal();
	}

	// this is a little broken, probably only getEmails() and perhaps a getPrimaryEmail() 
	// should exist
	@DMProperty
	@DMFilter("viewer.canSeeFriendsOnly(this)")
	public String getEmail() {
		// FIXME: We need to let the user select their "primary" email,
		// rather than returning a hash-table random email
		for (AccountClaim ac : user.getAccountClaims()) {
			Resource r = ac.getResource();
			if (r instanceof EmailResource)
				return ((EmailResource)r).getEmail();
		}
		
		return null;
	}
	
	@DMProperty
	@DMFilter("viewer.canSeeFriendsOnly(this)")
	public Set<String> getEmails() {
		Set<String> results = new HashSet<String>();
		for (AccountClaim ac : user.getAccountClaims()) {
			Resource r = ac.getResource();
			if (r instanceof EmailResource)
				results.add(((EmailResource)r).getEmail());
		}
		
		return results;
	}
	
	// broken; really only getAims should exist
	@DMProperty
	@DMFilter("viewer.canSeeFriendsOnly(this)")
	public String getAim() {
		for (AccountClaim ac : user.getAccountClaims()) {
			Resource r = ac.getResource();
			if (r instanceof AimResource)
				return ((AimResource)r).getScreenName();
		}
		
		return null;
	}
	
	@DMProperty
	@DMFilter("viewer.canSeeFriendsOnly(this)")
	public Set<String> getAims() {
		Set<String> results = new HashSet<String>();
		for (AccountClaim ac : user.getAccountClaims()) {
			Resource r = ac.getResource();
			if (r instanceof AimResource)
				results.add(((AimResource)r).getScreenName());
		}
		
		return results;
	}
	
	@DMProperty
	@DMFilter("viewer.canSeeFriendsOnly(this)")
	public String getXmpp() {
		for (AccountClaim ac : user.getAccountClaims()) {
			Resource r = ac.getResource();
			if (r instanceof XmppResource)
				return ((XmppResource)r).getJid();
		}
		
		return null;
	}

	@DMProperty
	@DMFilter("viewer.canSeeFriendsOnly(this)")
	public Set<String> getXmpps() {
		Set<String> results = new HashSet<String>();
		for (AccountClaim ac : user.getAccountClaims()) {
			Resource r = ac.getResource();
			if (r instanceof XmppResource)
				results.add(((XmppResource)r).getJid());
		}
		
		return results;
	}	
	
	@DMProperty
	public String getFacebook() {
		for (AccountClaim ac : user.getAccountClaims()) {
			Resource r = ac.getResource();
			if (r instanceof FacebookResource)
				return ((FacebookResource)r).getFacebookUserId();
		}
		
		return null;
	}
	
	@DMProperty
	@DMFilter("viewer.canSeePrivate(this)")
	public Set<String> getGoogleEnabledEmails() {
		Set<String> results = new HashSet<String>();
		for (EmailResource er : onlineDesktopSystem.getGoogleEnabledEmails(viewpoint, user)) {
			results.add(er.getEmail());
		}
		return results;
	}

	@DMProperty
	@DMFilter("viewer.canSeePrivate(this)")
	public Set<DesktopSettingDMO> getSettings() {
		Set<DesktopSettingDMO> result = new HashSet<DesktopSettingDMO>();
		
		Collection<DesktopSetting> userSettings = settings.getSettingsObjects(user); 
		
		for (DesktopSetting setting : userSettings) {
			result.add(session.findUnchecked(DesktopSettingDMO.class, new DesktopSettingKey(setting)));
		}
		
		return result;
	}

	// the Date here can be null, not sure if that's OK
	@DMProperty
	@DMFilter("viewer.canSeePrivate(this)")
	public long getApplicationUsageStart() {
		Date since = applicationSystem.getMyApplicationUsageStart(new UserViewpoint(user, Site.NONE));
		return since == null ? -1 : since.getTime();
	}
	
	@DMProperty
	@DMFilter("viewer.canSeePrivate(this)")
	public boolean getApplicationUsageEnabled() {
		return identitySpider.getApplicationUsageEnabled(user);
	}
	
	@DMProperty
	@DMFilter("viewer.canSeePrivate(this)")
	public Set<ApplicationDMO> getTopApplications() {
		UserViewpoint viewpoint = new UserViewpoint(user, Site.NONE);
		
		Set<ApplicationDMO> result = new HashSet<ApplicationDMO>();
	
		// returned "since" here can be null, which is OK
		Date since = applicationSystem.getMyApplicationUsageStart(viewpoint);

		List<String> appIds = applicationSystem.getMyMostUsedApplicationIds(viewpoint, since, 15);
		for (String appId : appIds) {
			result.add(session.findUnchecked(ApplicationDMO.class, appId));
		}
		
		return result;
	}
	
	@DMProperty
	@DMFilter("viewer.canSeePrivate(this)")
	public Set<ApplicationDMO> getPinnedApplications() {
		Set<ApplicationDMO> result = new HashSet<ApplicationDMO>();
	
		List<String> appIds = applicationSystem.getPinnedApplicationIds(user);
		for (String appId : appIds) {
			result.add(session.findUnchecked(ApplicationDMO.class, appId));
		}
		
		return result;
	}
	
	@DMInit(group=CURRENT_TRACK_GROUP)
	public void initCurrentTrack() {
		try {
			currentTrack = musicSystem.getCurrentTrack(AnonymousViewpoint.getInstance(Site.NONE), user);
			int duration = currentTrack.getTrack().getDuration();
			
			// A negative duration means "unknown". We also treat durations of over an hour as suspect,
			// and substitute a default duration for determining if the track is still playing
			if (duration <= 0 || duration > 60 * 60) {
				duration = 30 * 60;
			}
			
			// While we don't try to notify when tracks stop playing, we want to omit past tracks
			// to avoid doing web-services work to get the details of "current" tracks that were
			// played months ago.
			if (currentTrack.getLastUpdated().getTime() + duration * 1000L <  System.currentTimeMillis())
				currentTrack = null;

		} catch (NotFoundException e) {
		}
	}
	
	@DMProperty(group=CURRENT_TRACK_GROUP)
	public TrackDMO getCurrentTrack() {
		if (currentTrack != null)
			return session.findUnchecked(TrackDMO.class, currentTrack.getTrack().getId());
		else
			return null;
	}
	
	@DMProperty(group=CURRENT_TRACK_GROUP)
	public long getCurrentTrackPlayTime() {
		if (currentTrack != null)
			return currentTrack.getLastUpdated().getTime();
		else
			return -1;
	}

	@DMProperty
	public DMFeed<BlockDMO> getStack() {
		return new BlockFeed();
	}
	
	private class BlockFeed implements DMFeed<BlockDMO> {
		public Iterator<DMFeedItem<BlockDMO>> iterator(int start, int max, long minTimestamp) {
			List<UserBlockData> blocks = stacker.getStackBlocks(user, start, max, minTimestamp);
			
			List<DMFeedItem<BlockDMO>> items = new ArrayList<DMFeedItem<BlockDMO>>(); 
			for (UserBlockData ubd : blocks) {
				Class<? extends BlockDMO> dmoClass = BlockDMO.getDMOClass(ubd.getBlock().getBlockType());
				if (dmoClass != null) {
					logger.debug("dmoClass for {} is {}", ubd.getBlock().getBlockType(), dmoClass.getName());
					
					BlockDMO blockDMO = session.findUnchecked(dmoClass, new BlockDMOKey(ubd.getBlock()));
					items.add(new DMFeedItem<BlockDMO>(blockDMO, ubd.getStackTimestampAsLong()));
				}
			}
			
			return items.iterator();
		}
	}
}
