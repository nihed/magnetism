package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
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
	private Person member;
	private MembershipStatus status;
	private Person adder;
	
	protected GroupMember() {}
	
	public GroupMember(Group g, Person m, MembershipStatus s) {
		group = g;
		member = m;
		status = s;
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
	public Person getMember() {
		return member;
	}
	
	public void setMember(Person m) {
		member = m;
	}
	
	@Column(nullable=false)
	public MembershipStatus getStatus() {
		return status;
	}
	
	public void setStatus(MembershipStatus s) {
		status = s;
	}
	
	@ManyToOne
	public Person getAdder() {
		return adder;
	}
	
	public void setAdder(Person p) {
		adder = p;
	}
}
