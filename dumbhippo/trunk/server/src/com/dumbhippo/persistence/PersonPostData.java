package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;


@Entity
public class PersonPostData extends DBUnique {
	private static final long serialVersionUID = 1L;
	private Post post;
	private Person person;
	private Date clickedDate;
	
	protected PersonPostData() {}
	
	public PersonPostData(Person person, Post post) {
		this(person, post, new Date());
	}
	
	public PersonPostData(Person person, Post post, Date clickedDate) {
		this.person = person;
		this.post = post;
		this.clickedDate = clickedDate;
	}
	
	public void setClickedDate(Date seenDate) {
		this.clickedDate = seenDate;
	}	

	@Column(nullable=false)
	@ManyToOne
	public Person getPerson() {
		return person;
	}
	
	@Column(nullable=false)
	@ManyToOne
	public Post getPost() {
		return post;
	}
	
	@Column(nullable=true)
	public Date getClickedDate() {
		return clickedDate;
	}
	
	protected void setPerson(Person person) {
		this.person = person;
	}
	protected void setPost(Post post) {
		this.post = post;
	}	
}
