package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.server.listeners.AccountStatusListener;
import com.dumbhippo.server.listeners.ExternalAccountsListener;
import com.dumbhippo.server.listeners.GroupCreationListener;
import com.dumbhippo.server.listeners.GroupMembershipListener;
import com.dumbhippo.server.listeners.PostListener;
import com.dumbhippo.server.listeners.UserCreationListener;
import com.dumbhippo.server.views.GroupMugshotView;
import com.dumbhippo.server.views.PersonMugshotView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

/** 
 * The stacker manages the stack of Block objects and the UserBlockData associated with them.
 * 
 * @author Havoc Pennington
 */
@Local
public interface Stacker
	extends AccountStatusListener,
	PostListener, ExternalAccountsListener, GroupMembershipListener {
	
	// Listener methods. FIXME these should all be moved to their respective 
	// block handler beans.
	public void onPostCreated(Post post);
	public void onPostDisabledToggled(Post post);
	public void onGroupMemberCreated(GroupMember member);
	public void onExternalAccountCreated(User user, ExternalAccount external);
	public void onAccountDisabledToggled(Account account);
	public void onAccountAdminDisabledToggled(Account account);
	public void onMusicSharingToggled(Account account);

	// FIXME createBlock should probably move to AbstractBlockHandlerBean once 
	// the stacker itself never calls it, if that happens
	public Block createBlock(BlockKey key);
	public void stack(BlockKey key, long activity, Guid participantId, boolean isGroupParticipation);
	public void stack(BlockKey key, long activity);
	
	// FIXME: All of the stack<BlockType> methods below could be usefully
	// changed to take attached objects rather than object identifiers. They are meant
	// to be called within the transaction where the object is created or modified.
	// These methods are used when activity should cause the timestamp of a block to change. 
	// They are per block type. 
	public void stackMusicPerson(Guid userId, long activity);
	public void stackGroupChat(Guid groupId, long activity, Guid participantId);
	public void stackPost(Guid postId, long activity, Guid participantId, boolean isGroupParticipation);
	public void stackGroupMember(GroupMember member, long activity);
	public void stackFacebookPerson(User user, boolean onlySelf, long activity);
	public void stackBlogPerson(User user, boolean onlySelf, long activity);
	
	public void clickedPost(Post post, User user, long clickedTime);
	
	public BlockView loadBlock(Viewpoint viewpoint, UserBlockData ubd) throws NotFoundException;
	
	public void pageStack(Viewpoint viewpoint, User user, Pageable<BlockView> pageable, boolean participantOnly);

	/**
	 * Fetch recently active users on the system, along with a snapshot of their activity.
	 *  
	 * @param viewpoint viewpoint from which to view the returned blocks. Note that 
	 *     the selection of blocks isn't done using this viewpoint, but is rather
	 *     based on what is public to the entire system, so, for example,
	 *     activity in private groups that the viewer is a member of won't be visible.  
	 * @param startUser starting point in search (skip this number of active user)
	 * @param userCount maximum number of active users to return
	 * @param blocksPerUser number of blocks to return for each item in the result list
	 * @param includeGroupUpdates whether group updates should be included
	 * @return a list of PersonMugshotView objects that combine a PersonView and a list
	 *     of BlockViews
	 */
	public List<PersonMugshotView> getRecentUserActivity(Viewpoint viewpoint, int startUser, int userCount, int blocksPerUser, boolean includeGroupUpdates);
	
	/**
	 * Fetch recently active users on the system, along with a snapshot of their activity,
	 *  using a Pageable object.
	 * 
	 * @param viewpoint viewpoint from which to view the returned blocks. Note that 
	 *     the selection of blocks isn't done using this viewpoint, but is rather
	 *     based on what is public to the entire system, so, for example,
	 *     activity in private groups that the viewer is a member of won't be visible.  
	 * @param pageable object that contains information about what range of results to return
	 *     and that is used to store the results. 
	 * @param blocksPerUser number of blocks to return for each item in the result list
	 */
	public void pageRecentUserActivity(Viewpoint viewpoint, Pageable<PersonMugshotView> pageable, int blocksPerUser);

	// Returns a complete stack for the user.
	public List<BlockView> getStack(Viewpoint viewpoint, User user, long lastTimestamp, int start, int count);
	// Returns parts of the stack in which the user was an active participant if participantOnly is set to true.
	public List<BlockView> getStack(Viewpoint viewpoint, User user, long lastTimestamp, int start, int count, boolean participantOnly);

	public void pageStack(Viewpoint viewpoint, Group group, Pageable<BlockView> pageable, boolean byParticipation);	

	/**
	 * Fetch recently active groups on the system, along with a snapshot of their activity.
	 *  
	 * @param viewpoint viewpoint from which to view the returned blocks. Note that 
	 *     the selection of blocks isn't done using this viewpoint, but is rather
	 *     based on what is public to the entire system, so, for example,
	 *     activity in private groups that the viewer is a member of won't be visible.  
	 * @param startGroup starting point in search (skip this number of active groups)
	 * @param groupCount maximum number of active groups to return
	 * @param blocksPerGroup number of blocks to return for each group
	 * @return a list of GroupMugshotView objects that combine a GroupView and a list
	 *   of BlockViews
	 */
	public List<GroupMugshotView> getRecentGroupActivity(Viewpoint viewpoint, int startGroup, int groupCount, int blockPerGroup);

	/**
	 * Fetch recently active groups on the system, along with a snapshot of their activity,
	 *  using a Pageable object.
	 * 
	 * @param viewpoint viewpoint from which to view the returned blocks. Note that 
	 *     the selection of blocks isn't done using this viewpoint, but is rather
	 *     based on what is public to the entire system, so, for example,
	 *     activity in private groups that the viewer is a member of won't be visible.  
	 * @param pageable object that contains information about what range of results to return
	 *     and that is used to store the results. 
	 * @param blocksPerGroup number of blocks to return for each item in the result list
	 */
	public void pageRecentGroupActivity(Viewpoint viewpoint, Pageable<GroupMugshotView> pageable, int blockPerGroup);

	public UserBlockData lookupUserBlockData(UserViewpoint viewpoint, Guid guid) throws NotFoundException;
	
	public void setBlockHushed(UserBlockData userBlockData, boolean hushed);
	
	public void migrateEverything();
	public void migrateParticipation();
	public void migrateGroupBlockData();
	public void migrateGroups();
	public void migratePost(String postId);
	public void migratePostParticipation(String postId);
	public void migrateUser(String userId);
	public void migrateBlockParticipation(String blockId);
	public void migrateGroupChat(String groupId);
	public void migrateGroupChatParticipation(String groupId);
	public void migrateGroupMembers(String groupId);
	public void migrateGroupBlockData(String blockId);
}
