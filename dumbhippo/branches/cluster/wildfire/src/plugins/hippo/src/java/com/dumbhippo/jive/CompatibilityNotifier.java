package com.dumbhippo.jive;

import java.util.Set;

import org.jivesoftware.util.Log;

import com.dumbhippo.live.GroupEvent;
import com.dumbhippo.live.LiveEventListener;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.PostCreatedEvent;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
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
	private LiveEventListener<PostCreatedEvent> postCreatedListener = new LiveEventListener<PostCreatedEvent>() {
		public void onEvent(PostCreatedEvent event) {
			onPostCreatedEvent(event);
		}
	};
	
	private LiveEventListener<GroupEvent> groupEventListener = new LiveEventListener<GroupEvent>() {
		public void onEvent(GroupEvent event) {
			onGroupEvent(event);
		}
	};

	public void start() throws IllegalStateException {
		LiveState.addEventListener(PostCreatedEvent.class, postCreatedListener);		
		LiveState.addEventListener(GroupEvent.class, groupEventListener);		
	}

	public void stop() {
		LiveState.removeEventListener(PostCreatedEvent.class, postCreatedListener);
		LiveState.removeEventListener(GroupEvent.class, groupEventListener);		
	}
	
	private void onPostCreatedEvent(PostCreatedEvent event) {
		MessageSender messageSender = MessageSender.getInstance();
		
		PostingBoard postingBoard = EJBUtil.defaultLookup(PostingBoard.class);
		Post post;
		try {
			post = postingBoard.loadRawPost(SystemViewpoint.getInstance(), event.getPostId());
		} catch (NotFoundException e) {
			Log.error("Got PostCreatedEvent for a non-existent post");
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
			messageSender.sendNewPostMessage(user, post);
		}
	}
	
	private void onGroupEvent(GroupEvent event) {
		MessageSender messageSender = MessageSender.getInstance();
		GroupSystem groupSystem = EJBUtil.defaultLookup(GroupSystem.class);
		
		Group group;
		try {
			group = groupSystem.lookupGroupById(SystemViewpoint.getInstance(), event.getGroupId());
		} catch (NotFoundException e) {
			Log.error("Got GroupEvent for non-existent group");
			return;
		}
		
		GroupMember groupMember;
		try {
			groupMember = groupSystem.getGroupMember(group, event.getResourceId());
		} catch (NotFoundException e) {
			Log.error("Got GroupEvent for non-existent group member");
			return;
		}

		// If we send anything but these statuses to the client, it gets confused
        if (!(groupMember.getStatus().equals(MembershipStatus.FOLLOWER) ||
              groupMember.getStatus().equals(MembershipStatus.ACTIVE)))
        	return;
		
		Set<User> recipients = groupSystem.getMembershipChangeRecipients(group);
		for (User recipient : recipients) {
			messageSender.sendGroupMembershipChange(recipient, groupMember);
		}
	}
}
