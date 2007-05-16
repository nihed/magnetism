package com.dumbhippo.persistence;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

@Entity
@Indexed(index="group")
@Table(name="HippoGroup") // "Group" is a sql command so default name breaks things
public class Group extends GuidPersistable implements VersionedEntity {
	private static final long serialVersionUID = 1L;
	
	private GroupAccess access;
	private String name;
	private int version;
	private String description;
	private String stockPhoto;
	private Set<GroupMember> members;
	private Set<GroupFeed> feeds;
	private boolean markedForDelete;
		
	protected Group() {
	    this("", GroupAccess.PUBLIC);	
	}
	
	public Group(String name, GroupAccess access) {
		this.name = name;
		this.access = access;
		members = new HashSet<GroupMember>();
		feeds = new HashSet<GroupFeed>();
	}
	
	@Column(nullable=false)
	public GroupAccess getAccess() {
		return access;
	}

	public void setAccess(GroupAccess type) {
		this.access = type;
	}

	protected boolean isMarkedForDelete() {
		return markedForDelete;
	}

	public void setMarkedForDelete(boolean markedForDelete) {
		this.markedForDelete = markedForDelete;
	}

	@OneToMany(mappedBy="group")
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	public Set<GroupMember> getMembers() {
		return members;
	}
	
	/**
	 * Only hibernate should call this probably
	 * @param members
	 */
	protected void setMembers(Set<GroupMember> members) {
		if (members == null)
			throw new IllegalArgumentException("null");
		this.members = members;
	}
	
	@Field(index=Index.TOKENIZED, store=Store.NO)
	@Column(nullable=false)
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Column(nullable=false)
	public int getVersion() {
		return version;
	}
	
	public void setVersion(int version) {
		this.version = version;
	}
	
	@Field(index=Index.TOKENIZED, store=Store.NO)
	@Column(nullable=true)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column(nullable=true)
	public String getStockPhoto() {
		return stockPhoto;
	}
	
	public void setStockPhoto(String stockPhoto) {
		// last-ditch check, we also want to check closer to where we got the 
		// photo from (e.g. on input from the wire)
		if (stockPhoto != null && !Validators.validateStockPhoto(stockPhoto))
			throw new RuntimeException("Set invalid stock photo on Group");
		this.stockPhoto = stockPhoto;
	}
	
	@Transient
	public boolean isPublic() {
	    return (access == GroupAccess.PUBLIC || access == GroupAccess.PUBLIC_INVITE);	
	}
	
	@Transient
	public String getPhotoUrl() {
		if (stockPhoto != null) {
			return "/images2" + stockPhoto;
		} else {
			return "/files/groupshots/" + getId() + "?v=" + version;
		}
	}
	
	@OneToMany(mappedBy="group")
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	public Set<GroupFeed> getFeeds() {
		return feeds;
	}
	
	/**
	 * Only hibernate should call this probably
	 * @param feeds
	 */
	protected void setFeeds(Set<GroupFeed> feeds) {
		if (feeds == null)
			throw new IllegalArgumentException("null");
		this.feeds = feeds;
	}
	
	/**
	 * Convert an (unordered) set of groups into a a list and
	 * sort alphabetically with the default collator. You generally
	 * want to do this before displaying things to user, since
	 * iteration through Set will be in hash table order.
	 * 
	 * @param groups a set of Group objects
	 * @return a newly created List containing the sorted groups
	 */
	static public List<Group> sortedList(Set<Group> groups) {
		ArrayList<Group> list = new ArrayList<Group>();
		list.addAll(groups);

		final Collator collator = Collator.getInstance();
		Collections.sort(list, new Comparator<Group>() {
			public int compare (Group g1, Group g2) {
				return collator.compare(g1.getName(), g2.getName());
			}
		});
		
		return list;
	}
	
	@Override
	public String toString() {
		return "{Group " + "guid = " + getId() + " name = " + getName() + " access = " + getAccess() + "}";
	}	
}
