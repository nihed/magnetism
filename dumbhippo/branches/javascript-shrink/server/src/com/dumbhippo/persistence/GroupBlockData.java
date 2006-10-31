package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
@Table(name="GroupBlockData", 
	   uniqueConstraints = {
			@UniqueConstraint(columnNames={"group_id", "block_id"})
		})
public class GroupBlockData extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private Group group;
	private Block block;
	private boolean deleted;
	
	public GroupBlockData() {
		this.deleted = false;
	}
	
	public GroupBlockData(Group group, Block block) {
		this();
		this.block = block;
		this.group = group; 
	}
	
	@JoinColumn(nullable=false)
	@ManyToOne
	public Group getGroup() {
		return group;
	}

	protected void setGroup(Group group) {
		this.group = group;
	}
	
	@JoinColumn(nullable=false)
	@ManyToOne
	public Block getBlock() {
		return block;
	}

	protected void setBlock(Block block) {
		this.block = block;
	}	
	
	@Column(nullable=false)
	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}	
	
	@Override
	public String toString() {
		return "{GroupBlockData user=" + group + " block=" + block + "}";
	}
	
	// hashCode and equals for this class are intended to ensure uniqueness 
	// by user-block pair when in a set
	@Override
	public int hashCode() {
		int h = 33;
		if (group != null)
			h += group.hashCode();
		if (block != null)
			h += block.hashCode();
		return h;
	}
	
	@Override 
	public boolean equals(Object other) {
		if (!(other instanceof GroupBlockData))
			return false;
		GroupBlockData otherData = (GroupBlockData) other;

		// most common case first
		if (this.group != null && this.block != null) {
			if (otherData.group != null && otherData.block != null)
				return otherData.group.equals(this.group) && otherData.block.equals(this.block);
			else
				return false;
		} else if (this.group == null) {
			return otherData.group == null && otherData.block != null && otherData.block.equals(this.block);
		} else if (this.block == null) {
			return otherData.block == null && otherData.group != null && otherData.group.equals(this.group);
		} else {
			assert this.group == null;
			assert this.block == null;
			return otherData.block == null && otherData.group == null;
		}
	}
}
