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
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.DELEGATE;
		}
		
		@Override
		public boolean getClickedCountUseful() { 
			return true;
		}
		
		// This is only applicable for actual posts as opposed to feed posts
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA2;
		}
		
		@Override
		public boolean isChatGroupParticipation() {
			return true;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return false;
		}
	}, 
	GROUP_MEMBER { // 1 
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.DELEGATE;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA2;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return false;
		}
	},
	GROUP_CHAT { // 2
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.DELEGATE;
		}
		
		@Override
		public boolean isChatGroupParticipation() {
			return true;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return false;
		}
	},
	MUSIC_PERSON { // 3
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.DELEGATE;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return false;
		}
	},
	/** This is just placeholding a historically-used ordinal that should not be 
	 * reused to avoid database confusion.
	 */
	OBSOLETE_EXTERNAL_ACCOUNT_UPDATE { // 4
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.NOBODY;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return false;
		}
	},
	/** This is just placeholding a historically-used ordinal that should not be 
	 * reused to avoid database confusion.
	 */
	OBSOLETE_EXTERNAL_ACCOUNT_UPDATE_SELF { // 5
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.NOBODY;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return false;
		}
	},
	/** This is just placeholding a historically-used ordinal that should not be 
	 * reused to avoid database confusion.
	 */
	OBSOLETE_BLOG_PERSON { // 6
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.NOBODY;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return false;
		}
	},
	FACEBOOK_PERSON { // 7
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.NOBODY;
		}
		
		@Override
		public StackInclusion getDefaultStackInclusion() {
			return null;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA1;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return false;
		}
	},
	FACEBOOK_EVENT { // 8
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.OWNER;
		}
		
		@Override
		public StackInclusion getDefaultStackInclusion() {
			return null;
		}	
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA1;
		}			
		
		@Override
		public boolean isDirectlyChattable() {
			return false; // not completely clear ... it would make sense for some subtypes and not others
		}
	},
	FLICKR_PERSON { // 9
		// Right now we only get completely public Flickr photos
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}		
		
		@Override
		public boolean isDirectlyChattable() {
			return true; // per-photo chats might make more sense long-term
		}
	},
	FLICKR_PHOTOSET { // 10
		// Right now we only get completely public Flickr photosets
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}	
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return true;
		}
	},
	YOUTUBE_PERSON { // 11
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}		
		
		@Override
		public boolean isDirectlyChattable() {
			return true;  // per-video chats might make more sense long-term
		}
	},
	MYSPACE_PERSON { // 12
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}		
		
		@Override
		public boolean isDirectlyChattable() {
			return false; // wait until we have block-per-entry
		}
	},
	MUSIC_CHAT { // 13
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA1;
		}				
		
		@Override
		public boolean isDirectlyChattable() {
			return false;
		}
	},
	BLOG_ENTRY { // 14
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return true;
		}
	},
	DELICIOUS_PUBLIC_BOOKMARK { // 15
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}	
		
		@Override
		public boolean isDirectlyChattable() {
			return true;
		}
	},
	TWITTER_PERSON { // 16
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}		
		
		@Override
		public boolean isDirectlyChattable() {
			return false;
		}
	},
	// an item from the Digg "stuff you Dugg" feed
	DIGG_DUGG_ENTRY { // 17
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return true;
		}
	},
	// an item from the Reddit Overview feed, which is your comments and submissions both
	REDDIT_ACTIVITY_ENTRY { // 18
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return true;
		}
	},
	// a revision to a group's attributes
	GROUP_REVISION { // 19		
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.DELEGATE;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return true;
		}
	},
	// a Netflix movie
	NETFLIX_MOVIE { // 20
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		// We show this one even though the user did originate
		// the block by enqueuing the movie, because it also serves as a
		// notification of movies being shipped.
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA1;
		}		
		
		@Override
		public boolean isDirectlyChattable() {
			return true;
		}
	},
	// a question to the user about account options
	ACCOUNT_QUESTION { // 21
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.OWNER;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.INDIRECT_DATA1;
		}	
		
		@Override
		public StackInclusion getDefaultStackInclusion() {
			return StackInclusion.ONLY_WHEN_VIEWING_SELF;
		}		
		
		@Override
		public boolean isDirectlyChattable() {
			return false;
		}
	},
	GOOGLE_READER_SHARED_ITEM { // 22
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}
		
		@Override
		public boolean isDirectlyChattable() {
			return true;
		}
	},
	PICASA_PERSON { // 23
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}	
		
		@Override
		public boolean isDirectlyChattable() {
			return true; // might want per-album chats eventually
		}
	},
	AMAZON_REVIEW { // 24
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}	
		
		@Override
		public boolean isDirectlyChattable() {
			return true;
		}
	}, 
	AMAZON_WISH_LIST_ITEM { // 25
		@Override
		public BlockVisibility getBlockVisibility() {
			return BlockVisibility.PUBLIC;
		}
		
		@Override
		public BlockOwnership getBlockOwnership() {
			return BlockOwnership.DIRECT_DATA1;
		}	
		
		@Override
		public boolean isDirectlyChattable() {
			return true;
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
	
	// Who can see the block
	public enum BlockVisibility {
		PUBLIC,   // Everybody
		OWNER,    // Just the owner
		DELEGATE, // The block can be seen by people who can see some other object
		          // (See BlockDMO.getVisibilityDelegate())
		NOBODY,   // Obsolete block types, safety fallback
	}
	
	public BlockOwnership getBlockOwnership() { 
		return BlockOwnership.NONE;
	}
	
	abstract public BlockVisibility getBlockVisibility();
	
    // returns null if stack inclusion must be specified 
    // when the Block is constructed, which is typical
    // for any block type that is duplicated with different
    // inclusions
    public StackInclusion getDefaultStackInclusion() {
    	return StackInclusion.IN_ALL_STACKS;
    }
    
    // return true if we chat on this block type by creating
    // BlockType objects. false can mean either that there is
    // a different way to chat on the block (GroupMessage, etc)
    // or that it isn't interesting to chat on it at all
    abstract public boolean isDirectlyChattable();
    
    // return if chatting on a block of this type should count as
    // participation in the group
    public boolean isChatGroupParticipation() {
    	return false;
    }
    
    /** returns true if the block keeps track of clicks */
    public boolean getClickedCountUseful() {
    	return false;
    }
}
