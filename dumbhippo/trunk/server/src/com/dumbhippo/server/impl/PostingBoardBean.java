package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.lucene.search.Hits;
import org.jboss.annotation.IgnoreDependency;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.GroupEvent;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.PostCreatedEvent;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.FeedPost;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupFeed;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.postinfo.NodeName;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.postinfo.PostInfoType;
import com.dumbhippo.postinfo.ShareGroupPostInfo;
import com.dumbhippo.search.SearchSystem;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.CreateInvitationResult;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PostInfoSystem;
import com.dumbhippo.server.PostSearchResult;
import com.dumbhippo.server.PostType;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.RecommenderSystem;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.blocks.PostBlockHandler;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.util.GuidNotFoundException;
import com.dumbhippo.server.views.AnonymousViewpoint;
import com.dumbhippo.server.views.EntityView;
import com.dumbhippo.server.views.FeedView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class PostingBoardBean implements PostingBoard {

	private static final Logger logger = GlobalSetup.getLogger(PostingBoardBean.class);	
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PersonViewer personViewer;

	@EJB
	private AccountSystem accountSystem;

	@EJB
	private MessageSender messageSender;

	@EJB
	private Configuration configuration;
	
	@EJB
	private PostInfoSystem infoSystem;
	
	@EJB
	@IgnoreDependency
	private PostBlockHandler postBlockHandler;
	
	@EJB
	private InvitationSystem invitationSystem;
	
	@EJB
	private GroupSystem groupSystem;
	
	@EJB
	@IgnoreDependency
	private SearchSystem searchSystem;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	@IgnoreDependency
	private RecommenderSystem recommenderSystem;
	
	@EJB
	private Notifier notifier;
	
	@javax.annotation.Resource
	private EJBContext ejbContext;
	
	public boolean isAddressedRecipient(Post post, Resource resource) {
		return post.getExpandedRecipients().contains(resource); 
	}
	
	public Set<Resource> getPostRecipients(Post post) {
		return post.getExpandedRecipients();  
	}
	
	public void sendPostNotifications(Post post, PostType postType) {
		logger.debug("Sending out email notifications...");
		
		for (Resource r : post.getExpandedRecipients()) {
			messageSender.sendPostNotification(r, post, postType);
		}
		logger.debug("Sending out email notifications...done");		
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
	
	private void postPost(final Post post, final PostType postType) {
		// Update recommender synchronously; otherwise two things
		// could try to create a recommendation simultaneously
		// and cause a constraint violation
		final User poster = post.getPoster();
		if (poster != null) {
			// tell the recommender engine, so ratings can be updated
			recommenderSystem.addRatingForPostCreatedBy(post, poster);
		}			
		runner.runTaskOnTransactionCommit(new Runnable() {
			public void run() {
				// FIXME this should really NOT be in a transaction, we don't want to hold
				// a transaction open while sending out the notifications. But currently 
				// too lazy to test the below for "detached safety" (the issue is if 
				// sendPostNotifications or post.getGroupRecipients() require an attached
				// post)
				runner.runTaskInNewTransaction(new Runnable() {
					public void run() {
						PostingBoard board = EJBUtil.defaultLookup(PostingBoard.class);
						Post currentPost;
						try {
							currentPost = board.loadRawPost(SystemViewpoint.getInstance(), post.getGuid());
						} catch (NotFoundException e) {
							throw new RuntimeException(e);
						}
												
						// Sends out XMPP notification
						board.sendPostNotifications(currentPost, postType);
						
						LiveState liveState = LiveState.getInstance();			
						for (Group g : post.getGroupRecipients()) {
						    liveState.queueUpdate(new GroupEvent(g.getGuid(), post.getGuid(), GroupEvent.Detail.POST_ADDED));
						}
						liveState.queueUpdate(new PostCreatedEvent(post.getGuid(), poster != null ? 
								poster.getGuid() : null));				
					}
				});
			}
		});
	}
	
	private PostVisibility expandVisibilityForGroup(PostVisibility visibility, Group group) {
		if (visibility == PostVisibility.RECIPIENTS_ONLY &&
				(group.getAccess() == GroupAccess.PUBLIC_INVITE ||
				 group.getAccess() == GroupAccess.PUBLIC)) {
			return PostVisibility.ATTRIBUTED_PUBLIC;
		} else {
			return visibility;
		}
	}
	
	private Post doLinkPostInternal(User poster, boolean toWorld, String title, String text, URL url, Set<GuidPersistable> recipients, InviteRecipients inviteRecipients, PostInfo postInfo, PostType postType) throws NotFoundException {
		
		PostVisibility visibility;
		
		// if we want to explicitly send to "the world" then force public even if not to any public groups
		visibility = toWorld ? PostVisibility.ATTRIBUTED_PUBLIC : PostVisibility.RECIPIENTS_ONLY;
		
		url = removeFrameset(url);
		
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
				boolean send = true;
				if (inviteRecipients != InviteRecipients.DONT_INVITE) {
					// this is smart about doing nothing if the person is already invited
					// or already has an account (it's also very cheap if bestResource is an Account)
					// this does not send out an e-mail to invitee, but prepares an invite
					// to be sent out with the post, if applicable
					Pair<CreateInvitationResult,InvitationToken> result = invitationSystem.createInvitation(poster, null, bestResource, "", "");
					if (result.getFirst() == CreateInvitationResult.INVITE_WAS_NOT_CREATED && inviteRecipients == InviteRecipients.MUST_INVITE) {
						// It probably would be better to throw an exception and propagate that
						// back into a nice message to the user, but just avoiding sending the
						// invitation is better than nothing.
						logger.debug("Skipping sending share to {} because we couldn't create an invitation", r);
						send = false;
					}
				}
				if (send)
					personRecipients.add(bestResource);
				
			} else if (r instanceof Group) {
				groupRecipients.add((Group) r);
			} else {
				// wtf
				throw new NotFoundException("recipient not found " + r.getId());
			}
		}

		// build expanded recipients - note this logic is copied in FeedPost.makeExpandedRecipients,
		// keep them in sync
		expandedRecipients.addAll(personRecipients);
		expandedRecipients.add(identitySpider.getBestResource(poster));
		for (Group g : groupRecipients) {
	
			// If you copy a public group, the post is forced public
			visibility = expandVisibilityForGroup(visibility, g);
			
			for (GroupMember groupMember : g.getMembers()) {
				if (groupMember.getStatus().getReceivesPosts())
					expandedRecipients.add(groupMember.getMember());
			}
		}
		
		// if this throws we shouldn't send out notifications, so do it first
		Post post = createPost(poster, visibility, toWorld, title, text, shared, personRecipients, groupRecipients, expandedRecipients, postInfo);
		
		postPost(post, postType);
		
		return post;		
	}
	
	public Post doLinkPost(User poster, boolean isPublic, String title, String text, URL url, Set<GuidPersistable> recipients, InviteRecipients inviteRecipients, PostInfo postInfo) throws NotFoundException {
		return doLinkPostInternal(poster, isPublic, title, text, url, recipients, inviteRecipients, postInfo, PostType.NORMAL);
	}
	
	public Post doShareGroupPost(User poster, Group group, String title, String text, Set<GuidPersistable> recipients, InviteRecipients inviteRecipients)
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
		
		if (title == null)
			title = group.getName();
					
		ShareGroupPostInfo postInfo = PostInfo.newInstance(PostInfoType.SHARE_GROUP, ShareGroupPostInfo.class);
		postInfo.getTree().updateContentChild(group.getId(), NodeName.shareGroup, NodeName.groupId);
		postInfo.makeImmutable();

		return doLinkPostInternal(poster, false, title, text, url, recipients, inviteRecipients, postInfo, PostType.GROUP);		
	}

	private void doTutorialPost(User recipient, Character sender, String urlText, String title, String text) {
		logger.debug("Sending tutorial post to {}", recipient);
		User poster = accountSystem.getCharacter(sender);
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
			post = doLinkPostInternal(poster, false, title, text, url, recipientSet, InviteRecipients.MUST_INVITE, null, PostType.TUTORIAL);
		} catch (NotFoundException e) {
			logger.error("Failed to post: {}", e.getMessage());
			throw new RuntimeException(e);
		}
		logger.debug("Tutorial post done: {}", post);
	}
	
	public void doShareLinkTutorialPost(User recipient) {
		doTutorialPost(recipient, Character.LOVES_ACTIVITY, 
				configuration.getProperty(HippoProperty.BASEURL) + "/account",
				"What is this Mugshot thing?",
				"Set up your account and learn to use Mugshot by visiting this link");
	}

	public void doNowPlayingTutorialPost(User recipient) {
		doTutorialPost(recipient, Character.MUSIC_GEEK, 
				configuration.getProperty(HippoProperty.BASEURL) + "/nowplaying",
				"Put your music online",
				"Visit this link to learn how to put your music on your blog or MySpace page");
	}
	

	public void doGroupInvitationPost(User recipient, Group group) {
		doTutorialPost(recipient, Character.LOVES_ACTIVITY, 
				configuration.getProperty(HippoProperty.BASEURL) + "/group?who=" + group.getId(),
				"Invitation to join " + group.getName(),
				"You've been invited to join the " + group.getName() + " group");		
	}	
	
	public void doFeedPost(GroupFeed feed, FeedEntry entry) {
		Post post = createPost(feed, entry);		
		postPost(post, PostType.FEED);
	}

	private Post createAndIndexPost(Callable<Post> creator) {
		try {
			Post detached = runner.runTaskNotInNewTransaction(creator);
			Post post = em.find(Post.class, detached.getId());
			if (post == null)
				logger.error("reattach after creating new post FAILED ...");
			
			return post;
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			logger.error("WHAT THE HELL");
			return null; // not reached
		}		
	}
	
	private Post createPost(final User poster, final PostVisibility visibility, final boolean toWorld, final String title, final String text, final Set<Resource> resources, 
			                		final Set<Resource> personRecipients, final Set<Group> groupRecipients, final Set<Resource> expandedRecipients, final PostInfo postInfo) {
		return createAndIndexPost(new Callable<Post>() {
				public Post call() {
					Post post = new Post(poster, visibility, toWorld, title, text, personRecipients, groupRecipients, expandedRecipients, resources);
					post.setPostInfo(postInfo);
					em.persist(post);
					notifier.onPostCreated(post);
					logger.debug("saved new Post {}", post);
					return post;
				}
			});
	}
	
	private Post createPost(final GroupFeed feed, final FeedEntry entry) {
		return createAndIndexPost(new Callable<Post>() {
			public Post call() {
				FeedPost post = new FeedPost(feed, entry, expandVisibilityForGroup(PostVisibility.RECIPIENTS_ONLY, feed.getGroup()));
				em.persist(post);
				notifier.onPostCreated(post);

				logger.debug("saved new FeedPost {}", post);
				return post;
			}
		});
	}

	/*
	 * CAREFUL: These queries have to be kept in sync with the Java code in 
	 * canViewPost() 
	 */
	
	
	// Hibernate bug: I think we should be able to write
	// EXISTS (SELECT g FROM IN(post.groupRecipients) g WHERE [..])
	// according to the EJB3 persistance spec, but that results in
	// garbage SQL
	
	static private final String AUTH_GROUP =
        "g MEMBER OF post.groupRecipients AND " + 
          " EXISTS (SELECT vgm FROM GroupMember vgm, AccountClaim ac " +
          "WHERE vgm.group = g AND vgm.member = ac.resource AND ac.owner = :viewer AND " +
                "vgm.status >= " + MembershipStatus.INVITED.ordinal() + ") ";
		
	static private final String CAN_VIEW = 
		" (post.disabled != true) AND (post.visibility = " + PostVisibility.ATTRIBUTED_PUBLIC.ordinal() + " OR " +
			  "post.poster = :viewer OR " + 
              "EXISTS (SELECT ac FROM AccountClaim ac WHERE ac.owner = :viewer " +
              "        AND ac.resource MEMBER OF post.personRecipients) OR " +
              "EXISTS (SELECT g FROM Group g WHERE " + AUTH_GROUP + "))";
	
	static private final String CAN_VIEW_ANONYMOUS = 
		" (post.disabled != true) AND (post.visibility = " + PostVisibility.ATTRIBUTED_PUBLIC.ordinal() + ")";
	
	static private final String ORDER_RECENT = " ORDER BY post.postDate DESC ";

	private void addGroupRecipients(Viewpoint viewpoint, Post post, List<EntityView> recipients) {
		
		if (viewpoint instanceof SystemViewpoint) {
			for (Group g : post.getGroupRecipients()) {
				recipients.add(new GroupView(viewpoint, g, null, null));
			}
		} else if (viewpoint instanceof AnonymousViewpoint) {
			for (Group g : post.getGroupRecipients()) {
				if (g.getAccess().ordinal() >= GroupAccess.PUBLIC_INVITE.ordinal()) {
					recipients.add(new GroupView(viewpoint, g, null, null));
				}
			}
		} else {
			// We treat groups where we were a past member as visible. (See
			// GroupSystemBean.CAN_SEE_POST)			
			User viewer = ((UserViewpoint)viewpoint).getViewer();
			for (Group g : post.getGroupRecipients()) {
				boolean added = false;
				
				for (GroupMember member : g.getMembers()) {
					AccountClaim ac = member.getMember().getAccountClaim();
					if (ac != null &&
						ac.getOwner().equals(viewer) &&
						member.getStatus().getReceivesPosts()) {
						Set<PersonView> inviters  = new HashSet<PersonView>();
						if (member.getStatus() == MembershipStatus.INVITED ||
						    member.getStatus() == MembershipStatus.INVITED_TO_FOLLOW) {
							Set<User> adders = member.getAdders();
							for (User adder : adders) {
								inviters.add(personViewer.getPersonView(viewpoint, adder));
							}
						}
						recipients.add(new GroupView(viewpoint, g, member, inviters));
						added = true;
						break;
					}
				}
						
				if (!added) {
					// see if we can see this even if anonymous
					// (do this second, since it will not add the inviter as above if 
					//  we're only invited)
					if (g.getAccess().ordinal() >= GroupAccess.PUBLIC_INVITE.ordinal()) {
						recipients.add(new GroupView(viewpoint, g, null, null));
					}
				}
			}
		}
	}
	
	private List<Resource> getVisiblePersonRecipients(UserViewpoint viewpoint, Post post) {
		boolean canSeeRecipients = false;

		if (viewpoint.isOfUser(post.getPoster())) {
			canSeeRecipients = true;
		} else {
			// If you are a recipient, you can see the other recipients
			for (Resource recipient : post.getPersonRecipients()) {
				AccountClaim ac = recipient.getAccountClaim();
				if (ac != null && viewpoint.isOfUser(ac.getOwner())) {
					canSeeRecipients = true;
					break;
				}
			}
		}
		List<Resource> results = new ArrayList<Resource>();
		
		if (canSeeRecipients)
			results.addAll(post.getPersonRecipients());
		
		return results;
	}

	private EntityView getPosterView(Viewpoint viewpoint, Post post) {
		if (post.getPoster() != null)
			return personViewer.getPersonView(viewpoint, post.getPoster());
		else if (post instanceof FeedPost) {
			return new FeedView(((FeedPost)post).getFeed());
		} else {
			throw new RuntimeException("Don't know how to get a EntityView for the poster of " + post);
		}
	}
	
	// This doesn't check access to the Post, since that was supposed to be done
	// when loading the post... conceivably we should redo the API to eliminate 
	// that danger
	public PostView getPostView(Viewpoint viewpoint, Post post) {
		List<EntityView> recipients = new ArrayList<EntityView>();
		
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
			recipients.add(personViewer.getPersonView(viewpoint, recipient));
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
		
		UserBlockData ubd = null;
		if (viewpoint instanceof UserViewpoint) {
			try {
				ubd = postBlockHandler.lookupUserBlockData((UserViewpoint)viewpoint, post);
			} catch (NotFoundException e) {
				// No UserBlockData, presumably not a recipient
			}
		}
		
		Block block;
		if (ubd != null)
			block = ubd.getBlock();
		else
			block = postBlockHandler.lookupBlock(post);
		
		return new PostView(ejbContext, post, 
				getPosterView(viewpoint, post),
				block,
				ubd,
				recipients,
				viewpoint);
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
	
	private boolean isInPersonRecipients(UserViewpoint viewpoint, Post post) {
		for (Resource recipient : post.getPersonRecipients()) {
			AccountClaim ac = recipient.getAccountClaim();
			if (ac != null && viewpoint.isOfUser(ac.getOwner()))
				return true;
		}
		return false;
	}
	
	/**
	 * Can the given viewpoint view the given post based on groups the post
	 * was sent to
	 * @param viewpoint a user viewpoint
	 * @param post a post
	 * @return true if a receiving group is visible to viewpoint
	 */
	private boolean isInGroupRecipients(UserViewpoint viewpoint, Post post) {
		Set<AccountClaim> viewerClaims = viewpoint.getViewer().getAccountClaims();
		for (Group g : post.getGroupRecipients()) {
			for (GroupMember member : g.getMembers()) {
				if (member.getStatus().ordinal() >= MembershipStatus.INVITED.ordinal()) {
					for (AccountClaim ac : viewerClaims) {
						if (ac.getResource().equals(member.getMember())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Check if a particular viewer can see a particular post.
	 * 
	 * @param viewpoint
	 * @param post
	 * @return true if post is visible, false otherwise
	 */
	public boolean canViewPost(UserViewpoint viewpoint, Post post) {
		if (post.isDisabled()) {
			return false;
		}
		
		/* Optimization for a common case */
		if (viewpoint.isOfUser(post.getPoster()))
			return true;
		
		/* public post */
		if (post.getVisibility() == PostVisibility.ATTRIBUTED_PUBLIC)
			return true;
		
		if (isInPersonRecipients(viewpoint, post))
			return true;
		
		if (isInGroupRecipients(viewpoint, post))
			return true;
		
		logger.debug("User {} can't view post {}", viewpoint.getViewer(), post);
		
		return false;
	}

	public boolean canViewPost(Viewpoint viewpoint, Post post) {
		if (viewpoint == null || viewpoint instanceof SystemViewpoint) {
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

	public Post loadRawPost(Viewpoint viewpoint, Guid guid) throws NotFoundException {
		Post post = EJBUtil.lookupGuid(em, Post.class, guid);
		if (canViewPost(viewpoint, post)) {
			return post;
		} else {
			logger.debug("Viewpoint {} can't view post {}", viewpoint, post);
			throw new GuidNotFoundException(guid);
		}
	}
	
	public PostView loadPost(Viewpoint viewpoint, Guid guid) throws NotFoundException {
		// loadRawPost is supposed to do the permissions check
		Post p =  loadRawPost(viewpoint, guid);
		return getPostView(viewpoint, p);
	}

	public void postViewedBy(final Post post, final User user) {
		logger.debug("Post {} clicked by {}", post.getId(), user);
		final long clickTime = System.currentTimeMillis();
		
		// We don't actually need to wait for transaction commit to do this (the
		// transaction is going to be read-only typically), but this avoids having
		// to write our own code for asynchronous execution
		runner.runTaskOnTransactionCommit(new Runnable() {
			public void run() {
				runner.runTaskRetryingOnConstraintViolation(new Runnable() {
					public void run() {
						Post attachedPost = em.find(Post.class, post.getId());
						User attachedUser = em.find(User.class, user.getId());
		
						// Notify the recommender system that a user clicked through, so that ratings can be updated
						recommenderSystem.addRatingForPostViewedBy(attachedPost, attachedUser);
						
						// Update Block/UserBlockData for the new viewer and restack if necessary
						notifier.onPostClicked(attachedPost, attachedUser, clickTime);
					}
				});
			}
		});
		
	}

	public void postViewedBy(String postId, User user) {
		Guid postGuid;
		try {
			postGuid = new Guid(postId);
		} catch (ParseException e) {
			throw new RuntimeException("postViewedBy, bad Guid for some reason " + postId, e);
		}
		
		Post post;
		try {
			post = loadRawPost(new UserViewpoint(user), postGuid);
		} catch (NotFoundException e) {
			throw new RuntimeException("postViewedBy, nonexistent Guid for some reason " + postId, e);
		}
		
		postViewedBy(post, user);
  	}
		
  	public int getPostsForCount(Viewpoint viewpoint, Person forPerson) {
		return getPostsForCount(viewpoint, forPerson, null);
	}	
	
	public int getGroupPostsCount(Group recipient) {
		// We need to use a native query here since Hibernate is unable properly optimize
		// SELECT count(*) from Post p, Group g WHERE g.id = '12312312' AND g MEMBER OF p.groupRecipients
		// to SELECT count(*) FROM Post_HippoGroup WHERE groupRecipients_id = '12312312'
		return ((Number)em.createNamedQuery("groupPostCount")
					.setParameter("id", recipient.getId())
					.getSingleResult()).intValue();
	}
	
	public Set<EntityView> getReferencedEntities(Viewpoint viewpoint, Post post) {
		Set<EntityView> result = new HashSet<EntityView>();
		for (Group g : post.getGroupRecipients()) {
			// FIXME for now we always add anonymous group views, since at the moment
			// all the callers of this function just need anonymously viewable
			// information.  Later, if for example we add the viewer's membership status
			// to the toXml() method of GroupView, we should fix this function, probably 
			// by adding a GroupSystem.getGroupView(viewpoint, g).
			result.add(new GroupView(viewpoint, g, null, null));
		}
		for (Resource r : post.getPersonRecipients()) {
			result.add(personViewer.getPersonView(viewpoint, r));	
		}
		result.add(getPosterView(viewpoint, post));
		
		return result;
	}
	
	public boolean postIsGroupNotification(Post post) {
		try {
			String infoStr = post.getInfo();
			if (infoStr != null) {
				PostInfo info = PostInfo.parse(infoStr);
				return info.getType() == PostInfoType.SHARE_GROUP;
			}
		} catch (SAXException e) {
			logger.warn("Error parsing post info", e);
		}		
		return false;		
	}
	
	public PostSearchResult searchPosts(Viewpoint viewpoint, String queryString) {
		final String[] fields = { "explicitTitle", "text" };
		try {
			Hits hits = searchSystem.search(Post.class, fields, queryString);
			
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
	
	public boolean worthEmailNotification(Post post, Resource recipient) {
		if (post.getPersonRecipients().contains(recipient))
			return true;
		
		for (Group group : post.getGroupRecipients()) {
			if (group.getAccess() == GroupAccess.SECRET) {
				try {
					groupSystem.getGroupMember(group, recipient); // throws on not found
					return true;
				} catch (NotFoundException e) {}
			}
		}
		
		return false;
	}
	
	static final int MAX_BACKLOG = 20;

	public void setPostDisabled(Post post, boolean disabled) {
		if (post.isDisabled() != disabled) {
			post.setDisabled(disabled);
			logger.debug("Disabled flag toggled to {} on post {}", disabled, post);
			notifier.onPostDisabledToggled(post);
		}   
	}

	public List<Guid> getAllPostIds() {
		return TypeUtils.castList(Guid.class, em.createQuery("SELECT new com.dumbhippo.identity20.Guid(p.id) FROM Post p").getResultList());
	}
}
