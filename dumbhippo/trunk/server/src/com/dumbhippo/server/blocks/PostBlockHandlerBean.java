package com.dumbhippo.server.blocks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class PostBlockHandlerBean extends AbstractBlockHandlerBean<PostBlockView> implements
		PostBlockHandler {

	@EJB
	private PostingBoard postingBoard;
	
	public PostBlockHandlerBean() {
		super(PostBlockView.class);
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
	    blockView.setPostView(postView);
	    blockView.setRecentMessages(recentMessages);
	    blockView.setPopulated(true);
	}

	public Set<User> getInterestedUsers(Block block) {
		Post post;
		try {
			post = EJBUtil.lookupGuid(em, Post.class, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException("Invalid post id in data1 " + block, e);
		}
		Set<User> postRecipients = new HashSet<User>();
		Set<Resource> resources = post.getExpandedRecipients();
		for (Resource r : resources) {
			AccountClaim a = r.getAccountClaim();
			if (a != null)
				postRecipients.add(a.getOwner());
		}
		return postRecipients;
	}
}
