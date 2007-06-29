package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

public interface ChatMessage {

	@ManyToOne
	@JoinColumn(nullable = false)
	public User getFromUser();

	@Column(nullable = false)
	public String getMessageText();

	@Column(nullable = false)
	public Date getTimestamp();

	@Column(nullable = false)
	public Sentiment getSentiment();
	
	@Transient
	public long getId();
}