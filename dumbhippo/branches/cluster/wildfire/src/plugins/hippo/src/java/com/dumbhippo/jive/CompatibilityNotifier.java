package com.dumbhippo.jive;

import org.jivesoftware.util.Log;

import com.dumbhippo.live.LiveEventListener;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.PostCreatedEvent;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.util.EJBUtil;

/** 
 * This class handles sending out various notifications for the old
 * (pre-block-stacking) client.
 *  
 * @author otaylor
 */
public class CompatibilityNotifier {
	private PostCreatedListener postCreatedListener = new PostCreatedListener();
	
	public void start() throws IllegalStateException {
		LiveState.addEventListener(PostCreatedEvent.class, postCreatedListener);		
	}

	public void stop() {
		LiveState.removeEventListener(PostCreatedEvent.class, postCreatedListener);
	}
	
	private class PostCreatedListener implements LiveEventListener<PostCreatedEvent> { 
		public void onEvent(PostCreatedEvent event) {
			MessageSender messageSender = MessageSender.getInstance();
			
			PostingBoard postingBoard = EJBUtil.defaultLookup(PostingBoard.class);
			Post post;
			try {
				post = postingBoard.loadRawPost(SystemViewpoint.getInstance(), event.getPostId());
			} catch (NotFoundException e) {
				Log.error("Got PostCreatedEvent for a non-existant post");
				return;
			}
			
			for (Resource resource : post.getExpandedRecipients()) {
				// Since we are sending out initial notifications, we only need
				// to worry about people who actually had accounts when the post
				// was created.
				if (!(resource instanceof Account))
					continue;
				
				// Avoid creating the payload for users not on this server
				User user = ((Account)resource).getOwner();
				if (!messageSender.userIsPresent(user.getGuid()))
					continue;
				
				messageSender.sendNewPostMessage(user, post);
			}
		}
	}
}
