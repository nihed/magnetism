package com.dumbhippo.persistence;

public enum StackReason {
	NEW_BLOCK,     // Stacked because there was a new block (e.g., for posts)
	BLOCK_UPDATE,  // No more specific reason known
	VIEWER_COUNT,  // Stacked because the number of viewers changed (Post) 
	CHAT_MESSAGE   // Stacked because of a new chat message (Post, GroupChat)
}
