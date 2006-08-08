package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;

/** 
 * The stacker manages the stack of Block objects and the UserBlockData associated with them.
 * 
 * @author Havoc Pennington
 */
@Local
public interface Stacker {
	public void stackMusicPerson(Guid userId, long activity);
	public void stackGroupChat(Guid groupId, long activity);
	public void stackPost(Guid postId, long activity);
	
	public List<UserBlockData> getStack(Viewpoint viewpoint, User user, long lastTimestamp, int start, int count);
}
