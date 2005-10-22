package com.dumbhippo.persistence;

import java.util.Date;
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
	private Set<Person> recipients;
	private Set<Resource> resources;

	protected Post() {}
	
	/**
	 * @param poster
	 * @param explicitTitle
	 * @param text
	 * @param recipients
	 * @param resources
	 */
	public Post(Person poster, String explicitTitle, String text, Set<Person> recipients, Set<Resource> resources) {
		this.poster = poster;
		this.explicitTitle = explicitTitle;
		this.text = text;
		this.recipients = recipients;
		this.resources = resources;
		this.postDate = new Date();
	}
	
	@ManyToOne
	public Person getPoster() {
		return poster;
	}
	public void setPoster(Person poster) {
		this.poster = poster;
	}
	
	@ManyToMany
	public Set<Person> getRecipients() {
		return recipients;
	}
	protected void setRecipients(Set<Person> recipients) {
		this.recipients = recipients;
	}
	public void addRecipients(Set<Person> newRecipients) {
		this.recipients.addAll(newRecipients);
	}
	
	@ManyToMany
	public Set<Resource> getResources() {
		return resources;
	}
	protected void setResources(Set<Resource> resources) {
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

	protected Date getPostDate() {
		return postDate;
	}

	protected void setPostDate(Date postDate) {
		this.postDate = postDate;
	}

}
