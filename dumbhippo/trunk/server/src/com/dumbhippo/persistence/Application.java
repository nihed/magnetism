package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Application {
	String id;
	String name;
	String description;
	ApplicationCategory category;
	String rawCategories;
	String titlePatterns;
	
	// for hibernate
	public Application() {
	}
	
	public Application(String id) {
		this.id = id;
	}
	
	@Id
	@Column(nullable = false)
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ApplicationCategory getCategory() {
		return category;
	}

	public void setCategory(ApplicationCategory category) {
		this.category = category;
	}

	public String getRawCategories() {
		return rawCategories;
	}

	public void setRawCategories(String rawCategories) {
		this.rawCategories = rawCategories;
	}

	public String getTitlePatterns() {
		return titlePatterns;
	}

	public void setTitlePatterns(String titlePatterns) {
		this.titlePatterns = titlePatterns;
	}
}
