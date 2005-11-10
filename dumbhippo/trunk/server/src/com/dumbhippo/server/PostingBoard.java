package com.dumbhippo.server;

import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

@Local
public interface PostingBoard {

	public List<PostView> getPostsFor(Viewpoint viewpoint, Person poster, int max);

	public List<PostView> getReceivedPosts(Viewpoint viewpoint, Person recipient, int max);

	public List<PostView> getGroupPosts(Viewpoint viewpoint, Group recipient, int max);
	
	public List<PostView> getContactPosts(Viewpoint viewpoint, Person user, boolean include_received, int max);

	public Post doLinkPost(User poster, PostVisibility visibility, String title, String text, String link, Set<GuidPersistable> recipients)
		throws GuidNotFoundException;

	public void doShareLinkTutorialPost(Person recipient);
	
	/**
	 * You don't want to use this directly because it doesn't send any notifications.
	 * It's only public so we can use the transaction annotation on it.
	 * 
	 * @param poster
	 * @param visibility
	 * @param title
	 * @param text
	 * @param resources
	 * @param personRecipients
	 * @param groupRecipients
	 * @param expandedRecipients
	 * @return
	 */
	public Post createPost(User poster, PostVisibility visibility, String title, String text,
			Set<Resource> resources, Set<Person> personRecipients, Set<Group> groupRecipients,
			Set<Person> expandedRecipients);

	public Post loadRawPost(Viewpoint viewpoint, Guid guid);
	
	public PostView loadPost(Viewpoint viewpoint, Guid guid);

	public void postClickedBy(Post post, User clicker);
}
