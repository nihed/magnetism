package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.blocks.BlockView;
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
public interface Stacker {

	public Block getOrCreateBlock(BlockKey key);
	public Block createBlock(BlockKey key);
	public Block queryBlock(BlockKey key) throws NotFoundException;

	public Block stack(BlockKey key, long activity, User participant, boolean isGroupParticipation, StackReason reason);
	public Block stack(BlockKey key, long activity, StackReason reason);
	public void stack(Block block, long activity, User participant, boolean isGroupParticipation, StackReason reason);
	public void stack(Block block, long activity, StackReason reason);

	/**
	 * Re-requests the desired user/group block datas from the block handler, 
	 * and toggles the deleted flag on any ubd/gdb that already 
	 * exist to reflect the currently-desired state. Does not create any ubd/gbd that didn't exist 
	 * before, however, since it would not know the activity/participant/reason and other info.
	 * If you want to create new blocks you can just stack() instead, it will automatically 
	 * also do a refreshDeletedFlags() equivalent.
	 * @param block
	 */
	public void refreshDeletedFlags(Block block);
	public void refreshDeletedFlags(BlockKey key);
	
	/** 
	 * A migration/debug method that refreshes deleted flags on *all* blocks in the database
	 */
	public void refreshDeletedFlagsOnAllBlocks();
	
	public void refreshDeletedFlagsOnAllBlocksWithType(String typeName);
	
	public void blockClicked(BlockKey key, User user, long clickedTime);
	
	public BlockView loadBlock(Viewpoint viewpoint, UserBlockData ubd) throws NotFoundException;
	
	public void pageStack(Viewpoint viewpoint, User user, Pageable<BlockView> pageable, boolean participantOnly);

	public void pageStack(Viewpoint viewpoint, User user, Pageable<BlockView> pageable, long lastTimestamp, boolean participantOnly);
	
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
	
	public List<PersonMugshotView> getContactActivity(Viewpoint viewpoint, User user, int start, int count, int blocksPerUser);
	
	public List<GroupMugshotView> getMugshotViews(Viewpoint viewpoint, List<Group> groups, int blockPerGroup, boolean publicOnly);
	
	public void pageContactActivity(Viewpoint viewpoint, User viewedUser, int blocksPerUser, Pageable<PersonMugshotView> contactMugshots);

	public List<GroupMugshotView> getUserGroupActivity(Viewpoint viewpoint, User user, int start, int count, int blocksPerUser, GroupQueryType groupType);	
	
	public enum GroupQueryType {
		FOLLOWED,
		ACTIVE
	};
	
	public void pageUserGroupActivity(Viewpoint viewpoint, User user, int blocksPerGroup, GroupQueryType groupType, Pageable<GroupMugshotView> activeGroups);	
	
	public User getRandomActiveUser(Viewpoint viewpoint);
	
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
	public UserBlockData lookupUserBlockData(UserViewpoint viewpoint, BlockKey key) throws NotFoundException;
	
	public void setBlockHushed(UserBlockData userBlockData, boolean hushed);
	
	public void migrateEverything();
	public void migrateParticipation();
	public void migrateGroupBlockData();
	public void migrateGroups();
	public void migrateUsers();
	public void migratePost(String postId);
	public void migratePostParticipation(String postId);
	public void migrateUser(String userId);
	public void migrateBlockParticipation(String blockId);
	public void migrateGroupChat(String groupId);
	public void migrateGroupChatParticipation(String groupId);
	public void migrateGroupMembers(String groupId);
	public void migrateGroupBlockData(String blockId);
	public void migrateFlickr(User user);
	public void migrateTwitter(User user);
}
