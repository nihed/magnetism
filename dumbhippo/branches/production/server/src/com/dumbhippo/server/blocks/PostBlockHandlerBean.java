package com.dumbhippo.server.blocks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.FeedPost;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.StackFilterFlags;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class PostBlockHandlerBean extends AbstractBlockHandlerBean<PostBlockView> implements
		PostBlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(PostBlockHandlerBean.class);
	
	@EJB
	private PostingBoard postingBoard;
	
	public PostBlockHandlerBean() {
		super(PostBlockView.class);
	}

	public BlockKey getLookupOnlyKey(Post post) {
		return getLookupOnlyKey(post.getGuid());
	}
	
	public BlockKey getLookupOnlyKey(Guid postId) {
		return new BlockKey(BlockType.POST, postId, true);
	}		
	
	public BlockKey getKey(Post post) {
		User poster = post.getPoster();
		return new BlockKey(BlockType.POST, post.getGuid(), poster == null ? null : poster.getGuid());
	}
	
	@Override
	protected void populateBlockViewImpl(PostBlockView blockView) throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();
		
	    PostView postView;
		try {
			postView = postingBoard.loadPost(viewpoint, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new BlockNotVisibleException("Post for the block wasn't visible", e);
		}
	    List<ChatMessageView> recentMessages = chatSystem.viewMessages(
	        chatSystem.getNewestMessages(block, PostBlockView.RECENT_MESSAGE_COUNT),
			viewpoint);
	    
		int messageCount;
		if (recentMessages.size() < PostBlockView.RECENT_MESSAGE_COUNT) // Optimize out a query
			messageCount = recentMessages.size();
		else
			messageCount = chatSystem.getMessageCount(block);
			    
	    blockView.populate(postView, recentMessages, messageCount);
	}

	private Post loadPost(Block block) {
		try {
			return EJBUtil.lookupGuid(em, Post.class, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException("Invalid post id in data1 " + block, e);
		}
	}
	
	public Set<User> getInterestedUsers(Block block) {
		Post post = loadPost(block);
		Set<User> postRecipients = new HashSet<User>();
		Set<Resource> resources = post.getExpandedRecipients();
		for (Resource r : resources) {
			AccountClaim a = r.getAccountClaim();
			if (a != null)
				postRecipients.add(a.getOwner());
		}
		return postRecipients;
	}

	public Set<Group> getInterestedGroups(Block block) {
		Post post = loadPost(block);
		return post.getGroupRecipients();
	}
	
	public void onPostCreated(Post post) {
		boolean isFeed = (post instanceof FeedPost);
		Block block = stacker.createBlock(getKey(post));
		block.setPublicBlock(post.isPublic() && !post.isDisabled());
		if (isFeed)
			block.setFilterFlags(StackFilterFlags.FEED.getValue());
		User poster = post.getPoster();
		stacker.stack(block, post.getPostDate().getTime(),
					poster, !isFeed, StackReason.NEW_BLOCK);
	}

	public void onPostDisabledToggled(Post post) {
		Block block;
		try {
			block = stacker.queryBlock(getLookupOnlyKey(post));
		} catch (NotFoundException e) {
			logger.warn("No block found for post {} - migration needed?", post);
			return;
		}
		block.setPublicBlock(post.isPublic() && !post.isDisabled());
	}

	public void onPostClicked(Post post, User user, long clickedTime) {
		stacker.blockClicked(getLookupOnlyKey(post), user, clickedTime);
	}

	public Block lookupBlock(Post post) {
		try {
			return stacker.queryBlock(getLookupOnlyKey(post));
		} catch (NotFoundException e) {
			throw new RuntimeException("No Block found for Post " + post.getId() + ", migration needed?", e);
		}
			
	}
	
	public UserBlockData lookupUserBlockData(UserViewpoint viewpoint, Post post) throws NotFoundException {
		return stacker.lookupUserBlockData(viewpoint, getLookupOnlyKey(post));
	}
	
	
	public void setPostHushed(UserViewpoint viewpoint, Post post, boolean hushed) {
		UserBlockData ubd;
		try {
			ubd = lookupUserBlockData(viewpoint, post);
			stacker.setBlockHushed(ubd, hushed);
		} catch (NotFoundException e) {
			logger.warn("No UserBlockData found for post {}, user {}, skipping setPostHushed",
					    post, viewpoint.getViewer());
		}
	}
}
