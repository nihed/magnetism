package com.dumbhippo.persistence;

public enum FacebookEventType {
    UNREAD_MESSAGES_UPDATE  {
		@Override
		public boolean getDisplayToOthers() {
			return false;
		}
    },
    NEW_WALL_MESSAGES_EVENT,
    UNSEEN_POKES_UPDATE {
		@Override
		public boolean getDisplayToOthers() {
			return false;
		}
    },
    NEW_TAGGED_PHOTOS_EVENT,
    NEW_ALBUM_EVENT,
    MODIFIED_ALBUM_EVENT;
    
    public boolean getDisplayToSelf() {
    	return true;
    }
    
    public boolean getDisplayToOthers() {
    	return true;
    }
    
    public boolean shouldDisplay(boolean isSelf) {
    	if ((isSelf && getDisplayToSelf()) || (!isSelf && getDisplayToOthers())) {
    		return true;
    	}   	
    	return false;
    }
}
