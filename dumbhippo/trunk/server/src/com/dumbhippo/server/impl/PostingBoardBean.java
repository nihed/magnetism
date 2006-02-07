package com.dumbhippo.server.impl;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.PostViewedEvent;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.postinfo.NodeName;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.postinfo.PostInfoType;
import com.dumbhippo.postinfo.ShareGroupPostInfo;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PostInfoSystem;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.RecommenderSystem;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class PostingBoardBean implements PostingBoard {

	private static final Logger logger = GlobalSetup.getLogger(PostingBoardBean.class);	
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@EJB
	private IdentitySpider identitySpider;	

	@EJB
	private AccountSystem accountSystem;

	@EJB
	private MessageSender messageSender;

	@EJB
	private Configuration configuration;
	
	@EJB
	private PostInfoSystem infoSystem;
	
	@EJB
	private InvitationSystem invitationSystem;
	
	@EJB
	private GroupSystem groupSystem;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private RecommenderSystem recommenderSystem;
	
	@javax.annotation.Resource
	private EJBContext ejbContext;
	
	private void sendPostNotifications(Post post, Set<Resource> expandedRecipients, boolean isTutorialPost) {
		// FIXME I suspect this should be outside the transaction and asynchronous
		logger.debug("Sending out jabber/email notifications...");
		for (Resource r : expandedRecipients) {
			messageSender.sendPostNotification(r, post, isTutorialPost);
		}
	}
	
	/**
	 * If someone shares a link that's already been shared, we want to strip out the
	 * noise we added with the frameset/redirect stuff
	 * 
	 * @param original the original url
	 * @return either the same url or a fixed-up one
	 */
	private URL removeFrameset(URL original) {
		logger.debug(String.format("do we remove frameset with path %s host %s query %s",
				original.getPath(), original.getHost(), original.getQuery()));
		
		if (!original.getPath().equals("/visit"))
			return original;
		 
		URL baseurl = configuration.getBaseUrl();
		if (!original.getHost().equals(baseurl.getHost()))
			return original;
		String q = original.getQuery();
		if (q == null)
			return original;

		int i = q.indexOf("post=");
		if (i < 0)
			return original;
		i += "post=".length();
		String postId;
		try {
			String decoded = URLDecoder.decode(q.substring(i), "UTF-8");
			postId = decoded.substring(0, Guid.STRING_LENGTH);
		} catch (UnsupportedEncodingException e) {
			logger.error("should not be a checked exception, bad encoding", e);
			return original;
		} catch (IndexOutOfBoundsException e) {
			logger.warn("Failed to parse query string on frameset (too short postId): " + q, e);
			return original;
		}
		
		Post post;
		try {
			post = identitySpider.lookupGuidString(Post.class, postId);
		} catch (ParseException e) {
			logger.warn("Failed to parse postId from frameset: " + postId, e);
			return original;
		} catch (NotFoundException e) {
			logger.warn("Failed to parse postId from frameset: " + postId, e);
			return original;
		}
		
		URL replacement = post.getUrl();
		if (replacement == null) {
			logger.warn("Old post does not have an URL: " + post);
			return original;
		}
		
		logger.debug("Changing URL to '" + replacement.toExternalForm() + "'");
		
		return replacement;
	}
	
	private Post doLinkPostInternal(User poster, PostVisibility visibility, String title, String text, URL url, Set<GuidPersistable> recipients, boolean inviteRecipients, PostInfo postInfo, boolean isTutorialPost) throws NotFoundException {
		url = removeFrameset(url);
		
		Set<Resource> shared = (Collections.singleton((Resource) identitySpider.getLink(url.toExternalForm())));
		
		// for each recipient, if it's a group we want to explode it into persons
		// (but also keep the group itself), if it's a person we just add it
		
		Set<Resource> personRecipients = new HashSet<Resource>();
		Set<Group> groupRecipients = new HashSet<Group>();
		Set<Resource> expandedRecipients = new HashSet<Resource>();
		
		boolean addedPoster = false;
		
		// sort into persons and groups
		for (GuidPersistable r : recipients) {
			if (r instanceof Person) {
				Person p = (Person) r;
				Resource bestResource = identitySpider.getBestResource(p);
				personRecipients.add(bestResource);
				if (inviteRecipients) {
					// this is smart about doing nothing if the person is already invited
					// or already has an account (it's also very cheap if bestResource is an Account)
					invitationSystem.createInvitation(poster, bestResource);
				}
				
				if (p.equals(poster)) {
					addedPoster = true;
				}
				
			} else if (r instanceof Group) {
				groupRecipients.add((Group) r);
			} else {
				// wtf
				throw new NotFoundException("recipient not found " + r.getId());
			}
		}

		if (!addedPoster)
			personRecipients.add(identitySpider.getBestResource(poster));
		
		// build expanded recipients
		expandedRecipients.addAll(personRecipients);
		for (Group g : groupRecipients) {
			for (GroupMember groupMember : g.getMembers()) {
				if (groupMember.isParticipant())
					expandedRecipients.add(groupMember.getMember());
			}
		}
		
		// if this throws we shouldn't send out notifications, so do it first
		Post post = createPost(poster, visibility, title, text, shared, personRecipients, groupRecipients, expandedRecipients, postInfo);
		
		sendPostNotifications(post, expandedRecipients, isTutorialPost);
		
		// tell the recommender engine, so ratings can be updated
		recommenderSystem.addRatingForPostCreatedBy(post, poster);
		
		return post;		
	}
	
	public Post doLinkPost(User poster, PostVisibility visibility, String title, String text, URL url, Set<GuidPersistable> recipients, boolean inviteRecipients, PostInfo postInfo) throws NotFoundException {
		return doLinkPostInternal(poster, visibility, title, text, url, recipients, inviteRecipients, postInfo, false);
	}
	
	public Post doShareGroupPost(User poster, Group group, String text, Set<GuidPersistable> recipients, boolean inviteRecipients)
	throws NotFoundException {
		
		for (GuidPersistable r : recipients) {
			if (r instanceof Person)
				groupSystem.addMember(poster, group, (Person)r);
			else
				throw new NotFoundException("No recipient found for id " + r.getId());
		}

		String baseurl = configuration.getProperty(HippoProperty.BASEURL);
		URL url;
		try {
			url = new URL(baseurl + "/group?who=" + group.getId());
		} catch (MalformedURLException e) {
			throw new RuntimeException("We created an invalid url for a group", e);
		}
		
		String title = group.getName();
			
		PostVisibility visibility = group.getAccess() == GroupAccess.SECRET ? PostVisibility.RECIPIENTS_ONLY : PostVisibility.ANONYMOUSLY_PUBLIC;
		
		ShareGroupPostInfo postInfo = PostInfo.newInstance(PostInfoType.SHARE_GROUP, ShareGroupPostInfo.class);
		postInfo.getTree().updateContentChild(group.getId(), NodeName.shareGroup, NodeName.groupId);
		postInfo.makeImmutable();
		
		return doLinkPost(poster, visibility, title, text, url, recipients, inviteRecipients, postInfo);			
	}
	
	public void doShareLinkTutorialPost(User recipient) {
		logger.debug("Sending share link tutorial post");
		User poster = identitySpider.getTheMan();
		URL url;
		String urlText = configuration.getProperty(HippoProperty.BASEURL) + "/account";
		try {
			url = new URL(urlText);
		} catch (MalformedURLException e) {
			logger.error("Malformed tutorial url: " + urlText, e);
			throw new RuntimeException(e);
		}
		String title = "What is this DumbHippo thing?";
		String text = "Set up your account and learn to use DumbHippo by visiting this link"; 
		Set<GuidPersistable> recipientSet = Collections.singleton((GuidPersistable)recipient);
		Post post;
		try {
			post = doLinkPostInternal(poster, PostVisibility.RECIPIENTS_ONLY, title, text, url, recipientSet, false, null, true);
		} catch (NotFoundException e) {
			logger.error("Failed to post: " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
		logger.debug("Tutorial post done: " + post);
	}
	
	private Post createPost(final User poster, final PostVisibility visibility, final String title, final String text, final Set<Resource> resources, 
			               final Set<Resource> personRecipients, final Set<Group> groupRecipients, final Set<Resource> expandedRecipients, final PostInfo postInfo) {
		try {
			return runner.runTaskInNewTransaction(new Callable<Post>() {
				public Post call() {
					logger.debug("saving new Post");
					Post post = new Post(poster, visibility, title, text, personRecipients, groupRecipients, expandedRecipients, resources);
					post.setPostInfo(postInfo);
					em.persist(post);
				
					return post;
				}
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
	}

	private PersonPostData getPersonPostData(Viewpoint viewpoint, Post post) {
		Person viewer = viewpoint.getViewer();
		if (viewer == null)
			return null;
		
		Query q = em.createQuery("SELECT ppd FROM PersonPostData ppd " +
				                 "WHERE ppd.post = :post AND ppd.person = :viewer");
		q.setParameter("post", post);
		q.setParameter("viewer", viewer);
		try {
			return (PersonPostData) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	
	// Hibernate bug: I think we should be able to write
	// EXISTS (SELECT g FROM IN(post.groupRecipients) g WHERE [..])
	// according to the EJB3 persistance spec, but that results in
	// garbage SQL
	
	static final String AUTH_GROUP =
        "g MEMBER OF post.groupRecipients AND " + 
		" (g.access >= " + GroupAccess.PUBLIC.ordinal() + " OR " +
          "EXISTS (SELECT vgm FROM GroupMember vgm, AccountClaim ac " +
          "WHERE vgm.group = g AND vgm.member = ac.resource AND ac.owner = :viewer AND " +
                "vgm.status >= " + MembershipStatus.INVITED.ordinal() + ")) ";
	
	// For fetching visibility of groups in post recipient lists.
	// We optimize the handling of viewer GroupMember since we
	// are already fetching that, and we also treat groups where
	// we were a past member as visible. (See
	// GroupSystemBean.CAN_SEE_POST)
	static final String VISIBLE_GROUP_FOR_POST =
        "g MEMBER OF post.groupRecipients AND " + 
		" (g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " OR " +
          "(vgm IS NOT NULL AND " +
           "vgm.status >= " + MembershipStatus.REMOVED.ordinal() + ")) ";
	
	static final String VISIBLE_GROUP_ANONYMOUS =
        "g MEMBER OF post.groupRecipients AND " + 
		"g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal();
	
	static final String AUTH_GROUP_ANONYMOUS =
        "g MEMBER OF post.groupRecipients AND " + 
		"g.access >= " + GroupAccess.PUBLIC.ordinal();
	
	static final String CAN_VIEW = 
		" (post.visibility = " + PostVisibility.ATTRIBUTED_PUBLIC.ordinal() + " OR " +
              "EXISTS (SELECT ac FROM AccountClaim ac WHERE ac.owner = :viewer " +
              "        AND ac.resource MEMBER OF post.personRecipients) OR " +
              "EXISTS (SELECT g FROM Group g WHERE " + AUTH_GROUP + "))";
	
	static final String CAN_VIEW_ANONYMOUS = 
		" (post.visibility = " + PostVisibility.ATTRIBUTED_PUBLIC.ordinal() + " OR " + 
              "EXISTS (SELECT g FROM Group g WHERE " + AUTH_GROUP_ANONYMOUS + "))";
	
	static final String VIEWER_RECEIVED = 
		" EXISTS(SELECT ac FROM AccountClaim ac " +
		"        WHERE ac.owner = :viewer " +
		"            AND ac.resource MEMBER OF post.expandedRecipients) ";

	static final String ORDER_RECENT = " ORDER BY post.postDate DESC ";

	// If we wanted to indicate undisclosed recipients, we would just omit
	// the VISIBLE_GROUP_FOR_POST then compute visibility by hand on
	// the returned groups; this is cheap since we have the GroupMember
	// for the viewer already
	//
	
	static final String GET_POST_VIEW_QUERY = 
		"SELECT NEW com.dumbhippo.server.impl.GroupQueryResult(g,vgm) " +
		"FROM Post post, Group g LEFT JOIN g.members vgm, AccountClaim ac " +
		"WHERE post = :post AND vgm.member = ac.resource AND ac.owner = :viewer ";
	
	static final String GET_POST_VIEW_QUERY_ANONYMOUS = 
		"SELECT g " +
		"FROM Post post, Group g " +
		"WHERE post = :post";
	
	private void addGroupRecipients(Viewpoint viewpoint, Post post, List<Object> recipients) {
		Person viewer = viewpoint.getViewer();

		Query q;
		if (viewer == null) {
			q = em.createQuery(GET_POST_VIEW_QUERY_ANONYMOUS + " AND " + VISIBLE_GROUP_ANONYMOUS);
		} else {
			q = em.createQuery(GET_POST_VIEW_QUERY + " AND " + VISIBLE_GROUP_FOR_POST);
			q.setParameter("viewer", viewer);
		}

		q.setParameter("post", post);
		
		if (viewer == null) {
			for (Object o : q.getResultList()) {
				recipients.add(new GroupView((Group)o, null, null));
			}
		} else {
			for (Object o : q.getResultList()) {
				GroupQueryResult gr = (GroupQueryResult)o;
				GroupMember groupMember = gr.getGroupMember();
				PersonView inviter  = null;
				
				if (groupMember != null) {
					if (groupMember.getStatus() == MembershipStatus.INVITED) {
						Person adder = groupMember.getAdder();
						if (adder != null)
							inviter = identitySpider.getPersonView(viewpoint, adder);
					}
				}
				recipients.add(new GroupView(gr.getGroup(), groupMember, inviter));
			}
		}
	}

	static final String VISIBLE_PERSON_RECIPIENTS_QUERY = 
		"SELECT resource from Post post, Resource resource, AccountClaim ac " +
		"WHERE post = :post AND ac.owner = :viewer AND ac.resource MEMBER OF post.personRecipients AND " +
		"      resource MEMBER OF post.personRecipients";
	
	private List<Resource> getVisiblePersonRecipients(Viewpoint viewpoint, Post post) {
		@SuppressWarnings("unchecked")
		List<Resource> results = em.createQuery(VISIBLE_PERSON_RECIPIENTS_QUERY)
			.setParameter("post", post)
			.setParameter("viewer", viewpoint.getViewer())
			.getResultList();
		
		return results;
	}
	
	private PostView getPostView(Viewpoint viewpoint, Post post) {
		List<Object> recipients = new ArrayList<Object>();
		
		addGroupRecipients(viewpoint, post, recipients);
		
		// Person recipients are visible only if the viewer is also a person recipient
		for (Resource recipient : getVisiblePersonRecipients(viewpoint, post))
			recipients.add(identitySpider.getPersonView(viewpoint, recipient, PersonViewExtra.PRIMARY_RESOURCE, PersonViewExtra.PRIMARY_AIM));
	
		if (!em.contains(post))
			throw new RuntimeException("can't update post info if Post is not attached");
		
		// Ensure we're updated (potentially blocks for a while)
		infoSystem.updatePostInfo(post);
		
		String info = post.getInfo();
		if (info != null)
			logger.debug("Updated, post info now: " + info.replace("\n",""));
		else
			logger.debug("Updated, post info now null");
		
		try {
			
			return new PostView(ejbContext, post, 
					identitySpider.getPersonView(viewpoint, post.getPoster(), PersonViewExtra.ALL_RESOURCES),
					getPersonPostData(viewpoint, post),
					recipients,
					viewpoint);
		} catch (Exception e) {
			logger.debug("The exception was: " + e);
			throw new RuntimeException(e);
		}
	}
	
	private List<PostView> getPostViews(Viewpoint viewpoint, Query q, String search, int start, int max) {
		if (max > 0)
			q.setMaxResults(max);
		
		q.setFirstResult(start);

		@SuppressWarnings("unchecked")		
		List<Post> posts = q.getResultList();	

		// parallelize all the post updaters
		for (Post p : posts) {
			infoSystem.hintWillUpdateSoon(p);
		}
		
		List<PostView> result = new ArrayList<PostView>();
		for (Post p : posts) {
			PostView pv = getPostView(viewpoint, p);
			if (search != null)
				pv.setSearch(search);
			result.add(pv);
		}
		
		return result;		
	}
	
	private void appendPostLikeClause(StringBuilder queryText, String search) {
		if (search != null) {
			String likeClause = EJBUtil.likeClauseFromUserSearch(search, "post.text", "post.explicitTitle");
			if (likeClause != null) {
				queryText.append(" AND ");
				queryText.append(likeClause);
			}
		}
	}
	
	static final String GET_SPECIFIC_POST_QUERY = 
		"SELECT post from Post post WHERE post.id = :post";

	/**
	 * Check if a particular viewer can see a particular post.
	 * 
	 * @param viewpoint
	 * @param post
	 * @return true if post is visible, false otherwise
	 */
	public boolean canViewPost(Viewpoint viewpoint, Post post) {
		User viewer = viewpoint.getViewer();
		
		Query q;
		StringBuilder queryText = new StringBuilder(GET_SPECIFIC_POST_QUERY + " AND ");
		queryText.append(CAN_VIEW);
		logger.debug("Full canViewPost query is: '" + queryText.toString() + "'");
		
		q = em.createQuery(queryText.toString());
		q.setParameter("post", post);
		q.setParameter("viewer", viewer);
		
		try {
			Post resultPost = (Post)em.createQuery(queryText.toString())
	            .setParameter("post", post)
	            .setParameter("viewer", viewer)
	            .getSingleResult();
			logger.debug("canViewPost query got one result: " + resultPost + "; returning true/access granted");
			return true;
		} catch (EntityNotFoundException e) {
			logger.debug("canViewPost query got no result; returning false/access denied");
			return false;
		}
	}
	
	static final String GET_POSTS_FOR_QUERY =
		"SELECT post FROM Post post WHERE post.poster = :poster";
	
	public List<PostView> getPostsFor(Viewpoint viewpoint, Person poster, String search, int start, int max) {
		Person viewer = viewpoint.getViewer();
		Query q;
		
		StringBuilder queryText = new StringBuilder(GET_POSTS_FOR_QUERY + " AND ");
		if (viewer == null) {
			queryText.append(CAN_VIEW_ANONYMOUS);
		} else {
			queryText.append(CAN_VIEW);
		}
		
		appendPostLikeClause(queryText, search);
		
		queryText.append(ORDER_RECENT);
		
		logger.debug("Full getPostsFor search query is: '" + queryText.toString() + "'");
		
		q = em.createQuery(queryText.toString());
		q.setParameter("poster", poster);
		if (viewer != null)
			q.setParameter("viewer", viewer);
		
		return getPostViews(viewpoint, q, search, start, max);
	}
	
	public List<PostView> getReceivedPosts(Viewpoint viewpoint, Person recipient, String search, int start, int max) {
		// There's an efficiency win here by specializing to the case where
		// viewer == recipient ... we know that posts are always visible
		// to the recipient; we don't bother implementing the other case for
		// now.
		if (!recipient.equals(viewpoint.getViewer()))
			throw new IllegalArgumentException("recipient isn't the viewer");
		
		Query q;
		
		StringBuilder queryText = new StringBuilder("SELECT post FROM Post post " +
		           "WHERE " + VIEWER_RECEIVED);
		
		appendPostLikeClause(queryText, search);

		queryText.append(ORDER_RECENT);
		
		logger.debug("Full getReceivedPosts search query is: '" + queryText.toString() + "'");
		
		q = em.createQuery(queryText.toString());
		
		q.setParameter("viewer", recipient);

		return getPostViews(viewpoint, q, search, start, max);
	}
	
	static final String GET_GROUP_POSTS_QUERY = 
		"SELECT post FROM Post post WHERE :recipient MEMBER OF post.groupRecipients";
	
	public List<PostView> getGroupPosts(Viewpoint viewpoint, Group recipient, String search, int start, int max) {
		Person viewer = viewpoint.getViewer();
		Query q;

		StringBuilder queryText = new StringBuilder(GET_GROUP_POSTS_QUERY + " AND ");
		
		if (viewer == null) {
			queryText.append(CAN_VIEW_ANONYMOUS);
		} else {
			queryText.append(CAN_VIEW);
		}
		
		appendPostLikeClause(queryText, search);
		
		queryText.append(ORDER_RECENT);
		
		q = em.createQuery(queryText.toString());
		
		q.setParameter("recipient", recipient);
		if (viewer != null)
			q.setParameter("viewer", viewer);
		return getPostViews(viewpoint, q, search, start, max);
	}
	
	public List<PostView> getContactPosts(Viewpoint viewpoint, Person person, boolean include_received, int start, int max) {
		if (!person.equals(viewpoint.getViewer()))
			return Collections.emptyList();
		
		if (person instanceof Contact)
			return Collections.emptyList();
		
		User user = (User) person;
		
		Account account = accountSystem.lookupAccountByUser(user); 
		
		assert account != null;
		
		String recipient_clause = include_received ? "" : "NOT " + VIEWER_RECEIVED; 

		Query q;
		q = em.createQuery("SELECT post FROM Account account, Post post " + 
						   "WHERE account = :account AND " +
				               "post.poster MEMBER OF account.contacts AND " +
				                recipient_clause + " AND " +
				                CAN_VIEW + ORDER_RECENT);
		
		q.setParameter("account", account);
		q.setParameter("viewer", user);		
		
		return getPostViews(viewpoint, q, null, start, max);
	}
	
	// FIXME we aren't checking whether viewpoint can see the post...
	public Post loadRawPost(Viewpoint viewpoint, Guid guid) throws NotFoundException {
		Post post = em.find(Post.class, guid.toString());
		if (post == null)
			throw new NotFoundException("Post not found in database: " + guid.toString());
		return post;
	}
	
	public PostView loadPost(Viewpoint viewpoint, Guid guid) throws NotFoundException {
		Post p =  loadRawPost(viewpoint, guid);
		// FIXME access control check here, when used from post framer?
		return getPostView(viewpoint, p);
	}

     public List<PersonPostData> getPostViewers(Viewpoint viewpoint, Guid guid, int max) {
    	 if (viewpoint != null)
    		 throw new IllegalArgumentException("getPostViewers is not implemented for user viewpoints");
    	 
    	 Query q = em.createQuery("SELECT ppd FROM PersonPostData ppd " +
    			 				  "WHERE ppd.post = :postId ORDER BY clickedDate DESC")
		             .setParameter("postId", guid.toString());
    	 
    	 if (max > 0)
    	     q.setMaxResults(max);
    	 
    	 @SuppressWarnings("unchecked")
    	 List<PersonPostData> viewers = q.getResultList();
    	 
    	 return viewers;
    }
	
	public void postViewedBy(String postId, User clicker) {
		logger.debug("Post " + postId + " clicked by " + clicker);
		
		Post post;
		
		Guid postGuid;
		try {
			postGuid = new Guid(postId);
		} catch (ParseException e) {
			throw new RuntimeException("postViewedBy, bad Guid for some reason " + postId, e);
		}
		try {
			post = loadRawPost(new Viewpoint(clicker), postGuid);
		} catch (NotFoundException e) {
			throw new RuntimeException("postViewedBy, nonexistent Guid for some reason " + postId, e);
		}
		
		// Notify the recommender system that a user clicked through, so that ratings can be updated
		recommenderSystem.addRatingForPostViewedBy(post, clicker);
		
		if (!updatePersonPostData(clicker, post))
			return;
				
		// We send out notifications afterwards, so that the new clicker
		// is included in the list of viewers
		@SuppressWarnings("unchecked")
		List<User> viewers = em.createQuery("SELECT ppd.person FROM PersonPostData ppd " +
				   						    "WHERE ppd.post = :post ORDER BY clickedDate DESC")
					   			 .setParameter("post", post)
					   			 .getResultList();
		
		
		messageSender.sendPostClickedNotification(post, viewers, clicker);
		
        LiveState.getInstance().queueUpdate(new PostViewedEvent(postGuid, clicker.getGuid(), new Date()));
	}
	
	/**
	 * Creates a PersonPostData to indicate that the user has viewed the post
	 * if no such object previously existed. Otherwise does nothing 
	 * 
	 * @param user a User
	 * @param post a Post
	 * @return true if the user had not previously viewed the post
	 */
	private boolean updatePersonPostData(final User user, final Post post) {
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<Boolean>() {

				public Boolean call() {
					PersonPostData ppd;
					
					try {
						ppd = (PersonPostData)em.createQuery("SELECT ppd FROM PersonPostData ppd " +
							                                 "WHERE ppd.post = :post AND ppd.person = :user")
				            .setParameter("post", post)
				            .setParameter("user", user)
				            .getSingleResult();
						return false;
					} catch (EntityNotFoundException e) {
						ppd = new PersonPostData(user, post); 
						em.persist(ppd);
						return true;
					}
				}
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return false; // not reached
		}
	}

	public List<PostView> getPostsFor(Viewpoint viewpoint, Person poster, int start, int max) {
		return getPostsFor(viewpoint, poster, null, start, max);
	}

	public List<PostView> getReceivedPosts(Viewpoint viewpoint, Person recipient, int start, int max) {
		return getReceivedPosts(viewpoint, recipient, null, start, max);
	}

	public List<PostView> getGroupPosts(Viewpoint viewpoint, Group recipient, int start, int max) {
		return getGroupPosts(viewpoint, recipient, null, start, max);
	}
	
	public List<PostMessage> getPostMessages(Post post) {
		@SuppressWarnings("unchecked")
		List<PostMessage> messages = em.createQuery("SELECT pm from PostMessage pm WHERE pm.post = :post ORDER BY pm.timestamp")
		.setParameter("post", post)
		.getResultList();
		
		return messages;
	}
	
	public void addPostMessage(Post post, User fromUser, String text, Date timestamp, int serial) {
		if (serial < 0) 
			throw new IllegalArgumentException("Negative serial");
		
		PostMessage postMessage = new PostMessage(post, fromUser, text, timestamp, serial);
		em.persist(postMessage);
	}

	public URL parsePostURL(String urlStr) {
		URL url;		
		try {
			url = new URL(urlStr);
			if (!url.getProtocol().equals("http") || url.getProtocol().equals("https"))
				throw new IllegalArgumentException("invalid protocol in url" + urlStr);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
		return url;
	}
}

