package com.dumbhippo.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.dumbhippo.server.util.EJBUtil;

@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class Application {
	private String id;
	private String name;
	private String description;
	private ApplicationCategory category;
	private String rawCategories;
	private String titlePatterns;
	private String desktopNames;
	private Set<ApplicationIcon> icons;
	
	// for hibernate
	public Application() {
	}
	
	public Application(String id) {
		this.id = id;
	}
	
	@Id
	@Column(nullable = false, length=64)
	public String getId() {
		return id;
	}
	
	protected void setId(String id) {
		this.id = id;
	}

	@Column(nullable = false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(nullable = true)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column(nullable = false)
	public ApplicationCategory getCategory() {
		return category;
	}

	public void setCategory(ApplicationCategory category) {
		this.category = category;
	}

	@Column(nullable = true)
	public String getRawCategories() {
		return rawCategories;
	}

	public void setRawCategories(String rawCategories) {
		this.rawCategories = rawCategories;
	}

	@Column(nullable = true)
	public String getTitlePatterns() {
		return titlePatterns;
	}

	public void setTitlePatterns(String titlePatterns) {
		this.titlePatterns = titlePatterns;
	}
	
	@Column(nullable = true)
	public String getDesktopNames() {
		return desktopNames;
	}

	public void setDesktopNames(String desktopNames) {
		this.desktopNames = desktopNames;
	}
	
	@OneToMany(mappedBy="application")
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	public Set<ApplicationIcon> getIcons() {
		if (icons == null)
			icons = new HashSet<ApplicationIcon>();
		
		return icons;
	}
	
	protected void setIcons(Set<ApplicationIcon> icons) {
		if (icons == null)
			throw new NullPointerException();
		this.icons = icons;
	}
	
	/**
	 * Bug work around, see docs for EJBUtil.forceInitialization()
	 */
	public void prepareToModifyIcons() {
		EJBUtil.forceInitialization(icons);
	}
}
