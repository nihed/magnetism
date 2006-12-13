package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.MySpaceBlogComment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.UserViewpoint;

@Local
public interface MySpaceTracker {
	
	public void updateFriendId(User user);
	
	public void setFriendId(User user, String friendId);
	
	public void addMySpaceBlogComment(User user, long commentId, long posterId);
	
	public List<MySpaceBlogComment> getRecentComments(User user);

	public void notifyNewContactComment(UserViewpoint viewpoint, String mySpaceContactName);
}
