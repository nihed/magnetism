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
	},
	EXTERNAL_ACCOUNT_UPDATE_SELF;
	
    public boolean isAlwaysPublic() {
    	return false;
    }
}
