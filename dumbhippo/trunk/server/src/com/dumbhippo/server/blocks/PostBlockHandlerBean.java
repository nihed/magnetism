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
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.Resource;
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

	public BlockKey getKey(Post post) {
		return getKey(post.getGuid());
	}

	public BlockKey getKey(Guid postId) {
		return new BlockKey(BlockType.POST, postId);
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
	    List<ChatMessageView> recentMessages = postingBoard.viewPostMessages(
	        postingBoard.getNewestPostMessages(postView.getPost(), PostBlockView.RECENT_MESSAGE_COUNT),
			viewpoint);
	    
		int messageCount;
		if (recentMessages.size() < PostBlockView.RECENT_MESSAGE_COUNT) // Optimize out a query
			messageCount = recentMessages.size();
		else
			messageCount = postingBoard.getPostMessageCount(postView.getPost());
			    
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
		Block block = stacker.createBlock(getKey(post));
		block.setPublicBlock(post.isPublic() && !post.isDisabled());
		User poster = post.getPoster();
		stacker.stack(block, post.getPostDate().getTime(),
					poster, !(post instanceof FeedPost), StackReason.NEW_BLOCK);
	}

	public void onPostDisabledToggled(Post post) {
		Block block;
		try {
			block = stacker.queryBlock(getKey(post));
		} catch (NotFoundException e) {
			logger.warn("No block found for post {} - migration needed?", post);
			return;
		}
		block.setPublicBlock(post.isPublic() && !post.isDisabled());
	}

	public void onPostMessageCreated(PostMessage message) {
		stacker.stack(getKey(message.getPost()), message.getTimestamp().getTime(),
				message.getFromUser(), true, StackReason.CHAT_MESSAGE);
	}

	public void onPostClicked(Post post, User user, long clickedTime) {
		stacker.blockClicked(getKey(post), user, clickedTime);
	}

	public Block lookupBlock(Post post) {
		try {
			return stacker.queryBlock(getKey(post));
		} catch (NotFoundException e) {
			throw new RuntimeException("No Block found for Post {}, migration needed?", e);
		}
	}
	
	public UserBlockData lookupUserBlockData(UserViewpoint viewpoint, Post post) throws NotFoundException {
		return stacker.lookupUserBlockData(viewpoint, getKey(post));
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
