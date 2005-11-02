package com.dumbhippo.persistence;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.LobType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.jboss.annotation.ejb.Clustered;

import com.dumbhippo.XmlBuilder;

@Entity
public class Post extends GuidPersistable {

	private static final long serialVersionUID = 0L;

	private Person poster;
	private PostVisibility visibility;
	private String explicitTitle;
	private Date postDate;
	private String text;
	private Set<Person> personRecipients;
	private Set<Group> groupRecipients;
	private Set<Resource> resources;
	private Set<Person> expandedRecipients;
	
	private void initMissing() {
		if (visibility == null)
			visibility = PostVisibility.ANONYMOUSLY_PUBLIC;
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
	 * @param visibility 
	 * @param explicitTitle
	 * @param text
	 * @param personRecipients
	 * @param groupRecipients
	 * @param expandedRecipients
	 * @param resources
	 */
	public Post(Person poster, PostVisibility visibility, String explicitTitle, String text, Set<Person> personRecipients,
			Set<Group> groupRecipients, Set<Person> expandedRecipients, Set<Resource> resources) {
		this.poster = poster;
		this.visibility = visibility;
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
	@Column(nullable=false)
	public Person getPoster() {
		return poster;
	}
	public void setPoster(Person poster) {
		this.poster = poster;
	}
	
	@Column(nullable=false)
	public PostVisibility getVisibility() {
		return visibility;
	}

	public void setVisibility(PostVisibility visibility) {
		this.visibility = visibility;
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
	
	// This results in the "text" type on mysql. 
	// The Postgresql docs I've read basically say 
	// "varchar's fixed lengthness is stupid, text is always
	// a better choice unless you have a real reason for a fixed length"
	// on postgresql "text" is the same as "varchar" but doesn't run 
	// the code to truncate to the fixed length.
	// But some mysql stuff says mysql text type is screwy and it 
	// involves some kind of out-of-band lookaside table.
	// 
	// Anyhow, Hibernate seems to be broken, it doesn't 
	// properly convert the clob to a string, so we can't 
	// use this anyway. 255 char limit it is.
	//@Lob(type=LobType.CLOB, fetch=FetchType.EAGER)
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

	@Transient
	public String getTextAsHtml() {
		XmlBuilder builder = new XmlBuilder();
		builder.appendTextAsHtml(getText());
		return builder.toString();
	}
	
	@Column(nullable=false)
	public Date getPostDate() {
		return postDate;
	}

	protected void setPostDate(Date postDate) {
		this.postDate = postDate;
	}
	
	public String toString() {
		return "{Post id=" + getId() + "}";
	}
}
