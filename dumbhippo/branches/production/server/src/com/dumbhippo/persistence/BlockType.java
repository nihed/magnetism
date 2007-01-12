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
	POST { // 0
		@Override
		public boolean getClickedCountUseful() { 
			return true;
		}
	},
	GROUP_MEMBER, // 1
	GROUP_CHAT, // 2
	MUSIC_PERSON { // 3
		@Override
		public boolean userOriginIsData1() {
			return true;
		}
	},
	/** This is just placeholding a historically-used ordinal that should not be 
	 * reused to avoid database confusion.
	 */
	OBSOLETE_EXTERNAL_ACCOUNT_UPDATE, // 4
	/** This is just placeholding a historically-used ordinal that should not be 
	 * reused to avoid database confusion.
	 */
	OBSOLETE_EXTERNAL_ACCOUNT_UPDATE_SELF, // 5
	
	/** This is just placeholding a historically-used ordinal that should not be 
	 * reused to avoid database confusion.
	 */
	OBSOLETE_BLOG_PERSON { // 6
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		@Override
		public boolean userOriginIsData1() {
			return true;
		}		
	},
	FACEBOOK_PERSON { // 7
		@Override
		public StackInclusion getDefaultStackInclusion() {
			return null;
		}
		@Override
		public boolean userOriginIsData1() {
			return true;
		}		
	},
	FACEBOOK_EVENT { // 8
		@Override
		public StackInclusion getDefaultStackInclusion() {
			return null;
		}		
	},
	FLICKR_PERSON { // 9
		// Right now we only get completely public Flickr photos
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		@Override
		public boolean userOriginIsData1() {
			return true;
		}		
	},
	FLICKR_PHOTOSET { // 10
		// Right now we only get completely public Flickr photosets
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}		
	},
	YOUTUBE_PERSON { // 11
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		@Override
		public boolean userOriginIsData1() {
			return true;
		}		
	},
	MYSPACE_PERSON { // 12
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		@Override
		public boolean userOriginIsData1() {
			return true;
		}		
	},
	MUSIC_CHAT { // 13
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		@Override
		public boolean userOriginIsData1() {
			return true;
		}		
	},
	BLOG_ENTRY { // 14
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		@Override
		public boolean userOriginIsData1() {
			return true;
		}
	},
	DELICIOUS_PUBLIC_BOOKMARK { // 15
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		@Override
		public boolean userOriginIsData1() {
			return true;
		}
	},
	TWITTER_PERSON { // 16
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		@Override
		public boolean userOriginIsData1() {
			return true;
		}		
	},
	// an item from the Digg "stuff you Dugg" feed
	DIGG_DUGG_ENTRY { // 17
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		@Override
		public boolean userOriginIsData1() {
			return true;
		}		
	},
	// an item from the Reddit Overview feed, which is your comments and submissions both
	REDDIT_ACTIVITY_ENTRY { // 18
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		@Override
		public boolean userOriginIsData1() {
			return true;
		}
	},
	// a revision to a group's attributes
	GROUP_REVISION { // 19
		
				
	};
	
	// True for blocks which are originated by a user and the
	// user guid is stored in the block data1
	public boolean userOriginIsData1() { 
		return false; 
	}
	
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
    
    /** returns true if the block keeps track of clicks */
    public boolean getClickedCountUseful() {
    	return false;
    }
}
