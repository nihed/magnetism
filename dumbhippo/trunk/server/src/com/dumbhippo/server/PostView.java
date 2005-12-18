package com.dumbhippo.server;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJBContext;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.persistence.ChatRoom;
import com.dumbhippo.persistence.ChatRoomMessage;
import com.dumbhippo.persistence.ChatRoomScreenName;
import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.postinfo.PostInfoType;
import com.dumbhippo.server.formatters.AmazonFormatter;
import com.dumbhippo.server.formatters.DefaultFormatter;
import com.dumbhippo.server.formatters.EbayFormatter;
import com.dumbhippo.server.formatters.FlickrFormatter;
import com.dumbhippo.server.formatters.PostFormatter;
import com.dumbhippo.server.formatters.ShareGroupFormatter;

/**
 * @author otaylor
 *
 * This is a class encapsulating information about a Post that can be
 * returned out of the session tier and used by web pages; only the
 * constructor accesses lazily loaded fields of the Post; all of its
 * properties and methods are safe to access at any point.
 */
public class PostView {
	private static final Log logger = GlobalSetup.getLog(PostView.class);
	
	private Context context;
	private Post post;
	private String url;
	private boolean viewerHasViewed;
	private PersonView posterView;
	private ChatRoom chatRoom;
	private List<ChatRoomMessage> lastFewMessages;
	private List<Object> recipients;
	private String search;
	private PostFormatter formatter;
	private Resource mailRecipient;
	private Viewpoint viewpoint;
	
	public enum Context {
		MAIL_NOTIFICATION,
		WEB_BUBBLE
	};
	
	private PostView(Post p, Context context) {
		this.post = p;
		
		URL u = post.getUrl();
		if (u != null)
			url = u.toExternalForm();
		
		this.context = context;
	}
	
	/**
	 * Create a new PostView object for display on the web site.
	 * 
	 * @param ejbContext current EJB context (only accessed inside constructor)
	 * @param p the post to view
	 * @param poster the person who posted the post
	 * @param ppd information about the relationship of the viewer to the post, may be null
	 * @param chatRoom the associated chatRoom, or null
	 * @param lastFewMessages the last few chat room messages, or null
	 * @param recipientList the list of (visible) recipients of the post
	 * @param viewpoint who is looking at the post
	 */
	public PostView(EJBContext ejbContext, Post p, PersonView poster, PersonPostData ppd, ChatRoom chatRoom, List<ChatRoomMessage> lastFewMessages, List<Object>recipientList, Viewpoint viewpoint) {
		this(p, Context.WEB_BUBBLE);
		posterView = poster;
		viewerHasViewed = ppd != null;
		recipients = recipientList;
		this.chatRoom = chatRoom;
		this.lastFewMessages = lastFewMessages;
		this.viewpoint = viewpoint;
		
		initFormatter(ejbContext);
	}
	
	public PostView(EJBContext ejbContext, Post p, Resource mailRecipient) {
		this(p, Context.MAIL_NOTIFICATION);
		this.mailRecipient = mailRecipient;

		if (this.mailRecipient == null)
			throw new IllegalArgumentException("must specify mail recipient for mail notification PostView");
		
		initFormatter(ejbContext);
	}
	
	private PostFormatter getFormatter() {
		return formatter;
	}
	
	private void initFormatter(EJBContext ejbContext) {
		PostInfo postInfo = post.getPostInfo();
		PostInfoType type = postInfo != null ? postInfo.getType() : PostInfoType.GENERIC;
		
		switch (type) {
		case AMAZON:
			formatter = new AmazonFormatter();
			break;
		case EBAY:
			formatter = new EbayFormatter();
			break;
		case FLICKR:
			formatter = new FlickrFormatter();
			break;
		case SHARE_GROUP:
			formatter = new ShareGroupFormatter();
			break;
		default:
			formatter = new DefaultFormatter();
			break;
		}
		
		assert formatter != null;
		
		// bind formatter to this PostView
		formatter.init(this, ejbContext);
	}
	
	public Viewpoint getViewpoint() {
		return viewpoint;
	}
	
	public Context getContext() {
		return context;
	}
	
	public Resource getMailRecipient() {
		return mailRecipient;
	}
	
	public String getTitle() {
		return getFormatter().getTitleAsText();
	}

	public String getText() {
		return getFormatter().getTextAsText();
	}
	
	public String getUrl() {
		return url;	
	}
	
	public PersonView getPoster() {
		return posterView;
	}
	
	public List<Object> getRecipients() {
		return recipients;
	}
	
	public Post getPost() {
		return post;
	}

	public boolean isViewerHasViewed() {
		return viewerHasViewed;
	}
	
	public void setSearch(String search) {
		this.search = search;
	}
	
	public String getTitleAsHtml() {
		return getFormatter().getTitleAsHtml();
	}
	
	public String getTextAsHtml() {
		return getFormatter().getTextAsHtml();		
	}
	
	public String getChatRoomName() {
		if (chatRoom != null) {
			return chatRoom.getName();
		} else {
			return ChatRoom.createChatRoomNameStringFor(this.post);
		}
	}
	
	public boolean isChatRoomActive() {
		if (chatRoom == null) {
			logger.debug("chatroom is null on isChatRoomActive()");
			return false;
		} else {
			logger.debug("chatroom size on isChatRoomActive() is " + chatRoom.getRoster().size());
			return (chatRoom.getRoster().size() > 0);
		}
	}
	
	public String getChatRoomMembers() {
		if (chatRoom == null) {
			logger.debug("chatroom is null");
			return "Start a new chat!";
		} else {
			List<ChatRoomScreenName> members = chatRoom.getRoster();
			if ((members == null) || (members.size() == 0)) {
				logger.debug("chatroom is empty");
				if (members == null) {
					logger.debug("members is null");
				} else {
					logger.debug("members size is zero");
				}
				return "Start a new chat!";
			} else {	
				String memberlist = "Join chat with ";
				for (ChatRoomScreenName mem: members) {
					memberlist = memberlist + mem.getScreenName() + " ";
				}
				return memberlist;
			}
		}
	}

	public List<ChatRoomMessage> getLastFewChatRoomMessages() {
		return lastFewMessages;
	}

	public Collection<String> getSearchTerms() {
		if (search != null) {
			String[] terms = StringUtils.splitWords(search);
			return Arrays.asList(terms);
		} else {
			return Collections.emptyList();
		}
	}
}
