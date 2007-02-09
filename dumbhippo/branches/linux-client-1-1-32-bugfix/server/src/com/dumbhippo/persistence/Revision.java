package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 * We keep revision history of certain changes people make to the site. This allows us to display 
 * a history of changes to a person or group, and also to roll back to previous versions. 
 * 
 * The "DBUnique" database ID for the Revision table is essentially a global ever-increasing version number for the entire 
 * web site. Or at at least everything we track revisions for.
 * 
 * For revisions that represent changing a value, such as changing the name of a group, 
 * each revision contains the *new* value. To revert to an old value, you find the most recent non-reverted
 * previous revision to the same item, and set the current value to the old value.
 * You set reverted=true in the revision that was undone.
 * 
 * The reverted flag means the revision was explicitly dropped. This has at least two effects. First, when scanning 
 * back to find a previous revision to revert to, reverted revisions are skipped. Second, the revision would show 
 * as reverted in the user interface.
 * 
 * It's possible to try to revert the earliest not-already-reverted revision. In this case, conceptually we revert to 
 * some default value (like an empty string). This will in particular happen if we don't explicitly create a bunch of 
 * revisions in a migration script.
 * 
 * When reverting, a new Revision should be added, such that the latest revision never has reverted=true.
 * 
 * Some revisions do not represent changing a single value. For example, adding a feed to a group. These revisions may use a different
 * method for reverting. So for example to revert adding a feed, we just remove the feed (which adds a new "feed removed" revision).
 * In this case the Revision table is really justs recording the sequence of changes and who made them.
 * 
 * It isn't clear to me yet what the "reverted" flag means in this case, where there isn't a series of values, but instead a series of 
 * actions that potentially cancel each other out.
 * 
 * 
 * Rows in this table should never be deleted since e.g. Block objects will point to them.
 * 
 * @author Havoc Pennington
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Revision extends DBUnique {

	private RevisionType type;
	private User revisor;
	private long time;
	private boolean reverted;
	
	protected Revision(RevisionType type, User revisor, Date time) {
		this.type = type;
		this.revisor = revisor;
		this.time = time == null ? -1 : time.getTime(); 
	}
	
	protected Revision() {
		this.time = -1;
	}
	
	@JoinColumn(nullable=false)
	@ManyToOne
	public User getRevisor() {
		return revisor;
	}
	
	protected void setRevisor(User revisor) {
		this.revisor = revisor;
	}
	
	@Column(nullable=false)
	public Date getTime() {
		return time >= 0 ? new Date(time) : null;
	}
	protected void setTime(Date time) {
		this.time = time == null ? -1 : time.getTime();
	}
	
	@Transient
	public long getTimeAsLong() {
		return time;
	}
	
	@Column(nullable=false)
	public RevisionType getType() {
		return type;
	}
	protected void setType(RevisionType type) {
		this.type = type;
	}

	@Column(nullable=false)
	public boolean isReverted() {
		return reverted;
	}

	public void setReverted(boolean reverted) {
		this.reverted = reverted;
	}

	@Override
	public String toString() {
		return "{Revision " + getId() + " by " + getRevisor().getId() + " type " + getType().name() + " reverted " + reverted + " at " + getTime() + "}";
	}
}
