package com.dumbhippo.persistence;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
	private Set<Person> expandedRecipients;
	
	private void initMissing() {
		if (personRecipients == null)
			personRecipients = new HashSet<Person>();
		if (groupRecipients == null)
			groupRecipients = new HashSet<Group>();
		if (resources == null)
			resources = new HashSet<Resource>();
		if (expandedRecipients == null)
			expandedRecipients = new HashSet<Person>();
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
	 * @param expandedRecipients 
	 * @param resources
	 */
	public Post(Person poster, String explicitTitle, String text, Set<Person> personRecipients, Set<Group> groupRecipients, Set<Person> expandedRecipients, Set<Resource> resources) {
		this.poster = poster;
		this.explicitTitle = explicitTitle;
		this.text = text;
		this.personRecipients = personRecipients;
		this.groupRecipients = groupRecipients;
		this.expandedRecipients = expandedRecipients;
		this.resources = resources;
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
		return personRecipients;
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
		return groupRecipients;
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
		return resources;
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

	@ManyToMany
	@JoinTable(table=@Table(name="Post_Person_Expanded")) // otherwise conflicts with getRecipients
	public Set<Person> getExpandedRecipients() {
		return expandedRecipients;
	}

	public void setExpandedRecipients(Set<Person> expandedRecipients) {
		if (expandedRecipients == null)
			throw new IllegalArgumentException("null");
		this.expandedRecipients = expandedRecipients;
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
