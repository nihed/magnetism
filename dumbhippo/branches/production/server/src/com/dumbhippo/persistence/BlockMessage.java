package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Stores an individual message associated with a block.
 */
@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class BlockMessage extends EmbeddedMessage {

	private static final long serialVersionUID = 1L;
	
	private Block block;
	
	public BlockMessage() {
		this(null, null, null, Sentiment.INDIFFERENT, null);
	}

	public BlockMessage(Block block, User fromUser, String messageText, Sentiment sentiment, Date timestamp) {
		super(fromUser, messageText, timestamp, sentiment);
		this.block = block;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}
}
