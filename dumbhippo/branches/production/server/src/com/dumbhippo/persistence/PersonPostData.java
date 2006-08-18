package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;


@Entity
@Table(name="PersonPostData", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"post_id", "person_id"})}
	      )
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)	      
public class PersonPostData extends DBUnique {
	private static final long serialVersionUID = 1L;
	private Post post;
	private Person person;
	// store date in this form since it's immutable and lightweight; -1 = null
	private long clickedDate;
	private boolean ignored;
	
	protected PersonPostData() {	
	}
	
	public PersonPostData(Person person, Post post) {
		this.person = person;
		this.post = post;
		this.clickedDate = -1;
		this.ignored = false;
	}
	
	public void setClicked() {
		this.clickedDate = System.currentTimeMillis();
	}
	
	public void setClickedDate(Date seenDate) {
		this.clickedDate = seenDate != null ? seenDate.getTime() : -1;
	}	
	
	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}

	@JoinColumn(nullable=false)
	@ManyToOne
	public Person getPerson() {
		return person;
	}
	
	@JoinColumn(nullable=false)
	@ManyToOne
	public Post getPost() {
		return post;
	}
	
	@Column(nullable=true)
	public Date getClickedDate() {
		return clickedDate >= 0 ? new Date(clickedDate) : null;
	}
	
	@Transient
	public long getClickedDateAsLong() {
		return clickedDate;
	}
	
	@Column(nullable=false)
	public boolean isIgnored() {
		return ignored;
	}	
	
	protected void setPerson(Person person) {
		this.person = person;
	}
	protected void setPost(Post post) {
		this.post = post;
	}	
}
