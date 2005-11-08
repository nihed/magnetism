package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;


@Entity
public class PersonPostData extends DBUnique {
	private static final long serialVersionUID = 1L;
	private Post post;
	private Person person;
	// store date in this form since it's immutable and lightweight; -1 = null
	private long clickedDate;
	
	protected PersonPostData() {	
	}
	
	private PersonPostData(Person person, Post post, long clickedDate) {
		this.person = person;
		this.post = post;
		this.clickedDate = clickedDate;		
	}
	
	public PersonPostData(Person person, Post post) {
		this(person, post, System.currentTimeMillis());
	}
	
	public PersonPostData(Person person, Post post, Date clickedDate) {
		this(person, post, clickedDate.getTime());
	}
	
	public void setClickedDate(Date seenDate) {
		this.clickedDate = seenDate != null ? seenDate.getTime() : -1;
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
	
	protected void setPerson(Person person) {
		this.person = person;
	}
	protected void setPost(Post post) {
		this.post = post;
	}	
}
