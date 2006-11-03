package com.dumbhippo.persistence;

import com.dumbhippo.identity20.Guid;

/**
 * Not a persistence bean; this is an immutable object representing 
 * the BlockType-specific key to the Block table.
 * 
 */
public class BlockKey {
	
	private BlockType blockType;
	private Guid data1;
	private Guid data2;
	private long data3;
	private StackInclusion inclusion;
	
	public BlockKey(BlockType type, Guid data1, Guid data2, long data3, StackInclusion inclusion) {
		if (type == null)
			throw new IllegalArgumentException("null block type");
		
		this.blockType = type;
		this.data1 = data1;
		this.data2 = data2;
		this.data3 = data3;
		
		if (inclusion != null)
			this.inclusion = inclusion;
		else {
			this.inclusion = type.getDefaultStackInclusion();
			if (this.inclusion == null)
				throw new IllegalArgumentException("inclusion must be specified creating block keys of type " + type);
		}
	}
	
	public BlockKey(BlockType type, Guid data1, Guid data2, long data3) {
		this(type, data1, data2, data3, null);
	}
	
	public BlockKey(BlockType type, Guid data1, Guid data2, StackInclusion inclusion) {
		this(type, data1, data2, -1, inclusion);
	}	
	
	public BlockKey(BlockType type, Guid data1, Guid data2) {
		this(type, data1, data2, -1, null);
	}

	public BlockKey(BlockType type, Guid data1) {
		this(type, data1, null, -1, null);
	}	
	
	public BlockKey(BlockType type, Guid data1, StackInclusion inclusion) {
		this(type, data1, null, -1, inclusion);
	}

	public BlockType getBlockType() {
		return blockType;
	}

	public Guid getData1() {
		return data1;
	}

	public Guid getData2() {
		return data2;
	}

	public long getData3() {
		return data3;
	}

	public StackInclusion getInclusion() {
		return inclusion;
	}

	@Override
	public String toString() {
		return "{type=" + blockType + " data1=" + data1 + " data2=" + data2 + " data3=" + data3 + " inclusion=" + inclusion + "}";
	}
	
	// NO SETTERS this object is immutable
}
