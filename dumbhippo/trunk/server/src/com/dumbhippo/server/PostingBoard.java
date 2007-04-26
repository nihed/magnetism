package com.dumbhippo.server;

import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupFeed;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.server.views.EntityView;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.Viewpoint;

@Local
public interface PostingBoard {
	
	public int getPostsForCount(Viewpoint viewpoint, Person forPerson);	
	
	public boolean canViewPost(Viewpoint viewpoint, Post post);	
	
	/**
	 * Count the number of posts sent to a group ignoring visibility.
	 * (So the count may be greater than the number of posts a user
	 * can actually see.) 
	 * 
	 * @param group a group
	 * @return count of the number of posts sent to the group
	 */
	public int getGroupPostsCount(Group group);
	
	enum InviteRecipients {
		DONT_INVITE,         // Just send out plain emails without invitation links
		INVITE_IF_POSSIBLE,  // Send out plain emails if sender has no invitations  
		MUST_INVITE          // Skip sending email if sender has no invitations
	}
	
	/**
	 * Shares a link. 
	 * 
	 * @param poster
	 * @param isPublic make post publicly visible even if it isn't to any public groups
	 * @param title
	 * @param text
	 * @param link
	 * @param recipients
	 * @param inviteRecipients
	 * @param postInfo
	 * @return
	 * @throws NotFoundException
	 */
	public Post doLinkPost(User poster, boolean isPublic, String title, String text, URL link, Set<GuidPersistable> recipients, InviteRecipients inviteRecipients, PostInfo postInfo)
		throws NotFoundException;

	public Post doShareGroupPost(User poster, Group group, String title, String text, Set<GuidPersistable> recipients, InviteRecipients inviteRecipients)
		throws NotFoundException;
	
	/**
	 * Send out notifications of a new Feed item
	 * @param The GroupFeed object that "sends out the post"; the GroupFeed is the
	 *    sender conceptually rather than the Feed because the image for the feed
	 *    is the group's image. The recipients will be the members of the GroupFeed's group
	 * @param the entry to share
	 */
	public void doFeedPost(GroupFeed feed, FeedEntry entry);

	public void doGroupInvitationPost(User owner, Group group);	
	
	public void doShareLinkTutorialPost(User recipient);
	
	public void doNowPlayingTutorialPost(User recipient);
	
	public Post loadRawPost(Viewpoint viewpoint, Guid guid) throws NotFoundException;
	
	public PostView loadPost(Viewpoint viewpoint, Guid guid) throws NotFoundException;
	
	public PostView getPostView(Viewpoint viewpoint, Post post);
	
	/**
	 * Returns a set of EntityView (i.e. PersonView and GroupView) which contains the entities directly
	 * referenced by this post.  At the moment, this is just the person recipients, the poster, and the
	 * group recipients.  This does not include indirectly referenced entities (e.g. the members of a group).
	 * 
	 * @param viewpoint viewpoint from which the post is viewed
	 * @param post the post in question
	 * @return set of EntityView
	 */
	public Set<EntityView> getReferencedEntities(Viewpoint viewpoint, Post post);
		
	/**
	 * Notifies system that the post was viewed by the given person.
	 * If the postId is bad, silently does nothing.  This also
	 * un-ignores the post if the user had it ignored.
	 * 
	 * @param postId the ID of the post that was clicked
	 * @param clicker person who clicked on the post
	 */
	public void postViewedBy(Post post, User clicker);
	// Should get rid of this variant, but it's slightly a pain, due to the
	// usage from web servlets
	public void postViewedBy(String postId, User clicker);
	
	/**
	 * Search the database of posts using Lucene.
	 * 
	 * @param viewpoint the viewpoint being searched from
	 * @param queryString the search string to use, in Lucene syntax. The search
	 *   will be done across both the title and description fields
	 * @return a PostSearchResult object representing the search; you should
	 *    check the getError() method of this object to determine if an error
	 *    occurred (such as an error parsing the query string) 
	 */
	public PostSearchResult searchPosts(Viewpoint viewpoint, String queryString);
	
	/**
	 * Get a range of posts from the result object returned from searchPosts(). 
	 * This is slightly more efficient than calling PostSearchResult getPosts(),
	 * because we avoid some EJB overhead.
	 * 
	 * @param viewpoint the viewpoint for the returned PostView objects; must be the same 
	 *        as the viewpoint passed in when calling searchPosts()
	 * @param
	 * @param start the index of the first post to retrieve (starting at zero)
	 * @param count the maximum number of items desired 
	 * @return a list of PostView objects; may have less than count items when no more
	 *        are available. 
	 */
	public List<PostView> getPostSearchPosts(Viewpoint viewpoint, PostSearchResult searchResult, int start, int count);
	
	/**
	 * Determines whether a post is sufficiently interesting to one of the recipients
	 * to be worth sending an email notification for it. This is currently defined
	 * as the recipient being either a direct personRecipient of the post or a member
	 * of private group that the post was sent to.
	 * @param post
	 * @param recipient recipient to which the notification will be sent; no spidering
	 *   will be done, so this must be the same resource that is the post recipient
	 *   or group member.
	 * @return true if an email notification should be sent
	 */
	public boolean worthEmailNotification(Post post, Resource recipient);
	
	public boolean postIsGroupNotification(Post post);
	
	public void sendPostNotifications(Post post, PostType postType);

	/**
	 * Sets the disabled status on the post. Can be used from the admin console
	 * to disable posts.
	 * 
	 * @param post
	 * @param disabled
	 */
	public void setPostDisabled(Post post, boolean disabled);
	
	/**
	 * Gets a list of all ids for posts.  Intended to be used for
	 * batch operations like indexing.  Also excludes group 
	 * membership notifications.
	 * 
	 * @return a list of guids of all "real" posts (see above)
	 */
	public List<Guid> getAllPostIds();
}
