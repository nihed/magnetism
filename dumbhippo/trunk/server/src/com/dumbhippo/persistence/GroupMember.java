package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

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
public class GroupMember extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private Group group;
	private Resource member;
	private MembershipStatus status;
	private User adder;
	
	protected GroupMember() {}
	
	public GroupMember(Group group, Resource member, MembershipStatus status) {
		this.group = group;
		this.member = member;
		this.status = status;
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
	
	@ManyToOne
	public User getAdder() {
		return adder;
	}
	
	public void setAdder(User adder) {
		this.adder = adder;
	}
	
	/**
	 * Is the person "in the group" (which means they can see other members,
	 * posts, etc.)
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
		return getStatus().ordinal() >= MembershipStatus.ACTIVE.ordinal();
	}
	
	@Override
	public String toString() {
		return "{GroupMember status = " + getStatus() + " member = " + getMember() + " group = " + getGroup() + "}";
	}
}
