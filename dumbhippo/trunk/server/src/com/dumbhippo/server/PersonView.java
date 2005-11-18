package com.dumbhippo.server;

import java.text.Collator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dumbhippo.TypeFilteredCollection;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;

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
public class PersonView {
	
	public static final int MAX_SHORT_NAME_LENGTH = 15;
	
	private Contact contact;
	private User user;
	private Set<Resource> resources;
	private BitSet extras;
	
	private void setExtra(PersonViewExtra extra) {
		if (extras == null)
			this.extras = new BitSet();
		extras.set(extra.ordinal());
	}
	
	private boolean getExtra(PersonViewExtra extra) {
		if (extras == null)
			return false;
		return extras.get(extra.ordinal());
	}
	
	/**
	 * Construct a new PersonView object representing a view of a particular
	 * person by another object. Use IdentitySpider.getPersonView() rather than
	 * this function. Note that BOTH contact and user can be null...
	 * 
	 * FIXME we have this "primary resource" thing but we don't really keep 
	 * track of any such concept in the db, we just pick one at random...
	 * 
	 * 
	 * @param contact the contact the person is being viewed as, or null if none
	 * @param person the user corresponding to the contact, or null if none
	 * @param extras info you want the PersonView to contain about the person 
	 */
	public PersonView(Contact contact, User user) {
		this.contact = contact;
		this.user = user;
	}
	
	private Set<Resource> getResources() {
		if (this.resources == null) {
			this.resources = new HashSet<Resource>();
		}
		return this.resources;
	}
	
	public void addAllResources(Collection<Resource> resources) {
		setExtra(PersonViewExtra.ALL_RESOURCES);
		setExtra(PersonViewExtra.ALL_EMAILS);
		setExtra(PersonViewExtra.ALL_AIMS);
		setExtra(PersonViewExtra.PRIMARY_EMAIL);
		setExtra(PersonViewExtra.PRIMARY_AIM);
		setExtra(PersonViewExtra.PRIMARY_RESOURCE);
		this.getResources().addAll(resources);
	}
	
	public void addAllEmails(Collection<EmailResource> resources) {
		setExtra(PersonViewExtra.ALL_EMAILS);
		setExtra(PersonViewExtra.PRIMARY_EMAIL);
		setExtra(PersonViewExtra.PRIMARY_RESOURCE);
		this.getResources().addAll(resources);
	}

	public void addAllAims(Collection<AimResource> resources) {
		setExtra(PersonViewExtra.ALL_AIMS);
		setExtra(PersonViewExtra.PRIMARY_AIM);
		setExtra(PersonViewExtra.PRIMARY_RESOURCE);
		this.getResources().addAll(resources);
	}

	public void addPrimaryEmail(EmailResource resource) {
		setExtra(PersonViewExtra.PRIMARY_EMAIL);
		setExtra(PersonViewExtra.PRIMARY_RESOURCE);
		if (resource != null)
			this.getResources().add(resource);
	}
	
	public void addPrimaryAim(AimResource resource) {
		setExtra(PersonViewExtra.PRIMARY_AIM);
		setExtra(PersonViewExtra.PRIMARY_RESOURCE);
		if (resource != null)
			this.getResources().add(resource);
	}
	
	public void addPrimaryResource(Resource resource) {
		// resource can be null
		
		if (resource instanceof AimResource)
			addPrimaryAim((AimResource) resource);
		else if (resource instanceof AimResource)
			addPrimaryEmail((EmailResource) resource);
		else {
			setExtra(PersonViewExtra.PRIMARY_RESOURCE);
			if (resource != null)
				this.getResources().add(resource);
		}
	}
	
	public Person getPerson() {
		return contact != null ? contact : user;
	}

	private String getNickname() {
		String name = null;
		
		// we prefer the user name, then the contact alias for now;
		// eventually we might get more sophisticated and just drop 
		// the contact alias "one time" when the user first appears

		if (user != null)
			name = user.getNickname();
		
		if ((name == null || name.length() == 0) && contact != null)
			name = contact.getNickname();	
		
		if (name == null || name.length() == 0)
			return "<Unknown>";

		return name;
	}
	
	public String getName() {
		return getNickname();
	}
	
	public String getTruncatedName() {
		String name = getNickname();
		
		if (name.length() > MAX_SHORT_NAME_LENGTH) {
			return name.substring(0, MAX_SHORT_NAME_LENGTH) + "...";
		} else {
			return name;
		}
	}
	
	public Contact getContact() {
		return contact;
	}
	
	public User getUser() {
		return user;
	}
	
	private <T extends Resource> T getOne(PersonViewExtra extra, Class<T> resourceClass) {
		if (!getExtra(extra))
			throw new IllegalStateException("asked for " + extra + " but this PersonView wasn't created with that");
		
		for (Resource r : getResources()) {
			if (resourceClass.isAssignableFrom(r.getClass()))
				return resourceClass.cast(r);
		}
		return null;
	}
	
	private <T extends Resource> Collection<T> getMany(PersonViewExtra extra, Class<T> resourceClass) {
		if (!getExtra(extra))
			throw new IllegalStateException("asked for " + extra + " but this PersonView wasn't created with that");
		
		return new TypeFilteredCollection<Resource,T>(getResources(), resourceClass);
	}
	
	public Resource getPrimaryResource() {
		if (getExtra(PersonViewExtra.PRIMARY_EMAIL))
			return getEmail();
		else if (getExtra(PersonViewExtra.PRIMARY_AIM))
			return getAim();
		else
			return getOne(PersonViewExtra.PRIMARY_RESOURCE, Resource.class);
	}
	
	public EmailResource getEmail() {
		return getOne(PersonViewExtra.PRIMARY_EMAIL, EmailResource.class);
	}
	
	public AimResource getAim() {
		return getOne(PersonViewExtra.PRIMARY_AIM, AimResource.class);
	}
	
	public Collection<EmailResource> getAllEmails() {
		return getMany(PersonViewExtra.ALL_EMAILS, EmailResource.class);
	}
	
	public Collection<AimResource> getAllAims() {
		return getMany(PersonViewExtra.ALL_AIMS, AimResource.class);
	}
	
	public Collection<Resource> getAllResources() {
		return getMany(PersonViewExtra.ALL_RESOURCES, Resource.class);
	}
	
	/**
	 * Convert an (unordered) set of PersonView into a a list and
	 * sort alphabetically with the default collator. You generally
	 * want to do this before displaying things to user, since
	 * iteration through Set will be in hash table order.
	 * 
	 * @param groups a set of Person objects
	 * @return a newly created List containing the sorted groups
	 */
	static public List<PersonView> sortedList(Set<PersonView> views) {
		ArrayList<PersonView> list = new ArrayList<PersonView>();
		list.addAll(views);

		final Collator collator = Collator.getInstance();
		Collections.sort(list, new Comparator<PersonView>() {
			public int compare (PersonView v1, PersonView v2) {
				return collator.compare(v1.getName(), v2.getName());
			}
		});
		
		return list;
	}
}
