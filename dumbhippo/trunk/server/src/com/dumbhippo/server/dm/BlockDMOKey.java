package com.dumbhippo.server.dm;

import com.dumbhippo.dm.BadIdException;
import com.dumbhippo.dm.DMKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockType;

/**
 * Key for BlockDMO and subclasses; our normal convention would be BlockKey, but
 * avoid confusion with com.dumbhippo.persistence.BlockKey.
 * 
 * We include the type so we know what type of DMObject to create from the key.
 */
public class BlockDMOKey implements DMKey {
	private static final long serialVersionUID = -7356973672026993180L;
	
	private Guid blockId;
	private BlockType type;
	
	public BlockDMOKey(String keyString) throws BadIdException {
		if (keyString.length() > 15 && keyString.charAt(14) == '.') {
			try {
				blockId = new Guid(keyString.substring(0, 14));
			} catch (ParseException e) {
				throw new BadIdException("Bad GUID type in external account ID", e);
			}
			
			try {
				type = BlockType.valueOf(keyString.substring(15));
			} catch (IllegalArgumentException e) {
				throw new BadIdException("Bad block type in ID", e);
			}
		} else {
			throw new BadIdException("Bad external account resource ID");
		}
	}
	
	public BlockDMOKey(Block block) {
		blockId = block.getGuid();
		type = block.getBlockType();
	}
	
	public BlockDMOKey(Guid blockId, BlockType type) {
		this.blockId = blockId;
		this.type = type;
	}
	
	public BlockType getType() {
		return type;
	}

	public Guid getBlockId() {
		return blockId;
	}
	
	@Override
	public BlockDMOKey clone() {
		return this;
	}
	
	@Override
	public int hashCode() {
		return blockId.hashCode() + type.ordinal();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BlockDMOKey))
			return false;
		
		BlockDMOKey other = (BlockDMOKey)o;
		
		return type == other.type && blockId.equals(other.blockId);
	}

	@Override
	public String toString() {
		return blockId.toString() + "." + type.name();
	}
}
