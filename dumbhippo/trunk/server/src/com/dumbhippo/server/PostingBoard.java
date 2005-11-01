package com.dumbhippo.server;

import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

@Local
public interface PostingBoard {

	public List<PostInfo> getPostInfosFor(Person poster, Person viewer, int max);

	public List<PostInfo> getReceivedPostInfos(Person recipient, int max);

	public List<PostInfo> getGroupPostInfos(Group recipient, Person viewer, int max);
	
	public List<PostInfo> getContactPostInfos(Person viewer, boolean include_received, int max);

	public Post doLinkPost(Person poster, PostVisibility visibility, String title, String text, String link, Set<String> recipientGuids)
		throws ParseException, GuidNotFoundException;

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
	public Post createPost(Person poster, PostVisibility visibility, String title, String text,
			Set<Resource> resources, Set<Person> personRecipients, Set<Group> groupRecipients,
			Set<Person> expandedRecipients);

	public Post loadPost(Guid guid);
	
	public PostInfo loadPostInfo(Guid guid, Person viewer);

	public void postClickedBy(Post post, Person clicker);
}
