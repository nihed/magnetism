package com.dumbhippo.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * This class stores information about the membership of one person 
 * in one group
 * 
 * @author otaylor
 */
@Entity
@Table(name="GroupMember", 
	   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"group_id", "member_id"})}
	   )
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)	   
public class GroupMember extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private Group group;
	private Resource member;
	private MembershipStatus status;
	private Set<User> adders;
	
	protected GroupMember() {
		this(null, null, null);
	}
	
	public GroupMember(Group group, Resource member, MembershipStatus status) {
		this.group = group;
		this.member = member;		
		this.status = status;
		this.adders = new HashSet<User>();
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Group getGroup() {
		return group;
	}
	
	public void setGroup(Group g) {
		group = g;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Resource getMember() {
		return member;
	}
	
	public void setMember(Resource member) {
		this.member = member;
	}
	
	@Column(nullable=false)
	public MembershipStatus getStatus() {
		return status;
	}
	
	public void setStatus(MembershipStatus s) {
		status = s;
	}
	
    // This is a @ManyToMany relationship because:
	// -- a GroupMember can have multiple adders
	// -- an adder can add multiple GroupMembers
	@ManyToMany
	@JoinTable(name="GroupMember_HippoUser",
               uniqueConstraints={@UniqueConstraint(columnNames={"groupMember_id", "adder_id"})},
               joinColumns=@JoinColumn(name="groupMember_id", referencedColumnName="id"),               
               inverseJoinColumns=@JoinColumn(name="adder_id", referencedColumnName="id"))	
    @Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	public Set<User> getAdders() {
		return adders;
	}
	
	public void setAdders(Set<User> adders) {
		if (adders == null)
			throw new IllegalArgumentException("adders must not be null ");
		this.adders = adders;
	}
	
	public void addAdder(User adder) {
		// do not add duplicate adders
		if (!adders.contains(adder))
		    adders.add(adder);
	}
	
	public void removeAdder(User adder) {
		adders.remove(adder);
	}
	
	/**
	 * Is the person "in the group" (which means they can see other members,
	 * posts, etc. if it's a private group)
	 * @return true if the user is invited or active
	 */
	@Transient
	public boolean isParticipant() {
		return getStatus().ordinal() >= MembershipStatus.INVITED.ordinal();
	}
	
	/**
	 * Can the user change the group photo, etc.
	 * @return true if the group member can change stuff
	 */
	@Transient
	public boolean canModify() {
		return getStatus().getCanModify();
	}
	
	/**
	 * Can the user add members to the group.
	 * @return
	 */
	@Transient
	public boolean canAddMembers() {
		return getStatus().ordinal() >= MembershipStatus.REMOVED.ordinal();
	}
	
	@Override
	public String toString() {
		return "{GroupMember status = " + getStatus() + " member = " + getMember() + " group = " + getGroup() + "}";
	}
}
