package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PostInfoSystem;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class PostingBoardBean implements PostingBoard {

	private static final Log logger = GlobalSetup.getLog(PostingBoardBean.class);	
	
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
	
	@javax.annotation.Resource
	private EJBContext ejbContext;
	
	private void sendPostNotifications(Post post, Set<Resource> expandedRecipients) {
		// FIXME I suspect this should be outside the transaction and asynchronous
		logger.debug("Sending out jabber/email notifications...");
		for (Resource r : expandedRecipients) {
			messageSender.sendPostNotification(r, post);
		}
	}
	
	public Post doLinkPost(User poster, PostVisibility visibility, String title, String text, String url, Set<GuidPersistable> recipients, boolean inviteRecipients) throws GuidNotFoundException {
		Set<Resource> shared = (Collections.singleton((Resource) identitySpider.getLink(url)));
		
		// for each recipient, if it's a group we want to explode it into persons
		// (but also keep the group itself), if it's a person we just add it
		
		Set<Resource> personRecipients = new HashSet<Resource>();
		Set<Group> groupRecipients = new HashSet<Group>();
		Set<Resource> expandedRecipients = new HashSet<Resource>();
		
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
			} else if (r instanceof Group) {
				groupRecipients.add((Group) r);
			} else {
				// wtf
				throw new GuidNotFoundException(r.getId());
			}
		}
		
		// build expanded recipients
		expandedRecipients.addAll(personRecipients);
		for (Group g : groupRecipients) {
			for (GroupMember groupMember : g.getMembers()) {
				if (groupMember.isParticipant())
					expandedRecipients.add(groupMember.getMember());
			}
		}
		
		// if this throws we shouldn't send out notifications, so do it first
		Post post = createPostViaProxy(poster, visibility, title, text, shared, personRecipients, groupRecipients, expandedRecipients);
		
		sendPostNotifications(post, expandedRecipients);
		return post;
	}
	
	public void doShareLinkTutorialPost(Person recipient) {

		User poster = identitySpider.getTheMan();
		LinkResource link = identitySpider.getLink(configuration.getProperty(HippoProperty.BASEURL) + "/account");
		Set<Group> emptyGroups = Collections.emptySet();
		Set<Resource> recipientSet = Collections.singleton(identitySpider.getBestResource(recipient));

		Post post = createPostViaProxy(poster, PostVisibility.RECIPIENTS_ONLY, "What is this DumbHippo thing?",
				"Set up your account and learn to use DumbHippo by visiting this link", Collections.singleton((Resource) link), recipientSet, emptyGroups, recipientSet);

		sendPostNotifications(post, recipientSet);
	}
	
	private Post createPostViaProxy(User poster, PostVisibility visibility, String title, String text, Set<Resource> resources, Set<Resource> personRecipients, Set<Group> groupRecipients, Set<Resource> expandedRecipients) {
		PostingBoard proxy = (PostingBoard) ejbContext.lookup(PostingBoard.class.getCanonicalName());
		
		return proxy.createPost(poster, visibility, title, text, resources, personRecipients, groupRecipients, expandedRecipients);
	}
	
	// internal function that is public only because of TransactionAttribute; use createPostViaProxy
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Post createPost(User poster, PostVisibility visibility, String title, String text, Set<Resource> resources, Set<Resource> personRecipients, Set<Group> groupRecipients, Set<Resource> expandedRecipients) {
		
		logger.debug("saving new Post");
		Post post = new Post(poster, visibility, title, text, personRecipients, groupRecipients, expandedRecipients, resources);
		em.persist(post);
	
		return post;
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
			recipients.add(identitySpider.getPersonView(viewpoint, recipient));
	
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
			return new PostView(post, 
					identitySpider.getPersonView(viewpoint, post.getPoster(), PersonViewExtra.ALL_RESOURCES),
					getPersonPostData(viewpoint, post),
					recipients);
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
	
	public Post loadRawPost(Viewpoint viewpoint, Guid guid) {
		return em.find(Post.class, guid.toString());
	}
	
	public PostView loadPost(Viewpoint viewpoint, Guid guid) {
		Post p =  em.find(Post.class, guid.toString());
		// FIXME access control check here, when used from post framer?
		return getPostView(viewpoint, p);
	}

	public void postViewedBy(String postId, User clicker) {
		logger.debug("Post " + postId + " clicked by " + clicker);
		
		Post post;
		
		try {
			Guid postGuid = new Guid(postId);
			post = loadRawPost(new Viewpoint(clicker), postGuid);
		} catch (Guid.ParseException e) {
			throw new RuntimeException(e);
		}
			
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
	}
	
	/**
	 * Creates a PersonPostData to indicate that the user has viewed the post
	 * if no such object previously existed. Otherwise does nothing 
	 * 
	 * @param user a User
	 * @param post a Post
	 * @return true if the user had not previously viewed the post
	 */
	private boolean updatePersonPostData(User user, Post post) {
		PostingBoard proxy = (PostingBoard) ejbContext.lookup(PostingBoard.class.getCanonicalName());
		int retries = 1;
		
		while (true) {
			try {
				return proxy.updatePersonPostDataInternal(user, post);
			} catch (Exception e) {
				if (retries > 0 && EJBUtil.isDuplicateException(e)) {
					logger.debug("Race condition creating PersonPostData, retrying");
					retries--;
				} else {
					logger.error("Couldn't create PersonPostData resource", e);					
					throw new RuntimeException("Unexpected error creating PersonPostData", e);
				}
			}
		}		
		
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean updatePersonPostDataInternal(User user, Post post) {
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

	public List<PostView> getPostsFor(Viewpoint viewpoint, Person poster, int start, int max) {
		return getPostsFor(viewpoint, poster, null, start, max);
	}

	public List<PostView> getReceivedPosts(Viewpoint viewpoint, Person recipient, int start, int max) {
		return getReceivedPosts(viewpoint, recipient, null, start, max);
	}

	public List<PostView> getGroupPosts(Viewpoint viewpoint, Group recipient, int start, int max) {
		return getGroupPosts(viewpoint, recipient, null, start, max);
	}
}

