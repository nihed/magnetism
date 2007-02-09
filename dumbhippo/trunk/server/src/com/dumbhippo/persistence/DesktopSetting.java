package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/** 
 * Key-value pairs holding someone's desktop configuration.
 * 
 * @author Havoc Pennington
 */
@Entity
@Table(name="DesktopSetting", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"user_id","key"})}
	      )
public class DesktopSetting extends DBUnique {

	private static final long serialVersionUID = 1L;
	
	private User user;
	private String key;
	private String value;
	
	// used by hibernate
	protected DesktopSetting() {
		
	}
	
	public DesktopSetting(User user, String key, String value) {
		this.user = user;
		this.key = key;
		this.value = value;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@Column(nullable=false)
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	/** 
	 * value can't be null, to remove a setting you delete the row.
	 * @return
	 */
	@Column(nullable=false)
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
