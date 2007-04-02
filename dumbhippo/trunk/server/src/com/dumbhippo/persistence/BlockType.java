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
		
		// This is only applicable for actual posts as opposed to feed posts
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA2;
		}
	}, 
	GROUP_MEMBER { // 1 
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA2;
		}
	},
	GROUP_CHAT, // 2
	MUSIC_PERSON { // 3
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
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
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}		
	},
	FACEBOOK_PERSON { // 7
		@Override
		public StackInclusion getDefaultStackInclusion() {
			return null;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA1;
		}	
	},
	FACEBOOK_EVENT { // 8
		@Override
		public StackInclusion getDefaultStackInclusion() {
			return null;
		}	
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA1;
		}			
	},
	FLICKR_PERSON { // 9
		// Right now we only get completely public Flickr photos
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}		
	},
	FLICKR_PHOTOSET { // 10
		// Right now we only get completely public Flickr photosets
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}	
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}			
	},
	YOUTUBE_PERSON { // 11
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}		
	},
	MYSPACE_PERSON { // 12
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}		
	},
	MUSIC_CHAT { // 13
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA1;
		}				
	},
	BLOG_ENTRY { // 14
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}	
	},
	DELICIOUS_PUBLIC_BOOKMARK { // 15
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}	
	},
	TWITTER_PERSON { // 16
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}		
	},
	// an item from the Digg "stuff you Dugg" feed
	DIGG_DUGG_ENTRY { // 17
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}		
	},
	// an item from the Reddit Overview feed, which is your comments and submissions both
	REDDIT_ACTIVITY_ENTRY { // 18
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}	
	},
	// a revision to a group's attributes
	GROUP_REVISION { // 19		
	},
	// a Netflix movie
	NETFLIX_MOVIE { // 20
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		
		// We show this one even though the user did originate
		// the block by enqueuing the movie, because it also serves as a
		// notification of movies being shipped.
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA1;
		}				
	},
	// a question to the user about account options
	ACCOUNT_QUESTION { // 21
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA1;
		}	
		
		@Override
		public StackInclusion getDefaultStackInclusion() {
			return StackInclusion.ONLY_WHEN_VIEWING_SELF;
		}		
	},
	GOOGLE_READER_SHARED_ITEM { // 22
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}
	},
	PICASA_PERSON { // 23
		@Override
		public boolean isAlwaysPublic() {
			return true;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}	
	};
	
	// This enumeration specifies a number of qualities.  First, whether
	// the block was originated by a user at all.  Second, whether the user
	// directly originated the block.  For most external accounts this will
	// be DIRECT; the block is reflecting something the user created elsewhere.
	// For other types of blocks, the block is really originated by the server
	// such as chat blocks, facebook notifications, and account questions.
	// This distinction is currently used for the client filter "Hide my items".
	// These two qualities (DIRECT/INDIRECT) are further subdivided based on
	// whether the user guid is stored in the data1 or data2 field of the 
	// Block.  
	public enum BlockOwnership {
		NONE,
		INDIRECT_DATA1,
		INDIRECT_DATA2,
		DIRECT_DATA1,
		DIRECT_DATA2
	}
	
	public BlockOwnership getBlockOwnership() { 
		return BlockOwnership.NONE;
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
