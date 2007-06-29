package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Rating represents a preference expressed by an Account
 * for a particular Resource.  It's main use is as input to the
 * recommender engine.
 * 
 * @author dff
 */
@Entity
@Table(name="Rating", 
		   uniqueConstraints = 
			      {@UniqueConstraint(columnNames={"user_id", "item_id"})}
		   )
public class Rating extends GuidPersistable {
	private static final long serialVersionUID = 1L;
	
	// TODO: I think there's already an enum like this already extant somewhere else in the codebase...
	public static String POST_TYPE = "Post";
	public static String TRACK_TYPE = "Track";
	
	/**
	 * ID of the user that is expressing the preference
	 */
	private User user;
	
	/**
	 * ID of the item that a preference is being expressed about.
	 */
	private Post item;
	
	/**
	 * The preference score, in the range from 0.0 to 1.0
	 */
	private double score;
	
	/**
	 * The reason a rating was expressed (e.g. implicit from click-through, 
	 * explicit from user input.
	 */
	private String reason;
	
	/**
	 * The type of resource that was rated, for easy filtering.
	 */
	private String type;
	
	protected Rating() {}
	
	public Rating(User user, Post item, double score, String reason, String type) {
		this.user = user;
		this.item = item;
		this.score = score;
		this.reason = reason;
		this.type = type;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public User getUser() {
		return user;
	}
	
	protected void setUser(User user) {
		this.user = user;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Post getItem() {
		return item;
	}
	
	public void setItem(Post item) {
		this.item = item;
	}
	
	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	@Override
	public String toString() {
		return "{Rating account = " + getUser() + " item = " + getItem() + " score = " + getScore() + " reason = " + getReason() + " type = " + getType() + "}";
	}
}
