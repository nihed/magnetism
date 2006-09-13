package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;

/** 
 * The stacker manages the stack of Block objects and the UserBlockData associated with them.
 * 
 * @author Havoc Pennington
 */
@Local
public interface Stacker {
	public void onUserCreated();
	public void onGroupCreated();
	public void onPostCreated();
	
	public void stackMusicPerson(Guid userId, long activity);
	public void stackGroupChat(Guid groupId, long activity);
	public void stackPost(Guid postId, long activity);
	public void stackGroupMember(GroupMember member, long activity);
	public void stackAccountUpdate(Guid userId, ExternalAccountType type, long activity);
	
	public void clickedPost(Post post, User user, long clickedTime);
	
	public List<UserBlockData> getStack(Viewpoint viewpoint, User user, long lastTimestamp, int start, int count);

	public UserBlockData lookupUserBlockData(UserViewpoint viewpoint, Guid guid) throws NotFoundException;
	
	public void migrateEverything();
	public void migrateGroups();
	public void migratePost(String postId);
	public void migrateUser(String userId);
	public void migrateGroupChat(String groupId);
	public void migrateGroupMembers(String groupId);
}
