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
    NEW_TAGGED_PHOTOS_EVENT {
	    @Override
	    public String getPageName() {
		    return "photo_search";
	    }
    },
    NEW_ALBUM_EVENT {
	    @Override
	    public String getPageName() {
		    return "photos";
	    }
    },
    MODIFIED_ALBUM_EVENT {
	    @Override
	    public String getPageName() {
		    return "photos";
	    }
    },
    LOGIN_STATUS_EVENT {
		@Override
		public boolean getDisplayToOthers() {
			return false;
		}
    };
    
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
    
    public String getPageName() {
    	return "profile";
    }
}
