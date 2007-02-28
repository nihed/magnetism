package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class ApplicationIcon extends DBUnique {
	private Application application;
	private int size;
	private String iconKey;
	private int actualWidth;
	private int actualHeight;
	
	public ApplicationIcon() {
	}
	
	public ApplicationIcon(Application application, int size, String key, int actualWidth, int actualHeight) {
		this.application = application;
		this.size = size;
		this.iconKey = key;
		this.actualWidth = actualWidth;
		this.actualHeight = actualHeight;
	}
	
	@JoinColumn(nullable = false)
	@ManyToOne
	public Application getApplication() {
		return application;
	}
	
	public void setApplication(Application application) {
		this.application = application;
	}
	
	/**
	 * @return -1 if the icon's size was unspecified, otherwise the nominal size of the
	 *  icon (an icon's actual size may differ slightly from the nominal size, but it
	 *  should look OK displayed with other icons of the same nominal size)
	 */
	@Column(nullable = false)
	public int getSize() {
		return size;
	}
	
	public void setSize(int size) {
		this.size = size;
	}

	@Column(nullable = false)
	public String getIconKey() {
		return iconKey;
	}
	
	public void setIconKey(String key) {
		this.iconKey = key;
	}

	@Column(nullable = false)
	public int getActualWidth() {
		return actualWidth;
	}

	public void setActualWidth(int actualWidth) {
		this.actualWidth = actualWidth;
	}

	@Column(nullable = false)
	public int getActualHeight() {
		return actualHeight;
	}

	public void setActualHeight(int actualHeight) {
		this.actualHeight = actualHeight;
	}
}
