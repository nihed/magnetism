package com.dumbhippo.server;

import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.User;
import com.dumbhippo.postinfo.PostInfo;

@Local
public interface PostingBoard {

	public List<PostView> getPostsFor(Viewpoint viewpoint, Person poster, int start, int max);

	public List<PostView> getReceivedPosts(Viewpoint viewpoint, Person recipient, int start, int max);

	public List<PostView> getGroupPosts(Viewpoint viewpoint, Group recipient, int start, int max);
	
	public List<PostView> getContactPosts(Viewpoint viewpoint, Person user, boolean include_received, int start, int max);

	public List<PostView> getPostsFor(Viewpoint viewpoint, Person poster, String search, int start, int max);

	public List<PostView> getReceivedPosts(Viewpoint viewpoint, Person recipient, String search, int start, int max);

	public List<PostView> getGroupPosts(Viewpoint viewpoint, Group recipient, String search, int start, int max);
	
	public Post doLinkPost(User poster, PostVisibility visibility, String title, String text, URL link, Set<GuidPersistable> recipients, boolean inviteRecipients, PostInfo postInfo)
		throws NotFoundException;

	public Post doShareGroupPost(User poster, Group group, String text, Set<GuidPersistable> recipients, boolean inviteRecipients)
		throws NotFoundException;
	
	public void doShareLinkTutorialPost(User recipient);
	
	public Post loadRawPost(Viewpoint viewpoint, Guid guid);
	
	public PostView loadPost(Viewpoint viewpoint, Guid guid);

	/**
	 * Notifies system that the post was viewed by the given person.
	 * If the postId is bad, silently does nothing.
	 * 
	 * @param postId the ID of the post that was clicked
	 * @param clicker person who clicked on the post
	 */
	public void postViewedBy(String postId, User clicker);
}
