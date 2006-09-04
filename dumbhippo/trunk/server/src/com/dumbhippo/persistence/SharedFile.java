package com.dumbhippo.persistence;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;

@Entity
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class SharedFile extends EmbeddedGuidPersistable {
	
	private User creator;
	private String name;
	private String mimeType;
	private long sizeInBytes;
	private long creationDate;
	private boolean worldReadable;
	private StorageState state;
	private Set<SharedFileGroup> groups;
	private Set<SharedFileUser> users;
	
	protected SharedFile() {
		groups = new HashSet<SharedFileGroup>();
		users = new HashSet<SharedFileUser>();
		state = StorageState.NOT_STORED;
	}
	
	public SharedFile(User creator, String name, String mimeType,
			long sizeInBytes, boolean worldReadable, Collection<Group> groups,
			Collection<User> users) {
		this();
		
		if (creator == null || mimeType == null || sizeInBytes < 0)
			throw new IllegalArgumentException("bad args to SharedFile constructor");
		
		this.creator = creator;
		this.name = name;
		this.mimeType = mimeType;
		this.sizeInBytes = sizeInBytes;
		this.worldReadable = worldReadable;

		if (groups != null) {
			for (Group g : groups) {
				this.groups.add(new SharedFileGroup(this, g));
				
				// force world-readable if sharing with a public group,
				// this way queries can just look at the world readable flag
				if (g.getAccess() != GroupAccess.SECRET)
					this.worldReadable = true;
			}
		}
		if (users != null) {
			for (User u : users) {
				this.users.add(new SharedFileUser(this, u));
			}
		}
		// always put the creator in the set of users to simplify 
		// queries
		this.users.add(new SharedFileUser(this, creator));
		
		this.creationDate = System.currentTimeMillis();
	}

	@ManyToOne
	@JoinColumn(nullable=false)
	public User getCreator() {
		return creator;
	}


	public void setCreator(User creator) {
		this.creator = creator;
	}

	@Column(nullable=false)
	public String getMimeType() {
		return mimeType;
	}


	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	@Column(nullable=false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(nullable=false)
	public long getSizeInBytes() {
		return sizeInBytes;
	}

	public void setSizeInBytes(long size) {
		this.sizeInBytes = size;
	}

	@Column(nullable=false)
	public Date getCreationDate() {
		return creationDate >= 0 ? new Date(creationDate) : null;
	}
	
	public void setCreationDate(Date creationDate) {
		if (creationDate != null)
			this.creationDate = creationDate.getTime();
		else
			this.creationDate = -1;
	}
	
	// lazy fetch since if we know the file is world readable,
	// or we queried by user/group to begin with, there may 
	// be no point loading this
	@OneToMany(mappedBy="file",fetch=FetchType.LAZY)
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	public Set<SharedFileGroup> getGroups() {
		return groups;
	}


	protected void setGroups(Set<SharedFileGroup> groups) {
		this.groups = groups;
	}	

	// lazy fetch since if we know the file is world readable,
	// or we queried by user/group to begin with, there may 
	// be no point loading this
	@OneToMany(mappedBy="file",fetch=FetchType.LAZY)
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	public Set<SharedFileUser> getUsers() {
		return users;
	}

	protected void setUsers(Set<SharedFileUser> users) {
		this.users = users;
	}

	@Column(nullable=false)
	public boolean isWorldReadable() {
		return worldReadable;
	}

	public void setWorldReadable(boolean worldReadable) {
		this.worldReadable = worldReadable;
	}	
	
	@Override
	public String toString() {
		// this is for debug spew
		return "{SharedFile " + getId() + " created by " + getCreator() + "}";
	}

	@Column(nullable=false)
	public StorageState getState() {
		return state;
	}

	public void setState(StorageState state) {
		this.state = state;
	}
	
	// for use in a jsp
	@Transient
	public String getRelativeUriAsHtml() {
		return XmlBuilder.escape(StringUtils.urlEncode(getName())) + "?id=" + getId(); 
	}
}
