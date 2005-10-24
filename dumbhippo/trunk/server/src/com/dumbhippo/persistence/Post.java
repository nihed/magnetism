package com.dumbhippo.persistence;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

@Entity
public class Post extends GuidPersistable {

	private static final long serialVersionUID = 0L;

	private Person poster;
	private String explicitTitle;
	private String text;
	private Date postDate;
	private Set<Person> personRecipients;
	private Set<Group> groupRecipients;
	private Set<Resource> resources;
	
	private void initMissing() {
		if (personRecipients == null)
			personRecipients = new HashSet<Person>();
		if (groupRecipients == null)
			groupRecipients = new HashSet<Group>();
		if (resources == null)
			resources = new HashSet<Resource>();
	}
	
	protected Post() {
		initMissing();
	}
	
	/**
	 * @param poster
	 * @param explicitTitle
	 * @param text
	 * @param personRecipients
	 * @param groupRecipients 
	 * @param resources
	 */
	public Post(Person poster, String explicitTitle, String text, Set<Person> personRecipients, Set<Group> groupRecipients, Set<Resource> resources) {
		this.poster = poster;
		this.explicitTitle = explicitTitle;
		this.text = text;
		if (personRecipients != null)
			this.personRecipients = new HashSet<Person>(personRecipients);
		if (groupRecipients != null)
			this.groupRecipients = new HashSet<Group>(groupRecipients);
		if (resources != null)
			this.resources = new HashSet<Resource>(resources);
		this.postDate = new Date();
		initMissing();
	}
	
	@ManyToOne
	public Person getPoster() {
		return poster;
	}
	public void setPoster(Person poster) {
		this.poster = poster;
	}
	
	@ManyToMany
	public Set<Person> getPersonRecipients() {
		return Collections.unmodifiableSet(personRecipients);
	}
	protected void setPersonRecipients(Set<Person> recipients) {
		if (recipients == null)
			throw new IllegalArgumentException("null");
		this.personRecipients = recipients;
	}
	public void addPersonRecipients(Set<Person> newRecipients) {
		this.personRecipients.addAll(newRecipients);
	}
	
	@ManyToMany
	public Set<Group> getGroupRecipients() {
		return Collections.unmodifiableSet(groupRecipients);
	}
	protected void setGroupRecipients(Set<Group> recipients) {
		if (recipients == null)
			throw new IllegalArgumentException("null");
		this.groupRecipients = recipients;
	}
	public void addGroupRecipients(Set<Group> newRecipients) {
		this.groupRecipients.addAll(newRecipients);
	}
	
	@ManyToMany
	public Set<Resource> getResources() {
		return Collections.unmodifiableSet(resources);
	}
	protected void setResources(Set<Resource> resources) {
		if (resources == null)
			throw new IllegalArgumentException("null");
		this.resources = resources;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getExplicitTitle() {
		return explicitTitle;
	}
	public void setExplicitTitle(String title) {
		this.explicitTitle = title;
	}
	
	@Transient
	public String getTitle() {
		if (explicitTitle != null)
			return explicitTitle;
		else if (resources != null && !resources.isEmpty()) {
			// FIXME look for an url and use its title
		
			return "";
		} else {
			return "";
		}
	}

	public Date getPostDate() {
		return postDate;
	}

	protected void setPostDate(Date postDate) {
		this.postDate = postDate;
	}
}
