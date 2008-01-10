package com.dumbhippo.server.views;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.TypeFilteredCollection;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.LiveUser;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.FacebookResource;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.VersionedEntity;
import com.dumbhippo.persistence.XmppResource;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.dm.UserDMO;
import com.dumbhippo.web.ListBean;

/**
 * @author otaylor
 *
 * This is a class encapsulating information about a Person that can be
 * returned out of the session tier and used by web pages; only the
 * constructor makes queries into the database; the read-only properties of 
 * this object access pre-computed data.
 * 
 * This class is a person as viewed by another person; it differs from
 * PersonView primarily in not being a session bean.
 */
public class PersonView extends EntityView {
	private final static Logger logger = GlobalSetup.getLogger(PersonView.class);
	
	public static final int MAX_SHORT_NAME_LENGTH = 12;
	
	private Contact contact;
	private User user;
	private Set<Resource> resources;
	private EnumSet<PersonViewExtra> extras;
	private boolean invited; 
	private String preferredName;
	private String fallbackName;
	private Guid fallbackIdentifyingGuid;
	private String bioAsHtmlCached;
	private String musicBioAsHtmlCached;
	private boolean viewOfSelf;
	private boolean online;
	private boolean isContactOfViewer;
	private Set<ExternalAccountView> externalAccountViews;
	private List<TrackView> trackHistory;
	private String aimPresenceKey;
	private boolean viewerIsContact;
	private InvitationView invitationView;
	private boolean isSpecialCharacter;
	
	private void addExtras(EnumSet<PersonViewExtra> more) {
		if (extras == null)
			this.extras = more;
		else 
			this.extras.addAll(more);
	}
	
	public boolean hasExtra(PersonViewExtra extra) {
		if (extras == null)
			return false;
		return extras.contains(extra);
	}
	
	/**
	 * Construct a new PersonView object representing a view of a particular
	 * person by another object. Use PersonViewer.getPersonView() rather than
	 * this function. Normally, user and contact will be set once the PersonView
	 * is created, but BOTH contact and user can be null...
	 * 
	 * FIXME we have this "primary resource" thing but we don't really keep 
	 * track of any such concept in the db, we just pick one at random...
	 */
	public PersonView() {
	}
	
	private Set<Resource> getResources() {
		if (this.resources == null) {
			this.resources = new HashSet<Resource>();
		}
		return this.resources;
	}
	
	public void addAllResources(Collection<Resource> resources) {
		this.getResources().addAll(resources);
	}
	
	public void addInvitedStatus(boolean invited) {
		addExtras(EnumSet.of(PersonViewExtra.INVITED_STATUS));
		this.invited = invited;
	}
	
	/**
	 * Gets the id that should be used with person.jsp, which is 
	 * the User if any else null. Contacts without accounts don't have a public page.
	 * 
	 * @return the id or null
	 */
	public String getViewPersonPageId() {
		if (user != null)
			return user.getId();
		else
			return null;
	}
	
	public void addExternalAccountViews(Set<ExternalAccountView> externalAccountViews) {
		if (externalAccountViews == null)
			throw new IllegalArgumentException("can't add null internal accounts set");
		addExtras(EnumSet.of(PersonViewExtra.EXTERNAL_ACCOUNTS));
		this.externalAccountViews = externalAccountViews;
	}
	
	public UserDMO getDmo() {
		if (user != null) {
			UserDMO dmo = DataService.getModel().currentSessionRO().findUnchecked(UserDMO.class, user.getGuid());
			return dmo;
		}
		return null;
	}
	
	/**
	 * Gets the account associated with this person, or null if none.
	 * 
	 * @return the account or null
	 */	
	public Account getAccount() {
		if (user != null)
			return user.getAccount();
		return null;
	}
	
	public boolean isInvited() {
		if (!hasExtra(PersonViewExtra.INVITED_STATUS))
			throw new IllegalStateException("asked for " + PersonViewExtra.INVITED_STATUS + " but this PersonView wasn't created with that");
		return invited;
	}
	
	private String getNickname() {
		String name = preferredName;
		
		if ((name == null || name.length() == 0) && user != null)
			name = user.getNickname();
		
		// Contacts just should not have a nickname field, unless/until 
		// we ever have a way for people to set it to something. So 
		// we ignore this (but left commented out to show logically 
		// how it would work, until we drop the field for good)
		/*
		if ((name == null || name.length() == 0) && contact != null)
			name = contact.getNickname();	
        */
		
		// we may not have had a User, we try to use a resource instead
		// or the "fallback name"
		// PrimaryResource will not be included or will be null if the viewer should
		// not see it
		if (name == null || name.length() == 0) {
			Resource primaryResource = getPrimaryResource();
			if (primaryResource == null) {
				// try fallback name then
				if (fallbackName != null) {
					name = fallbackName;
				} else  {
                    // TODO: check if this happens when you aren't logged in and we create a PersonView for anonymous
					logger.warn("PersonView has no User, Resource, or fallback name; totally useless: " + this);
				}
			} else {
				if (primaryResource instanceof FacebookResource && fallbackName != null && fallbackName.trim().length() > 0)
					name = fallbackName;
				else
                    name = primaryResource.getHumanReadableString();
			}
		}
		
		if (name == null || name.length() == 0) {
			return "<Unknown>";
		}

		return name;
	}
	
	@Override
	public String getName() {
		return getNickname();
	}
	
	public String getTruncatedName() {
		String name = getName();
		return StringUtils.truncateString(name, MAX_SHORT_NAME_LENGTH);	
	}
	
	/**
	 * @param contact the contact the person is being viewed as
	 */
	public void setContact(Contact contact) {
		this.contact = contact;
	}
	
	public Contact getContact() {
		return contact;
	}
	
	/**
	 * @param user the user corresponding to the contact
	 */
	public void setUser(User user) {
	    this.user = user;		
	}
	
	public User getUser() {
		return user;
	}
	
	public LiveUser getLiveUser() {
		if (user == null)
			return null;
		LiveState state = LiveState.getInstance();
		return state.getLiveUser(user.getGuid());
	}
	
	private <T extends Resource> T getOne(Class<T> resourceClass) {
		for (Resource r : getResources()) {
			if (resourceClass.isAssignableFrom(r.getClass()))
				return resourceClass.cast(r);
		}
		
		return null;
	}
	
	private <T extends Resource> Collection<T> getMany(Class<T> resourceClass) {
		return new TypeFilteredCollection<Resource,T>(getResources(), resourceClass);
	}
	
	public Resource getPrimaryResource() {
		Resource resource;
		
		resource = getEmail();
		if (resource != null)
			return resource;
		resource = getAim();
		if (resource != null)
			return resource;
		
		return getOne(Resource.class);
	}
	
	public EmailResource getEmail() {
		return getOne(EmailResource.class);
	}
	
	public String getEmailLink() {
		EmailResource email = getEmail();
		if (email == null)
			return null;
		return "mailto:" + StringUtils.urlEncodeEmail(email.getEmail());
	}
	
	public AimResource getAim() {
		return getOne(AimResource.class);
	}
	
	public void setAimPresenceKey(String aimPresenceKey) {
		this.aimPresenceKey = aimPresenceKey;
	}
	
	public String getAimPresenceImageLink() {
		AimResource aim = getAim();
		if (aim == null || aimPresenceKey == null ||( aimPresenceKey.length() == 0))
			return null;
		String aimName = aim.getScreenName();

		return "http://api.oscar.aol.com/SOA/key=" + aimPresenceKey + "/presence/" + aimName;
	}
	
	public String getAimLink() {
		AimResource aim = getAim();
		if (aim == null)
			return null;
		String aimName = aim.getScreenName();
		return "aim:GoIM?screenname=" + StringUtils.urlEncode(aimName);
	}
	
	public Collection<EmailResource> getAllEmails() {
		return getMany(EmailResource.class);
	}
	
	public Collection<AimResource> getAllAims() {
		return getMany(AimResource.class);
	}
	
	public Collection<XmppResource> getAllXmpps() {
		return getMany(XmppResource.class);
	}
	
	public Collection<Resource> getAllResources() {
		return getMany(Resource.class);
	}
	
	public Set<ExternalAccountView> getExternalAccountViews() {
		if (!hasExtra(PersonViewExtra.EXTERNAL_ACCOUNTS))
			throw new IllegalStateException("asked for " + PersonViewExtra.EXTERNAL_ACCOUNTS + " but this PersonView wasn't created with that, only with " + extras + " for " + this.hashCode());
		return externalAccountViews;
	}
	
	public ListBean<ExternalAccountView> getAccountsBySentiment(Sentiment sentiment) {
		List<ExternalAccountView> list = new ArrayList<ExternalAccountView>();
		if (sentiment == Sentiment.LOVE) {
			for (ExternalAccountView a : getExternalAccountViews()) {
				if (a.getExternalAccount().isLovedAndEnabled())
					list.add(a);
			}
		} else {
			for (ExternalAccountView a : getExternalAccountViews()) {
				if (a.getExternalAccount().getSentiment() == sentiment) {
					list.add(a);
				}
			}			
		}
		Collections.sort(list, new Comparator<ExternalAccountView>() {
			public int compare(ExternalAccountView first, ExternalAccountView second) {
				return ExternalAccount.compare(first.getExternalAccount(), second.getExternalAccount());	
			}			
		});
		return new ListBean<ExternalAccountView>(list);
	}
	
	public ListBean<ExternalAccountView> getLovedAccounts() {
		return getAccountsBySentiment(Sentiment.LOVE);
	}
	
	public ListBean<ExternalAccountView> getHatedAccounts() {
		return getAccountsBySentiment(Sentiment.HATE);
	}
	
	@Override
	public String toString() {
		return "{PersonView " + contact + " " + user + " invited = " + invited + " extras = " + extras + " resources = " + resources + "}";
	}
	
	public void setCurrentTrack(TrackView currentTrack) {
		this.trackHistory = new ArrayList<TrackView>();
		this.trackHistory.add(currentTrack);
	}
	
	public TrackView getCurrentTrack() {
		if (trackHistory != null)
			return trackHistory.get(0);
		return null;
	}
	
	public void setTrackHistory(List<TrackView> history) {
		this.trackHistory = history;
	}
	
	public List<TrackView> getTrackHistory() {
		return this.trackHistory;
	}
	
	/**
	 * Divide an (unordered) set of PersonView into lists based on the 
	 * type of person, then, if the sort flag is on, sort alphabetically 
	 * with the default collator. The types of people are: people who 
	 * have accounts, people who have been invited, people who need 
	 * invites, and people for whom their invited status is not set. 
	 * Return the list that contains the four lists. 
	 * 
	 * @param views a set of PersonView objects
	 * @param sort a flag indicating whether to sort the formed lists alphabetically
	 * @return a List of created Lists
	 */
	static private <T extends PersonView> List<List<T>> generatePeopleLists(Set<T> views, boolean sort) {	
		ArrayList<T> listOfUsers = new ArrayList<T>();
		ArrayList<T> listOfPeopleWithInvites = new ArrayList<T>();
		ArrayList<T> listOfPeopleWithNoInvites = new ArrayList<T>();
		ArrayList<T> listOfPeopleWithNoInvitedStatus = new ArrayList<T>();
		
		for (T pv : views) {
			if (pv.getUser() != null) {
  		        listOfUsers.add(pv);
			} else if (!pv.hasExtra(PersonViewExtra.INVITED_STATUS)) {
				listOfPeopleWithNoInvitedStatus.add(pv);				
			} else if (pv.isInvited()) {
			    listOfPeopleWithInvites.add(pv);
			} else {
				listOfPeopleWithNoInvites.add(pv);
			}
		}
		
		if (sort) {
		    final Collator collator = Collator.getInstance();
		    Comparator<PersonView> comparator = 
			    new Comparator<PersonView>() {
			        public int compare (PersonView v1, PersonView v2) {
				        return collator.compare(v1.getName(), v2.getName());
			        }
		        };
		    
		    Collections.sort(listOfUsers, comparator);
		    Collections.sort(listOfPeopleWithInvites, comparator);
		    Collections.sort(listOfPeopleWithNoInvites, comparator);
		    Collections.sort(listOfPeopleWithNoInvitedStatus, comparator);
		}
	  
		ArrayList<List<T>> listOfLists = new ArrayList<List<T>>();
		listOfLists.add(listOfUsers);
		listOfLists.add(listOfPeopleWithInvites);
		listOfLists.add(listOfPeopleWithNoInvites);
		listOfLists.add(listOfPeopleWithNoInvitedStatus);
		
		return listOfLists;
	}
	
	/**
	 * Divide an (unordered) set of PersonView into lists based on the 
	 * type of person, then sort alphabetically with the default collator. 
	 * The types of people are: people who have accounts, people who have 
	 * been invited, people who need invites, and people for whom their 
	 * invited status is not set. Return the list that combines the members
	 * of the four lists in the above order. You generally want to do this 
	 * before displaying things to user, since iteration through Set will 
	 * be in hash table order.
	 * 
	 * @param views a set of PersonView objects
	 * @return a newly created List containing the sorted people
	 */
	static public <T extends PersonView> List<T> sortedList(Set<T> views) {
		
		List<List<T>> listOfLists = generatePeopleLists(views, true);
		
		// combine the above lists in the list to return
		ArrayList<T> list = new ArrayList<T>();
		list.addAll(listOfLists.get(0));
		list.addAll(listOfLists.get(1));
		list.addAll(listOfLists.get(2));
		list.addAll(listOfLists.get(3));
		
		return list;
	}
	
	/**
	 * This method is a quick hack to display non-users only if the
	 * user is viewing his own contacts. If we decide to display non-users
	 * to others, we should truncate their e-mail addresses,
	 * but currently non-users end up being displayed as "<Unknown>"   
	 * to others, which ain't pretty. If we do not want to display non-users
	 * to others, we can have getContacts() of IdentitySpider return only 
	 * the contacts the viewpoint is allowed to see.
	 * 
	 * Divide an (unordered) set of PersonView into lists based on the 
	 * type of person, then sort alphabetically with the default collator. 
	 * The types of people are: people who have accounts, people who have 
	 * been invited, people who need invites, and people for whom their 
	 * invited status is not set. Return the list that combines the members
	 * of the four lists in the above order. You generally want to do this 
	 * before displaying things to user, since iteration through Set will 
	 * be in hash table order.
	 * 
	 * @param viewpoint
	 * @param viewedUser
	 * @param views a set of PersonView objects
	 * @return a newly created List containing the sorted people
	 */
	static public <T extends PersonView> List<T> sortedList(Viewpoint viewpoint, User viewedUser, Set<T> views) {
		
		List<List<T>> listOfLists = generatePeopleLists(views, true);
		
		// combine the above lists in the list to return
		ArrayList<T> list = new ArrayList<T>();
		list.addAll(listOfLists.get(0));
		if (viewpoint.isOfUser(viewedUser)) {
		    list.addAll(listOfLists.get(1));
		    list.addAll(listOfLists.get(2));
		    list.addAll(listOfLists.get(3));
		}
		
		return list;
	}
	
	/**
	 * Divide an (unordered) set of PersonView into lists based on the 
	 * type of person, then sort alphabetically with the default collator. 
	 * The types of people are: people who have accounts, people who have 
	 * been invited, people who need invites, and people for whom their 
	 * invited status is not set. Return the list that contains a subset
	 * of a list with the members of the four lists in the above order. 
	 * The subset is defined by the index of the element to start with 
	 * (1-based), number of rows the elements need to fill in, and the 
	 * number of different types of elements that fit per one row.  
	 * 
	 * @param views a set of PersonView objects
	 * @param start an index of element to start with, the order is 1-based, 
	 *              0 is treated as 1
	 * @param rows number of rows elements are expected to fill in
	 * @param usersPerRow number of users that fit per one row
	 * @param nonUsersPerRow number of non-users that fit per one row
	 * @return a newly created List containing the specified sorted subset 
	 *         of people
	 */
	static public <T extends PersonView> List<T> sortedList(Set<T> views, int start, 
			                                  int rows, int usersPerRow, 
			                                  int nonUsersPerRow) {
		
		List<List<T>> listOfLists = generatePeopleLists(views, true);

		ArrayList<T> listOfUsers = (ArrayList<T>)listOfLists.get(0);
		ArrayList<T> listOfPeopleWithInvites = (ArrayList<T>)listOfLists.get(1);
		ArrayList<T> listOfPeopleWithNoInvites = (ArrayList<T>)listOfLists.get(2);
		ArrayList<T> listOfPeopleWithNoInvitedStatus = (ArrayList<T>)listOfLists.get(3);
		
		// combine appropriate members of the above lists in the list to return
		ArrayList<T> list = new ArrayList<T>();
		
		int count = 0;
		int index = start-1;
		if (start == 0) {
			index = 0;
		}
		
		while ((listOfUsers.size() > index) && (count < rows*usersPerRow)) {
				list.add(listOfUsers.get(index));
				count++;
				index++;
		}
		
		// this is an integer division
		int rowsUsed = count/usersPerRow;
		
		// want to add one if there was a row that was partially used
		if (rowsUsed*usersPerRow < count) {
			rowsUsed++;
		}
	
		int spacesLeft = (rows-rowsUsed)*nonUsersPerRow;
		
		if (count > 0) {
			index = 0;
		} else {
			index = index-listOfUsers.size();
		}
				
		while ((listOfPeopleWithInvites.size() > index) && (spacesLeft > 0)) {
			list.add(listOfPeopleWithInvites.get(index));
			spacesLeft--;
			count++;
			index++;
		}
		
		if (count > 0) {
			index = 0;
		} else {
			index = index-listOfPeopleWithInvites.size();
		}		
		
		while ((listOfPeopleWithNoInvites.size() > index) && (spacesLeft > 0)) {
			list.add(listOfPeopleWithNoInvites.get(index));
			spacesLeft--;
			count++;
			index++;
		}
		
		if (count > 0) {
			index = 0;
		} else {
			index = index-listOfPeopleWithNoInvites.size();
		}		
		
		while ((listOfPeopleWithNoInvitedStatus.size() > index) && (spacesLeft > 0)) {
			list.add(listOfPeopleWithNoInvitedStatus.get(index));
			spacesLeft--;
			count++;
			index++;
		}
		
		return list;
	}
	
	/**
	 * Divide an (unordered) set of PersonView into lists based on the 
	 * type of person. The types of people are: people who have accounts, 
	 * people who have been invited, people who need invites, and people 
	 * for whom their invited status is not set. Then identify an 
	 * ndex of an appropriate start element, given the index of the stop 
	 * element and the layout constraints.
	 * 
	 * In this context, the stop element means that we want to get the 
	 * maximum number of elements it is possible to display up to and 
	 * including this element. More elements can be added past that
	 * element, if space allows. Therefore, we provide the function that 
	 * identifies a start element index, rather than the function that 
	 * assembles a list based on the stop element index.
	 * 
     * If a function that returns a list that ends with a given element
     * turns out to be useful, having a function just like this one and
     * assembling a list in it by adding elements to the front of it would 
     * do the trick.
	 * 
	 * @param views a set of PersonView objects
	 * @param stop an index of an element up to which we want to display a 
	 *             maximum possible number of elements, the order is 1-based
	 * @param rows number of rows elements are expected to fill in
	 * @param usersPerRow number of users that fit per one row
	 * @param nonUsersPerRow number of non-users that fit per one row
	 * @return an index of an appropriate start element
	 */
	static public int computeStart(Set<PersonView> views, int stop, 
                                   int rows, int usersPerRow, 
                                   int nonUsersPerRow) {	   
		// we do not need lists to be sorted for this function
		List<List<PersonView>> listOfLists = generatePeopleLists(views, false);

		ArrayList<PersonView> listOfUsers = (ArrayList<PersonView>)listOfLists.get(0);
		ArrayList<PersonView> listOfPeopleWithInvites = (ArrayList<PersonView>)listOfLists.get(1);
		ArrayList<PersonView> listOfPeopleWithNoInvites = (ArrayList<PersonView>)listOfLists.get(2);
		ArrayList<PersonView> listOfPeopleWithNoInvitedStatus = (ArrayList<PersonView>)listOfLists.get(3);
		
        int index = listOfPeopleWithNoInvitedStatus.size()-1;
		int spacesLeft = rows*nonUsersPerRow;
		
		int sizeOfFirstTwoLists =  listOfUsers.size() + listOfPeopleWithInvites.size();
		int sizeOfFirstThreeLists = sizeOfFirstTwoLists + listOfPeopleWithNoInvites.size();
		// should be the same as the size of the views Set
		int sizeOfAllFourLists = sizeOfFirstThreeLists + listOfPeopleWithNoInvitedStatus.size();

        // if stop is less than or equal to the sizeOfFirstThreeLists, the index will be negative,
        // an we won't add any elements in the while loop
        if (stop < sizeOfAllFourLists ) {
        	index = stop - sizeOfFirstThreeLists - 1;
        }
        while ((index >= 0) && (spacesLeft > 0)) {
        	// imagine adding listOfPeopleWithNoInvitedStatus.get(index) to the front of the list
        	index--;
        	spacesLeft--;
        	
        }
        
        index = listOfPeopleWithNoInvites.size()-1;
        if  (stop < sizeOfFirstThreeLists) {
        	index = stop - sizeOfFirstTwoLists - 1;
        }
    	while ((index >= 0) && (spacesLeft > 0)) {
        	// imagine adding listOfPeopleWithNoInvites.get(index) to the front of the list
    		index--;
    		spacesLeft--;
    	}
    	
        index = listOfPeopleWithInvites.size()-1;
        if  (stop < sizeOfFirstTwoLists) {
        	index = stop - listOfUsers.size() - 1;
        }
    	while ((index >= 0) && (spacesLeft > 0)) {
        	// imagine adding listOfPeopleWithInvites.get(index) to the front of the list
    		index--;
    		spacesLeft--;
    	}        
    	
    	int spacesUsed = (rows*nonUsersPerRow)-spacesLeft;
    	// this is an integer division, so it would round down
    	int rowsUsed = spacesUsed/nonUsersPerRow;
        if (rowsUsed*nonUsersPerRow < spacesUsed) {
            rowsUsed++;	
        }
        
        int count = 0;
        index = listOfUsers.size()-1;
        if (stop < listOfUsers.size()) {
        	index = stop - 1;
        }
    	while ((index >= 0) && (count < (rows-rowsUsed)*usersPerRow)) {
        	// imagine adding listOfUsers.get(index) to the front of the list
    		index--;
    		count++;
    	}        
        
    	// spacesUsed + count is how many items we definitely want to display
    	int start = stop - spacesUsed - count + 1; 
    	if (stop > sizeOfAllFourLists) {
    		start = sizeOfAllFourLists - spacesUsed - count + 1;
    	}
    	
        return start;
    }
	
	/**
	 * Is this user currently online?
	 * 
	 * @return true if user is online, false otherwise
	 */
	public boolean isOnline() {
		return online;
	}
	
	public String getOnlineIcon() {
		if (isOnline())
			return "/images3/online_icon.png";
		else
			return "/images3/offline_icon.png";
	}

	public String getOnlineIconForPhoto() {
		if (isOnline())
			return "/images3/online_icon_for_photo.png";
		else
			return "/images3/offline_icon_for_photo.png";
	}
	
	public void setOnline(boolean online) {
		this.online = online;
	}

	@Override
	protected VersionedEntity getVersionedEntity() {
		return user;
	}
		
	/**
	 * This method appends an XML node <user> if the view is of 
	 * a user, and otherwise appends a <resource> node
	 */	
	@Override
	public void writeToXmlBuilderOld(XmlBuilder builder) {
		if (user != null) {
			builder.appendTextNode("user", "", "id", user.getId(), 
				               	   "name", getName(),
				               	   "homeUrl", getHomeUrl(), 
				               	   "smallPhotoUrl", getPhotoUrl(),
				               	   "isContact", hasExtra(PersonViewExtra.CONTACT_STATUS) ? Boolean.toString(isContactOfViewer()) : null);
		} else {
			builder.appendTextNode("resource", "", "id", getIdentifyingGuid().toString(), "name", getName());
		}		
	}
	
	public void writeToXmlBuilder(XmlBuilder builder) {
		if (user != null) {
			builder.openElement("user", 
								"id", user.getId(), 
				               	"name", getName(),
				               	"homeUrl", getHomeUrl(), 
				               	"photoUrl", getPhotoUrl(),
				               	"isContact", hasExtra(PersonViewExtra.CONTACT_STATUS) ? Boolean.toString(isContactOfViewer()) : null);
		} else {
			builder.openElement("resource", 
					            "id", getIdentifyingGuid().toString(), 
					            "name", getName(),
					            "isContact", hasExtra(PersonViewExtra.CONTACT_STATUS) ? Boolean.toString(isContactOfViewer()) : null);
		}
		
		if (getCurrentTrack() != null)
			getCurrentTrack().writeToXmlBuilder(builder, "currentTrack");
		
		builder.closeElement();
	}

	/**
	 * This method gives an XML fragment sufficient for identifying the 
	 * person or resource being viewed.
	 * 
	 * @return an XML fragment
	 */
	@Override
	public String toIdentifyingXml() {
		XmlBuilder builder = new XmlBuilder();		
		if (user != null) {
			builder.appendTextNode("user", "", "id", user.getId());
		} else {
			builder.appendTextNode("resource", "", "id", getIdentifyingGuid().toString());
		}
		return builder.toString();					
	}
	
	/**
	 * This method returns the identifying Guid of the person or
	 * resource being viewed, see toIdentifyingXml
	 * 
	 * @return an identifying Guid
	 */
	@Override
	public Guid getIdentifyingGuid() {
		if (user != null) {
			return user.getGuid();
		} else if (getPrimaryResource() != null) {
			return getPrimaryResource().getGuid();
		} else if (fallbackIdentifyingGuid != null){
			return fallbackIdentifyingGuid;
		} else {
			/* FIXME this is really not supposed to happen; it happens when 
			 * someone's own contact has no associated resources, in which case
			 * IdentitySpiderBean doesn't set a fallbackIdentifyingGuid
			 * 
			 * This avoids a crash on hashCode(), but probably has other issues
			 * (e.g. toIdentifyingXml() still doesn't work) and so needs a better
			 * fix of some kind ... not sure I grok it fully
			 */
			logger.warn("PersonView with no fallback {}", this);
			if (contact == null)
				throw new RuntimeException("No guid whatsoever on personview, " + this);
			return contact.getGuid();
		}
	}

	/**
	 * This method returns a contact or user id, in that order of preference,
	 * or null.
	 *
	 * @return a contact or user id
	 */
	public Guid getPersonIdentifyingGuid() {
		if (contact != null)
			return contact.getGuid();
		else if (user != null)
			return user.getGuid();
		else
			return null;		
	}
	
	@Override
	public String getHomeUrl() {
		if (user != null)
			return "/person?who=" + user.getId();
		else
			return null;
	}
	
	@Override
	public String getPhotoUrl() {
		if (user != null)
			return user.getPhotoUrl();
		else 
			return "/images2/user_pix1/nonuser.png";
	}
	
	/**
	 * If the PersonView has no User or resources stored in it, then this should be set to the guid of a 
	 * resource that the contact owns.
	 * 
	 * @param guid the fallback guid used if there's no user or resources
	 */
	public void setFallbackIdentifyingGuid(Guid guid) {
		fallbackIdentifyingGuid = guid;
	}
	
	/**
	 * Sometimes the dafault name we can store for the user is not 
	 * that great (e.g. "Facebook user 123456"), and we can only
	 * get the preferred name on the fly.
	 * 
	 * @return the preferred name or null
	 */
	public String getPreferredName() {
		return preferredName;
	}

	public void setPreferredName(String preferredName) {
		this.preferredName = preferredName;
	}
	
	/**
	 * A name we use if we don't have anything else (no contact/user/resources),
	 * typically null but we set it sometimes when we know we'll need it.
	 * Specifically this was added when viewing someone else's contact we 
	 * create a fallback name which is a truncated/obfuscated version of one
	 * of the resources on the contact
	 * 
	 * @return the fallback name or null
	 */
	public String getFallbackName() {
		return fallbackName;
	}

	public void setFallbackName(String fallbackName) {
		this.fallbackName = fallbackName;
	}
	
	public String getBioAsHtml() {
		// the cache is because jsp's can be expected to test this for null
		// then use it, and we don't want to do the htmlizing twice
		if (bioAsHtmlCached == null) {
			if (user == null)
				return null;
			String bio = user.getAccount().getBio();
			if (bio == null)
				return null;
			XmlBuilder xml = new XmlBuilder();
			xml.appendTextAsHtml(bio, null);
			bioAsHtmlCached = xml.toString(); 
		}
		return bioAsHtmlCached;
	}
	
	public String getMusicBioAsHtml() {
		// the cache is because jsp's can be expected to test this for null
		// then use it, and we don't want to do the htmlizing twice
		if (musicBioAsHtmlCached == null) {
			if (user == null)
				return null;
			String bio = user.getAccount().getMusicBio();
			if (bio == null)
				return null;
			XmlBuilder xml = new XmlBuilder();
			xml.appendTextAsHtml(bio, null);
			musicBioAsHtmlCached = xml.toString(); 
		}
		return musicBioAsHtmlCached;
	}
	
	public boolean isViewOfSelf() {
		return viewOfSelf;
	}
	
	public void setViewOfSelf(boolean viewOfSelf) {
		this.viewOfSelf = viewOfSelf;
	}

	public void setViewerIsContact(boolean isContact) {
		viewerIsContact = isContact;
	}
	
	public boolean getViewerIsContact() {
		return viewerIsContact;
	}

	public void setContactOfViewer(boolean isContact) {
		this.isContactOfViewer = isContact;
		addExtras(EnumSet.of(PersonViewExtra.CONTACT_STATUS));
	}
	
	public boolean isContactOfViewer() {
		if (!hasExtra(PersonViewExtra.CONTACT_STATUS))
			logger.warn("isContactofViewer called on a PersonView without the CONTACT_STATUS extra");
		return isContactOfViewer;
	}
	
	public void setInvitationView(InvitationView invitationView) {
		this.invitationView = invitationView;
	}
	
	public InvitationView getInvitationView() {
		return invitationView;
	}

	public void setSpecialCharacter(boolean isMugshotCharacter) {
		this.isSpecialCharacter = isMugshotCharacter;
		addExtras(EnumSet.of(PersonViewExtra.SPECIAL_CHARACTER_STATUS));
	}
	
	public boolean isSpecialCharacter() {
		if (!hasExtra(PersonViewExtra.SPECIAL_CHARACTER_STATUS))
			logger.warn("isMugshotCharacter called on a PersonView without the MUGSHOT_CHARACTER_STATUS extra");
		return isSpecialCharacter;
	}	
	
}
