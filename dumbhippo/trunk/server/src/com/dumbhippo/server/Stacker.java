package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.BlockView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

/** 
 * The stacker manages the stack of Block objects and the UserBlockData associated with them.
 * 
 * @author Havoc Pennington
 */
@Local
public interface Stacker {
	// FIXME: All of the on<Entity>Created stack<BlockType> methods below could be usefully
	// changed to take attached objects rather than object identifiers. They are meant
	// to be called within the transaction where the object is created or modified.
	
	// These methods are used for creating blocks corresponding to an entity when the entity
	// is created. They are per entity type, not per-block type, though the two currently
	// correspond 1:1.
	public void onUserCreated(Guid userId);
	public void onGroupCreated(Guid groupId);
	public void onPostCreated(Guid postId);
	public void onGroupMemberCreated(GroupMember member);
	public void onExternalAccountCreated(Guid userId, ExternalAccountType type);
	
	// These methods are used when activity should cause the timestamp of a block to change. 
	// They are per block type. 
	public void stackMusicPerson(Guid userId, long activity);
	public void stackGroupChat(Guid groupId, long activity);
	public void stackPost(Guid postId, long activity);
	public void stackGroupMember(GroupMember member, long activity);
	public void stackAccountUpdate(Guid userId, ExternalAccountType type, long activity);
	public void stackAccountUpdateSelf(Guid userId, ExternalAccountType type, long activity);
	
	public void clickedPost(Post post, User user, long clickedTime);
	
	public BlockView loadBlock(Viewpoint viewpoint, UserBlockData ubd) throws NotFoundException;
	
	public List<BlockView> getStack(Viewpoint viewpoint, User user, long lastTimestamp, int start, int count);

	public UserBlockData lookupUserBlockData(UserViewpoint viewpoint, Guid guid) throws NotFoundException;
	
	public void migrateEverything();
	public void migrateGroups();
	public void migratePost(String postId);
	public void migrateUser(String userId);
	public void migrateGroupChat(String groupId);
	public void migrateGroupMembers(String groupId);
}
