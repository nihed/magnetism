package com.dumbhippo.persistence;

/**
 * This enum's integer values are in the database, so don't break them.
 * 
 * @author Havoc Pennington
 *
 */
public enum BlockType {
	POST,
	GROUP_MEMBER,
	GROUP_CHAT,
	MUSIC_PERSON,
	EXTERNAL_ACCOUNT_UPDATE {
	    @Override
	    public boolean isAlwaysPublic() {
	    	return true;
	    }
	    
	    @Override
	    public StackInclusion getDefaultStackInclusion() {
	    	return StackInclusion.ONLY_WHEN_VIEWED_BY_OTHERS;
	    }
	},
	EXTERNAL_ACCOUNT_UPDATE_SELF {
	    @Override
	    public StackInclusion getDefaultStackInclusion() {
	    	return StackInclusion.ONLY_WHEN_VIEWING_SELF;
	    }		
	};
	
	// returns true if all blocks of this type are always public,
	// regardless of their content/specifics
    public boolean isAlwaysPublic() {
    	return false;
    }
    
    // returns null if stack inclusion must be specified 
    // when the Block is constructed, which is typical
    // for any block type that is duplicated with different
    // inclusions
    public StackInclusion getDefaultStackInclusion() {
    	return StackInclusion.IN_ALL_STACKS;
    }
}
