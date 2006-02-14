package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.live.LiveUser;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;

@Local
public interface MessageSender {
	public void sendPostNotification(Resource recipient, Post post, boolean isTutorialPost);
	
	/**
	 * Send out notifications to the sender and recipients of a post
	 * about new viewers of the post. (Currently resends the original
	 * post information augmented with a list of everybody who has
	 * seen the post. This won't scale.) 
	 * 
	 * @param post a Post
	 * @param viewers The list of people who have seen the post, sorted
	 *   with the most-recent viewer first
	 * @param clicker The new viewer; no notification is sent to them
	 */
	public void sendPostClickedNotification(Post post, List<User> viewers, User clicker);
	
	public void sendMySpaceNameChangedNotification(User user);
	
	public void sendMySpaceContactCommentNotification(User user);

	public void sendHotnessChanged(LiveUser user);

	public void sendActivePostsChanged(LiveUser user);
}
