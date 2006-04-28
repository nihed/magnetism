package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.hibernate.lucene.DocumentBuilder;
import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.GroupPostAddedEvent;
import com.dumbhippo.live.LiveGroup;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.PostViewedEvent;
import com.dumbhippo.persistence.Account;
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
import com.dumbhippo.server.Character;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.EntityView;
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
import com.dumbhippo.server.PostSearchResult;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.RecommenderSystem;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.util.GuidNotFoundException;

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
		
		if (post.getVisibility() == PostVisibility.RECIPIENTS_ONLY) {
			for (Resource r : expandedRecipients) {
				messageSender.sendPostNotification(r, post, isTutorialPost);
			}			
		} else if (post.getVisibility() == PostVisibility.ANONYMOUSLY_PUBLIC || 
				   post.getVisibility() == PostVisibility.ATTRIBUTED_PUBLIC) {
			for (Account acct : accountSystem.getRecentlyActiveAccounts()) {
				if (identitySpider.getNotifyPublicShares(acct.getOwner())) {
					messageSender.sendPostNotification(acct, post, isTutorialPost);
				}
			}
			for (Resource r : expandedRecipients) {
				if (!(r instanceof Account)) { // We covered the accounts above
					messageSender.sendPostNotification(r, post, isTutorialPost);
				}
			}
		} else {
			throw new RuntimeException("invalid visibility on post " + post.getId());
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
			logger.debug("Old post does not have an URL: {}", post);
			return original;
		}
		
		logger.debug("Changing URL to '{}'", replacement);
		
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
		
		// sort into persons and groups
		for (GuidPersistable r : recipients) {
			if (r instanceof Person) {
				Person p = (Person) r;
				Resource bestResource = identitySpider.getBestResource(p);
				personRecipients.add(bestResource);
				if (inviteRecipients) {
					// this is smart about doing nothing if the person is already invited
					// or already has an account (it's also very cheap if bestResource is an Account)
					// this does not send out an e-mail to invitee, but prepares an invite
					// to be sent out with the post, if applicable
					invitationSystem.createInvitation(poster, null, bestResource, "", "");
				}
				
			} else if (r instanceof Group) {
				groupRecipients.add((Group) r);
			} else {
				// wtf
				throw new NotFoundException("recipient not found " + r.getId());
			}
		}

		// build expanded recipients
		// FIXME: a recipient will be added to the list twice if they are in
		//    a group with something other than their "best resource"; we
		//    probably should spider the best resource from the group
		//    member.
		expandedRecipients.addAll(personRecipients);
		expandedRecipients.add(identitySpider.getBestResource(poster));
		for (Group g : groupRecipients) {
			for (GroupMember groupMember : g.getMembers()) {
				if (groupMember.isParticipant())
					expandedRecipients.add(groupMember.getMember());
			}
		}
		
		// if this throws we shouldn't send out notifications, so do it first
		Post post = createPost(poster, visibility, title, text, shared, personRecipients, groupRecipients, expandedRecipients, postInfo);
		
		sendPostNotifications(post, expandedRecipients, isTutorialPost);
		
		LiveState liveState = LiveState.getInstance();
		for (Group g : groupRecipients) {
			LiveGroup liveGroup = liveState.peekLiveGroup(g.getGuid());
			if (liveGroup != null) {
		        liveState.queueUpdate(new GroupPostAddedEvent(g.getGuid()));					
			}
		}
		
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

	private void doTutorialPost(User recipient, Character sender, String urlText, String title, String text) {
		logger.debug("Sending tutorial post to {}", recipient);
		User poster = identitySpider.getCharacter(sender);
		URL url;
		try {
			url = new URL(urlText);
		} catch (MalformedURLException e) {
			logger.error("Malformed tutorial url: {}", urlText);
			throw new RuntimeException(e);
		}
		Set<GuidPersistable> recipientSet = Collections.singleton((GuidPersistable)recipient);
		Post post;
		try {
			post = doLinkPostInternal(poster, PostVisibility.RECIPIENTS_ONLY, title, text, url, recipientSet, false, null, true);
		} catch (NotFoundException e) {
			logger.error("Failed to post: {}", e.getMessage());
			throw new RuntimeException(e);
		}
		logger.debug("Tutorial post done: {}", post);
	}
	
	public void doShareLinkTutorialPost(User recipient) {
		doTutorialPost(recipient, Character.LOVES_ACTIVITY, 
				configuration.getProperty(HippoProperty.BASEURL) + "/account",
				"What is this DumbHippo thing?",
				"Set up your account and learn to use DumbHippo by visiting this link");
	}

	public void doNowPlayingTutorialPost(User recipient) {
		doTutorialPost(recipient, Character.MUSIC_GEEK, 
				configuration.getProperty(HippoProperty.BASEURL) + "/nowplaying",
				"Put your music online",
				"Visit this link to learn how to put your music on your blog or MySpace page");
	}
	
	private Post createPost(final User poster, final PostVisibility visibility, final String title, final String text, final Set<Resource> resources, 
			               final Set<Resource> personRecipients, final Set<Group> groupRecipients, final Set<Resource> expandedRecipients, final PostInfo postInfo) {
		try {
			Post detached = runner.runTaskInNewTransaction(new Callable<Post>() {
				public Post call() {
					Post post = new Post(poster, visibility, title, text, personRecipients, groupRecipients, expandedRecipients, resources);
					post.setPostInfo(postInfo);
					em.persist(post);
					logger.debug("saved new Post {}", post);
					return post;
				}
			});
			Post post = em.find(Post.class, detached.getId());
			
			// Add the new post to the Hibernate index; it would likely be better to 
			// do this asynchronously
			indexPost(post);
			
			return post;
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
	}

	private PersonPostData getPersonPostData(UserViewpoint viewpoint, Post post) {
		Query q = em.createQuery("SELECT ppd FROM PersonPostData ppd " +
				                 "WHERE ppd.post = :post AND ppd.person = :viewer");
		q.setParameter("post", post);
		q.setParameter("viewer", viewpoint.getViewer());
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
	
	static final String VISIBLE_GROUP_SYSTEM =
        "g MEMBER OF post.groupRecipients";
	
	static final String AUTH_GROUP_ANONYMOUS =
        "g MEMBER OF post.groupRecipients AND " + 
		"g.access >= " + GroupAccess.PUBLIC.ordinal();
	
	static final String CAN_VIEW = 
		" (post.visibility = " + PostVisibility.ATTRIBUTED_PUBLIC.ordinal() + " OR " +
			  "post.poster = :viewer OR " + 
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
		Query q;

		if (viewpoint instanceof SystemViewpoint) {
			q = em.createQuery(GET_POST_VIEW_QUERY_ANONYMOUS + " AND " + VISIBLE_GROUP_SYSTEM);
		} else if (viewpoint instanceof UserViewpoint) {
			User viewer = ((UserViewpoint)viewpoint).getViewer();

			q = em.createQuery(GET_POST_VIEW_QUERY + " AND " + VISIBLE_GROUP_FOR_POST);
			q.setParameter("viewer", viewer);
		} else {
			q = em.createQuery(GET_POST_VIEW_QUERY_ANONYMOUS + " AND " + VISIBLE_GROUP_ANONYMOUS);
		}

		q.setParameter("post", post);
		
		if (viewpoint instanceof UserViewpoint) {
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
		} else {
			for (Object o : q.getResultList()) {
				recipients.add(new GroupView((Group)o, null, null));
			}			
		}
	}

	static final String VISIBLE_PERSON_RECIPIENTS_QUERY = 
		"SELECT resource from Post post, Resource resource " +
		"WHERE post = :post AND " +
		"      (post.poster = :viewer OR " +
		"       EXISTS(SELECT ac from AccountClaim ac WHERE ac.owner = :viewer AND ac.resource MEMBER OF post.personRecipients)) AND " +
		"      resource MEMBER OF post.personRecipients";
	
	private List<Resource> getVisiblePersonRecipients(UserViewpoint viewpoint, Post post) {
		@SuppressWarnings("unchecked")
		List<Resource> results = em.createQuery(VISIBLE_PERSON_RECIPIENTS_QUERY)
			.setParameter("post", post)
			.setParameter("viewer", viewpoint.getViewer())
			.getResultList();
		
		return results;
	}
	
	// This doesn't check access to the Post, since that was supposed to be done
	// when loading the post... conceivably we should redo the API to eliminate 
	// that danger
	public PostView getPostView(Viewpoint viewpoint, Post post) {
		List<Object> recipients = new ArrayList<Object>();
		
		addGroupRecipients(viewpoint, post, recipients);
		
		Collection<Resource> recipientResources;
		if (viewpoint instanceof SystemViewpoint) {
			recipientResources = post.getPersonRecipients();
		} else if (viewpoint instanceof UserViewpoint) {
			recipientResources = getVisiblePersonRecipients((UserViewpoint)viewpoint, post);
		} else {
			recipientResources = Collections.emptyList();
		}
		
		for (Resource recipient : recipientResources) {
			recipients.add(identitySpider.getPersonView(viewpoint, recipient, PersonViewExtra.PRIMARY_RESOURCE, PersonViewExtra.PRIMARY_AIM));
		}
	
		if (!em.contains(post))
			throw new RuntimeException("can't update post info if Post is not attached");
		
		// Ensure we're updated (potentially blocks for a while)
		infoSystem.updatePostInfo(post);
		
		/*
		String info = post.getInfo();
		if (info != null)
			logger.debug("Updated, post info now: {}", info.replace("\n",""));
		else
			logger.debug("Updated, post info now null");
		*/
		
		try {
			PersonPostData ppd;
			if (viewpoint instanceof UserViewpoint)
				ppd = getPersonPostData((UserViewpoint)viewpoint, post);
			else
				ppd = null;
			
			return new PostView(ejbContext, post, 
					identitySpider.getPersonView(viewpoint, post.getPoster(), PersonViewExtra.ALL_RESOURCES),
					ppd,
					recipients,
					viewpoint);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private List<PostView> getPostViews(Viewpoint viewpoint, Query q, String search, int start, int max) {
		if (max > 0)
			q.setMaxResults(max);
		
		q.setFirstResult(start);
		
		List<Post> posts = TypeUtils.castList(Post.class, q.getResultList());	

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
	public boolean canViewPost(UserViewpoint viewpoint, Post post) {
		User viewer = viewpoint.getViewer();
		
		/* This is an optimization for a common case */
		if (post.getPoster().equals(viewer))
			return true;
		
		/* FIXME we should probably do this by looking at the Post 
		 * in memory, not with a query
		 */
		
		/* Do the query if no optimizations apply */
		Query q;
		StringBuilder queryText = new StringBuilder(GET_SPECIFIC_POST_QUERY + " AND ");
		queryText.append(CAN_VIEW);
		//logger.debug("Full canViewPost query is: '{}'", queryText);
		
		q = em.createQuery(queryText.toString());
		q.setParameter("post", post);
		q.setParameter("viewer", viewer);
		
		try {
			/*Post resultPost = (Post) */em.createQuery(queryText.toString())
	            .setParameter("post", post)
	            .setParameter("viewer", viewer)
	            .getSingleResult();
			//logger.debug("canViewPost query got one result: {}; returning true/access granted", resultPost);
			return true;
		} catch (EntityNotFoundException e) {
			//logger.debug("canViewPost query got no result; returning false/access denied");
			return false;
		}
	}

	public boolean canViewPost(Viewpoint viewpoint, Post post) {
		if (viewpoint instanceof SystemViewpoint) {
			return true;
		} else if (viewpoint instanceof UserViewpoint) {
			return canViewPost((UserViewpoint) viewpoint, post);
		} else {
			return post.getVisibility() == PostVisibility.ATTRIBUTED_PUBLIC;
		}
	}
	
	private Query buildGetPostsForQuery(Viewpoint viewpoint, Person poster, String search, boolean isCount) {
		User viewer = null;
		Query q;
		
		StringBuilder queryText = new StringBuilder("SELECT ");
		if (isCount)
			queryText.append("count(post)");
		else
			queryText.append("post");
		queryText.append(" FROM Post post WHERE post.poster = :poster ");
		if (viewpoint instanceof SystemViewpoint) {
			// No access-control clause
		} else if (viewpoint instanceof UserViewpoint) {
			viewer = ((UserViewpoint)viewpoint).getViewer();
			queryText.append(" AND ");
			queryText.append(CAN_VIEW);
		} else {
			queryText.append(" AND ");			
			queryText.append(CAN_VIEW_ANONYMOUS);
		}
		
		appendPostLikeClause(queryText, search);
		
		if (!isCount)
			queryText.append(ORDER_RECENT);
		
		//logger.debug("Full getPostsFor search query is: '{}'", queryText);
		
		q = em.createQuery(queryText.toString());
		q.setParameter("poster", poster);
		if (viewer != null)
			q.setParameter("viewer", viewer);
		return q;
	}
	
	public int getPostsForCount(Viewpoint viewpoint, Person forPerson, String search) {
		Query q = buildGetPostsForQuery(viewpoint, forPerson, search, true);
		Object result = q.getSingleResult();
		return ((Number) result).intValue();	
	}

	
	public List<PostView> getPostsFor(Viewpoint viewpoint, Person forPerson, String search, int start, int max) {
		Query q = buildGetPostsForQuery(viewpoint, forPerson, search, false);
		return getPostViews(viewpoint, q, search, start, max);
	}

	private Query buildReceivedPostsQuery(UserViewpoint viewpoint, User recipient, String search, boolean isCount) {
		// There's an efficiency win here by specializing to the case where
		// viewer == recipient ... we know that posts are always visible
		// to the recipient; we don't bother implementing the other case for
		// now.
		if (!viewpoint.isOfUser(recipient))
			throw new IllegalArgumentException("recipient isn't the viewer");
		
		Query q;
		
		StringBuilder queryText = new StringBuilder("SELECT ");
		if (isCount)
			queryText.append("count(post)");
		else
			queryText.append("post");
		queryText.append(" FROM Post post WHERE " + VIEWER_RECEIVED);
		
		appendPostLikeClause(queryText, search);

		if (!isCount)
			queryText.append(ORDER_RECENT);
		
		//logger.debug("Full getReceivedPosts search query is: '{}'", queryText);
		
		q = em.createQuery(queryText.toString());
		
		q.setParameter("viewer", recipient);
		return q;
	}
	
	public int getReceivedPostsCount(UserViewpoint viewpoint, User recipient, String search) {
		Query q = buildReceivedPostsQuery(viewpoint, recipient, search, true);
		Object result = q.getSingleResult();
		return ((Number) result).intValue();
	}
	
	public List<PostView> getReceivedPosts(UserViewpoint viewpoint, User recipient, String search, int start, int max) {
		Query q  = buildReceivedPostsQuery(viewpoint, recipient, search, false);
		return getPostViews(viewpoint, q, search, start, max);
	}

	private Query buildGetGroupPostsQuery(Viewpoint viewpoint, Group recipient, String search, boolean isCount) {
		User viewer = null;
		Query q;

		StringBuilder queryText = new StringBuilder("SELECT ");
		if (isCount)
			queryText.append("count(post)");
		else
			queryText.append("post");
		queryText.append(" FROM Post post WHERE :recipient MEMBER OF post.groupRecipients ");

		if (viewpoint instanceof SystemViewpoint) {
		    // No access control clause
		} else if (viewpoint instanceof UserViewpoint) {
			viewer = ((UserViewpoint)viewpoint).getViewer();
			queryText.append(" AND ");
			queryText.append(CAN_VIEW);
		} else {
			queryText.append(" AND ");			
			queryText.append(CAN_VIEW_ANONYMOUS);
		}
		
		appendPostLikeClause(queryText, search);
		
		if (!isCount)
			queryText.append(ORDER_RECENT);
		
		q = em.createQuery(queryText.toString());
		
		q.setParameter("recipient", recipient);
		if (viewer != null)
			q.setParameter("viewer", viewer);
		return q;
	}
	
	public int getGroupPostsCount(Viewpoint viewpoint, Group recipient, String search) {
		Query q = buildGetGroupPostsQuery(viewpoint, recipient, search, true);
		Object result = q.getSingleResult();
		return ((Number) result).intValue();		
	}
	
	public List<PostView> getGroupPosts(Viewpoint viewpoint, Group recipient, String search, int start, int max) {
		Query q = buildGetGroupPostsQuery(viewpoint, recipient, search, false);
		return getPostViews(viewpoint, q, search, start, max);
	}

	public List<PostView> getContactPosts(UserViewpoint viewpoint, User user, boolean include_received, int start, int max) {
		if (!viewpoint.isOfUser(user))
			return Collections.emptyList();
		
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
	
	public List<PostView> getFavoritePosts(Viewpoint viewpoint, User user, int start, int maxResults) {
		
		/*
		 // if only friends could see the faves list...
		 // right now though, anyone can see it, if they can see the posts 
		if (!identitySpider.isViewerSystemOrFriendOf(viewpoint, user))
			return Collections.emptyList();
		*/
		
		Account account = accountSystem.lookupAccountByUser(user);
		Set<Post> faves = account.getFavoritePosts();
		List<Post> dateSorted = new ArrayList<Post>();
		
		for (Post p : faves) {
			if (canViewPost(viewpoint, p))
				dateSorted.add(p);
		}
		
		Collections.sort(dateSorted, new Comparator<Post>() {

			public int compare(Post a, Post b) {
				long aDate = a.getPostDate().getTime();
				long bDate = b.getPostDate().getTime();
				if (aDate < bDate)
					return -1;
				else if (aDate > bDate)
					return 1;
				else
					return 0;
			}
			
		});
		
		List<PostView> views = new ArrayList<PostView>();
		int i = Math.min(dateSorted.size(), start);
		int count = Math.min(dateSorted.size() - start, maxResults);
		while (count > 0) {
			views.add(getPostView(viewpoint, dateSorted.get(i)));
			--count;
			++i;
		}
		return views;
	}
	
	public Post loadRawPost(Viewpoint viewpoint, Guid guid) throws NotFoundException {
		Post post = EJBUtil.lookupGuid(em, Post.class, guid);
		if (canViewPost(viewpoint, post))
			return post;
		else
			throw new GuidNotFoundException(guid);
	}
	
	public PostView loadPost(Viewpoint viewpoint, Guid guid) throws NotFoundException {
		// loadRawPost is supposed to do the permissions check
		Post p =  loadRawPost(viewpoint, guid);
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
    
	public int getPostViewerCount(Guid guid) {	
		Query q = em.createQuery("SELECT COUNT(ppd) FROM PersonPostData ppd WHERE ppd.post = :postId");
		q.setParameter("postId", guid.toString());
		Number count = (Number) q.getSingleResult();
		return count.intValue();
	}    
	
	public void postViewedBy(String postId, User clicker) {
		logger.debug("Post {} clicked by {}", postId, clicker);
		
		Post post;
		
		Guid postGuid;
		try {
			postGuid = new Guid(postId);
		} catch (ParseException e) {
			throw new RuntimeException("postViewedBy, bad Guid for some reason " + postId, e);
		}
		try {
			post = loadRawPost(new UserViewpoint(clicker), postGuid);
		} catch (NotFoundException e) {
			throw new RuntimeException("postViewedBy, nonexistent Guid for some reason " + postId, e);
		}
		
		// Notify the recommender system that a user clicked through, so that ratings can be updated
		recommenderSystem.addRatingForPostViewedBy(post, clicker);
		
		if (!updatePersonPostData(clicker, post))
			return;
	
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

	public int getPostsForCount(Viewpoint viewpoint, Person forPerson) {
		return getPostsForCount(viewpoint, forPerson, null);
	}	
	
	public List<PostView> getPostsFor(Viewpoint viewpoint, Person poster, int start, int max) {
		return getPostsFor(viewpoint, poster, null, start, max);
	}

	public int getReceivedPostsCount(UserViewpoint viewpoint, User recipient) {
		return getReceivedPostsCount(viewpoint, recipient, null);
	}
	
	public List<PostView> getReceivedPosts(UserViewpoint viewpoint, User recipient, int start, int max) {
		return getReceivedPosts(viewpoint, recipient, null, start, max);
	}

	public int getGroupPostsCount(Viewpoint viewpoint, Group recipient) {
		return getGroupPostsCount(viewpoint, recipient, null);
	}
	
	public List<PostView> getGroupPosts(Viewpoint viewpoint, Group recipient, int start, int max) {
		return getGroupPosts(viewpoint, recipient, null, start, max);
	}
	
	static final String GET_HOT_POSTS_QUERY = 
		"SELECT post FROM Post post WHERE ";

	public List<PostView> getHotPosts(Viewpoint viewpoint, int start, int max) {
		User viewer = null;
		Query q;

		StringBuilder queryText = new StringBuilder(GET_HOT_POSTS_QUERY);

		if (viewpoint instanceof SystemViewpoint) {
		    // No access control clause
		} else if (viewpoint instanceof UserViewpoint) {
			viewer = ((UserViewpoint)viewpoint).getViewer();
			queryText.append(CAN_VIEW);
		} else {
			queryText.append(CAN_VIEW_ANONYMOUS);
		}
		
		queryText.append(ORDER_RECENT);
		
		q = em.createQuery(queryText.toString());
		
		if (viewer != null)
			q.setParameter("viewer", viewer);
		return getPostViews(viewpoint, q, null, start, max);
	}
	
	private static final String POST_MESSAGE_QUERY = "SELECT pm from PostMessage pm WHERE pm.post = :post";
	private static final String POST_MESSAGE_RECENT = " and (pm.timestamp - current_timestamp()) < :recentTime";
	private static final String POST_MESSAGE_ORDER = " ORDER BY pm.timestamp";
	
	public List<PostMessage> getPostMessages(Post post) {
		@SuppressWarnings("unchecked")
		List<PostMessage> messages = em.createQuery(POST_MESSAGE_QUERY + POST_MESSAGE_ORDER)
		.setParameter("post", post)
		.getResultList();
		
		return messages;
	}
	
	public List<PostMessage> getRecentPostMessages(Post post, int seconds) {
		List<?> messages = em.createQuery(POST_MESSAGE_QUERY + POST_MESSAGE_RECENT +
													POST_MESSAGE_ORDER)
		.setParameter("post", post)
		.setParameter("recentTime", seconds)
		.getResultList();		
		return TypeUtils.castList(PostMessage.class, messages);
	}	
	
	public void addPostMessage(Post post, User fromUser, String text, Date timestamp, int serial) {
		// we use serial = -1 in other places in the system to designate a message that contains
		// the post description, but we never add this type of message to the database
		if (serial < 0) 
			throw new IllegalArgumentException("Negative serial");
		
		PostMessage postMessage = new PostMessage(post, fromUser, text, timestamp, serial);
		em.persist(postMessage);
	}

	public URL parsePostURL(String urlStr) {
		URL url;		
		try {
			url = new URL(urlStr);
			if (!(url.getProtocol().equals("http") || url.getProtocol().equals("https")))
				throw new IllegalArgumentException("Invalid protocol in url " + urlStr);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
		return url;
	}

	public Set<EntityView> getReferencedEntities(Viewpoint viewpoint, Post post) {
		Set<EntityView> result = new HashSet<EntityView>();
		for (Group g : post.getGroupRecipients()) {
			// FIXME for now we always add anonymous group views, since at the moment
			// all the callers of this function just need anonymously viewable
			// information.  Later, if for example we add the viewer's membership status
			// to the toXml() method of GroupView, we should fix this function, probably 
			// by adding a GroupSystem.getGroupView(viewpoint, g).
			result.add(new GroupView(g, null, null));
		}
		for (Resource r : post.getPersonRecipients()) {
			result.add(identitySpider.getPersonView(viewpoint, r, PersonViewExtra.PRIMARY_RESOURCE));	
		}
		result.add(identitySpider.getPersonView(viewpoint, post.getPoster(), PersonViewExtra.PRIMARY_RESOURCE));
		return result;
	}

	private void indexPosts(List<Post> posts, boolean create) {
		try {
			// FIXME: StopAnalyzer is quite crude; it doesn't do any stemming, for example
						
			// FIXME: Any changes to posts that run while this is going on
			// will end up throwing an IOException because the will time
			// out after a second of trying to getting the IndexWriter
			// lock. The right setup is likely a thread that runs and
			// does all indexing asynchronously, and can properly schedule
			// a reindex without creating multiple IndexWriter() objects.

			// FIXME: passing create=true to new IndexWriter creates a
			// new index replacing anything existing. We need to check
			// to make sure tha this doesn't break searches that go
			// on concurrently. Searching on the old index or on the new
			// index would be fine, but crashes isn't.
			//
			// Note that this method of recreating the index is not safe if posts 
			// could be deleted from the database, since the incremental update 
			// would delete them, then we'd add them back. However, we never
			// currently delete posts. A single thread would also fix this
			// by serializing access. Eventually we might want to do the
			// indexing on a separate *machine* for the entire cluster, with
			// the index exported by NFS or similar. (NFS locks, scary!)
			
			DocumentBuilder<Post> builder = new DocumentBuilder<Post>(Post.class);
			IndexWriter writer = new IndexWriter(builder.getFile(), new StopAnalyzer(), create);
			
			for (Post post : posts) {
				Document document = builder.getDocument(post, post.getId());
				writer.addDocument(document);
			}
			writer.close();
		} catch (IOException e) {
		}
		
	}
	
	private void indexPost(Post post) {
		indexPosts(Collections.singletonList(post), false);
	}
	
	public void reindexAllPosts() {
		List<?> l = em.createQuery("SELECT p FROM Post p").getResultList();
		indexPosts(TypeUtils.castList(Post.class, l), true);
	}
	
	public PostSearchResult searchPosts(Viewpoint viewpoint, String queryString) {
		final String[] fields = { "ExplicitTitle", "Text" };
		// See comment on StopAnalyzer above
		QueryParser queryParser = new MultiFieldQueryParser(fields, new StopAnalyzer());
		queryParser.setDefaultOperator(Operator.AND);
		org.apache.lucene.search.Query query;
		try {
			query = queryParser.parse(queryString);
			
			// FIXME: We should reuse one IndexReader, which is thread safe, rather
			// than repeatedly creating new ones.
			IndexReader reader = IndexReader.open(new DocumentBuilder<Post>(Post.class).getFile());
			Searcher searcher = new IndexSearcher(reader);
			Hits hits = searcher.search(query);
			
			return new PostSearchResult(hits);
			
		} catch (org.apache.lucene.queryParser.ParseException e) {
			return new PostSearchResult("Can't parse query '" + queryString + "'");
		} catch (IOException e) {
			return new PostSearchResult("System error while searching, please try again");
		}
	}
	
	public List<PostView> getPostSearchPosts(Viewpoint viewpoint, PostSearchResult searchResult, int start, int count) {
		// The efficiency gain of having this wrapper is that we pass the real 
		// object to the method rather than the proxy; getPosts() can make many, 
		// many calls back against the PostingBoard.
		return searchResult.getPosts(this, viewpoint, start, count);
	}
	
	public void setFavoritePost(UserViewpoint viewpoint, Post post, boolean favorite) {
		Account account = viewpoint.getViewer().getAccount();
		if (favorite)
			account.addFavoritePost(post);
		else
			account.removeFavoritePost(post);
	}
}
