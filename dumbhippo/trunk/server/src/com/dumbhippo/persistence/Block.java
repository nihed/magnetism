package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;

/**
 * A Block is a block to be displayed in the Stacker, can also be thought of 
 * as an event in an event log. A Block may be shared among multiple users; the
 * PersonBlockData object tracks per-user information.
 * 
 * Each Block has a timestamp of last activity, and blocks are ordered by 
 * this timestamp.
 * 
 * To avoid a bunch of block subclasses, it stores a guid and a long, which generally 
 * point to database ids in other tables, depending on the type of block.
 * 
 * @author Havoc Pennington
 */
@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
@Table(name="Block", 
	   uniqueConstraints = {
			@UniqueConstraint(columnNames={"blockType", "data1", "data2", "data3"})
		})
/*
 This doesn't work because hibernate sets the index length for data1 to 20 but the column 
 only has length 14, which makes mysql barf.
 
 @org.hibernate.annotations.Table(name="Block",
		// this indexing could be overzealous
		indexes = {
			@Index(name="timestamp_index", columnNames = { "timestamp" }),
			@Index(name="data1_index", columnNames = { "data1" }),
			@Index(name="data2_index", columnNames = { "data2" }),
			@Index(name="data3_index", columnNames = { "data3" })
		})
		*/
public class Block extends EmbeddedGuidPersistable {

	private BlockType blockType;
	private long timestamp;
	private Guid data1;
	private Guid data2;
	private long data3;
	private int clickedCount;
	private boolean publicBlock;
	
	// for hibernate
	public Block() {
		this.timestamp = 0;
		this.data3 = -1;
		this.publicBlock = false;
	}
	
	public Block(BlockType type, Guid data1, Guid data2, long data3) {
		this();
		this.blockType = type;
		this.data1 = data1;
		this.data2 = data2;
		this.data3 = data3;
	}

	public Block(BlockType type, Guid data1, Guid data2, long data3, boolean publicBlock) {
		this(type, data1, data2, data3);
        this.publicBlock = publicBlock;
	}
	
	public Block(BlockType type, Guid data1, Guid data2) {
		this(type, data1, data2, -1);
	}

	public Block(BlockType type, Guid data1, Guid data2, boolean publicBlock) {
		this(type, data1, data2, -1, publicBlock);
	}
	
	@Column(nullable=false)
	public BlockType getBlockType() {
		return blockType;
	}
	
	public void setBlockType(BlockType type) {
		this.blockType = type;
	}
	
	@Column(nullable=false)
	public Date getTimestamp() {
		return new Date(timestamp);
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp.getTime();
	}	
	
	@Transient
	public long getTimestampAsLong() {
		return this.timestamp;
	}
	
	public void setTimestampAsLong(long timestamp) {
		this.timestamp = timestamp;
	}
	
	@Transient
	public Guid getData1AsGuid() {
		return data1;
	}	

	public void setData1AsGuid(Guid data1) {
		// no copy since Guid is immutable
		this.data1 = data1;
	}

	// this must not be nullable or the unique constraint breaks;
	// protected to be sure we don't leak the weird empty string 
	// magic to mean null
	@Column(length = Guid.STRING_LENGTH, nullable = false)
	protected String getData1() {
		Guid g = getData1AsGuid();
		if (g == null)
			return "";
		String s = g.toString();
		assert s.length() == Guid.STRING_LENGTH;
		return s;
	}

	public void setData1(String data1) throws ParseException {
		setData1AsGuid(data1.length() > 0 ? new Guid(data1) : null);
	}
	
	@Transient
	public Guid getData2AsGuid() {
		return data2;
	}	

	public void setData2AsGuid(Guid data2) {
		// no copy since Guid is immutable
		this.data2 = data2;
	}

	// this must not be nullable or the unique constraint breaks;
	// protected to be sure we don't leak the weird empty string 
	// magic to mean null	
	@Column(length = Guid.STRING_LENGTH, nullable = false)
	protected String getData2() {
		Guid g = getData2AsGuid();
		if (g == null)
			return "";
		String s = g.toString();
		assert s.length() == Guid.STRING_LENGTH;
		return s;
	}

	protected void setData2(String data2) throws ParseException {
		setData2AsGuid(data2.length() > 0 ? new Guid(data2) : null);
	}
	
	@Column(nullable = false)
	public long getData3() {
		return data3;
	}

	public void setData3(long data3) {
		this.data3 = data3;
	}
	
	// this is a denormalized field; it should be equal to the number of 
	// UserBlockData for this block with a non-null clicked time
	@Column(nullable=false)
	public int getClickedCount() {
		return clickedCount;
	}
	
	public void setClickedCount(int clickedCount) {
		this.clickedCount = clickedCount;
	}
	
	@Column(nullable=false)
	public boolean isPublicBlock() {
		return publicBlock;
	}	

	public void setPublicBlock(boolean publicBlock) {
		this.publicBlock = publicBlock;
	}
	
	@Override
	public String toString() {
		return "{Block type=" + getBlockType() + " timestamp=" + getTimestampAsLong() + " data1=" + getData1() + " data2=" + getData2() + "}";
	}
}
