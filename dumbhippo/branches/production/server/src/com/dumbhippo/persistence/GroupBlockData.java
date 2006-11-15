package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
@Table(name="GroupBlockData", 
	   uniqueConstraints = {
			@UniqueConstraint(columnNames={"group_id", "block_id"})
		})
@org.hibernate.annotations.Table(appliesTo = "GroupBlockData", indexes={ 
		@Index(name="participatedTimestamp_index", columnNames = { "participatedTimestamp" } ),
		@Index(name="groupStack_index", columnNames = { "group_id", "stackTimestamp" } ), 
		@Index(name="groupParticipated_index", columnNames = { "group_id", "participatedTimestamp" } ) 
})		
public class GroupBlockData extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private Group group;
	private Block block;
	private boolean deleted;
	private long participatedTimestamp;
	private long stackTimestamp;
	
	public GroupBlockData() {
		this.deleted = false;
	}
	
	public GroupBlockData(Group group, Block block) {
		this();
		this.block = block;
		this.group = group; 
		this.participatedTimestamp = -1;
		this.stackTimestamp = block.getTimestampAsLong();
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
	
	@Column(nullable=true)
	public Date getParticipatedTimestamp() {
		return participatedTimestamp >= 0 ? new Date(participatedTimestamp) : null;
	}

	@Transient
	public long getParticipatedTimestampAsLong() {
		return participatedTimestamp;
	}
	
	public void setParticipatedTimestamp(Date participatedTimestamp) {
		this.participatedTimestamp = participatedTimestamp != null ? participatedTimestamp.getTime() : -1;
	}
	
	public void setParticipatedTimestampAsLong(long participatedTimestamp) {
		this.participatedTimestamp = participatedTimestamp;
	}
	
	@Transient
	public boolean getParticipated() {
		return this.participatedTimestamp >= 0;
	}
	
	@Column(nullable=false)
	public Date getStackTimestamp() {
		return new Date(stackTimestamp);
	}

	@Transient
	public long getStackTimestampAsLong() {
		return stackTimestamp;
	}
	
	public void setStackTimestamp(Date stackTimestamp) {
		this.stackTimestamp = stackTimestamp.getTime();
	}
	
	public void setStackTimestampAsLong(long stackTimestamp) {
		this.stackTimestamp = stackTimestamp;
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
