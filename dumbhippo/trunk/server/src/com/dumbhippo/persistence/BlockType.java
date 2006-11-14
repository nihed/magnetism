package com.dumbhippo.persistence;

/**
 * This enum's integer values are in the database, so don't break them.
 * 
 * If a BlockType is per-person then it should have _PERSON in the name, 
 * so e.g. a FACEBOOK_PERSON block will be one per person with a Facebook 
 * account, while a FACEBOOK_COMMENT block (if it existed, which it doesn't)
 * would be one per comment or something. 
 * 
 * @author Havoc Pennington
 *
 */
public enum BlockType {
	POST,
	GROUP_MEMBER,
	GROUP_CHAT,
	MUSIC_PERSON,
	/** This is just placeholding a historically-used ordinal that should not be 
	 * reused to avoid database confusion.
	 */
	OBSOLETE_EXTERNAL_ACCOUNT_UPDATE,
	/** This is just placeholding a historically-used ordinal that should not be 
	 * reused to avoid database confusion.
	 */
	OBSOLETE_EXTERNAL_ACCOUNT_UPDATE_SELF,
	
	BLOG_PERSON {
		@Override
		public StackInclusion getDefaultStackInclusion() {
			return null;
		}
	},
	FACEBOOK_PERSON {
		@Override
		public StackInclusion getDefaultStackInclusion() {
			return null;
		}
	},
	FACEBOOK_EVENT {
		@Override
		public StackInclusion getDefaultStackInclusion() {
			return null;
		}		
	},
	FLICKR_PERSON {
		// Right now we only get completely public Flickr photos
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
	},
	FLICKR_PHOTOSET {
		// Right now we only get completely public Flickr photosets
		@Override
		public boolean isAlwaysPublic() {
			return true;
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
