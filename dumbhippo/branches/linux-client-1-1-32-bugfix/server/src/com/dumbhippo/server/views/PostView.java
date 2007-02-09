package com.dumbhippo.server.views;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJBContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.live.PresenceService;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.FeedPost;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
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
public class PostView implements ObjectView {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(PostView.class);
	
	private Context context;
	private Post post;
	private String url;
	private boolean viewerHasViewed;
	private EntityView posterView;
	private List<EntityView> recipients;
	private String search;
	private PostFormatter formatter;
	private Resource mailRecipient;
	private Viewpoint viewpoint;
	private int totalViewers;
	private boolean favorite;
	private boolean ignored;
	private boolean toWorld;
	private String feedFavicon;
	
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
	 * @param block object for this post
	 * @param ubd information about the relationship of the viewer to the post, may be null
	 * @param chatRoom the associated chatRoom, or null
	 * @param lastFewMessages the last few chat room messages, or null
	 * @param recipientList the list of (visible) recipients of the post
	 * @param viewpoint who is looking at the post
	 */
	public PostView(EJBContext ejbContext, Post p, EntityView poster, Block block, UserBlockData ubd, List<EntityView>recipientList, Viewpoint viewpoint) {
		this(p, Context.WEB_BUBBLE);
		posterView = poster;
		viewerHasViewed = (ubd != null && ubd.isClicked());
		recipients = recipientList;
		this.viewpoint = viewpoint;
		this.toWorld = p.isToWorld();
		
		totalViewers = block.getClickedCount();
	
		if (p instanceof FeedPost) {
			feedFavicon = ((FeedPost)p).getFeed().getFeed().getFavicon();
		}
		
		if (viewpoint instanceof UserViewpoint) {
			User viewer = ((UserViewpoint) viewpoint).getViewer();
			this.favorite = viewer.getAccount().getFavoritePosts().contains(post);
			this.ignored = (ubd != null && ubd.isIgnored());
		}
		
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
	
	public EntityView getPoster() {
		return posterView;
	}
	
	public List<EntityView> getRecipients() {
		return recipients;
	}
	
	public Post getPost() {
		return post;
	}

	public String getFeedFavicon() {
		return feedFavicon;
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
	
	public String getTextAsHtmlShort() {
		String textAsHtml = getFormatter().getTextAsHtml();
		if (textAsHtml.length() >= 70) {
			return textAsHtml.substring(0, 70) + "...";
		}
		return textAsHtml;	
	}	
	
	public boolean isChatRoomActive() {
		return true;
	}

	public int getTotalViewers() {
		return totalViewers;
	}
	
	public boolean isFavorite() {
		return favorite;
	}
	
	public boolean isToWorld() {
		return toWorld;
	}
	
	public boolean isIgnored() {
		return ignored;
	}
	
	public String getChatRoomMembers() {
		return "Start chatting";

//      return "Start a new chat!";
//		String memberlist = "Join chat with ";
//		for (ChatRoomScreenName mem: members) {
//			memberlist = memberlist + mem.getScreenName() + " ";
//		}
//				return memberlist;
	}

	public int getChattingUserCount() {
		return PresenceService.getInstance().getPresentUsers("/rooms/" + post.getId(), 2).size();
	}
	
	public Collection<String> getSearchTerms() {
		if (search != null) {
			String[] terms = StringUtils.splitWords(search);
			return Arrays.asList(terms);
		} else {
			return Collections.emptyList();
		}
	}

	// CAUTION - used in both HttpMethods AND XMPP, migrate both xmpp client and javascript if you modify this
	public void writeToXmlBuilderOld(XmlBuilder builder) {
		builder.openElement("post", "id", post.getId());
		builder.appendTextNode("poster", posterView.getIdentifyingGuid().toString());
		builder.appendTextNode("href", post.getUrl().toString());
		builder.appendTextNode("title", post.getTitle());
		builder.appendTextNode("text", post.getText());
		builder.appendBooleanNode("toWorld", isToWorld());
		builder.appendLongNode("postDate", post.getPostDate().getTime()/1000);
		PostInfo pi = post.getPostInfo();
		if (pi != null)
			builder.appendTextNode("postInfo", pi.toXml());
		builder.openElement("recipients");
		for (Object o : recipients) {
			if (o instanceof PersonView) {
				PersonView pv = (PersonView) o;
				builder.append(pv.toIdentifyingXml());
			} else if (o instanceof GroupView) {
				GroupView gv = (GroupView) o;
				builder.append(gv.toIdentifyingXml());
			}			
		}
		builder.closeElement();		
		builder.appendBooleanNode("favorite", favorite);
		builder.appendBooleanNode("ignored", ignored);
		builder.closeElement();
	}
	
	public String toXmlOld() {
		XmlBuilder builder = new XmlBuilder();
		writeToXmlBuilderOld(builder);
		return builder.toString();
	}

	public Guid getIdentifyingGuid() {
		return post.getGuid();
	}
	
	public List<Object> getReferencedObjects() {
		List<Object> result = new ArrayList<Object>();
		
		result.add(posterView);
		for (EntityView entityView : recipients)
			result.add(entityView);
		
		return result;
	}
	
	public void writeToXmlBuilder(XmlBuilder builder) {
		builder.openElement("post", 
						    "id", post.getId(),
						    "poster", posterView.getIdentifyingGuid().toString(),
						    "href", post.getUrl().toString(),
							"postDate", Long.toString(post.getPostDate().getTime()),
							"toWorld", Boolean.toString(isToWorld()),
							"viewed", Boolean.toString(isViewerHasViewed()),
							"totalViewers", Integer.toString(getTotalViewers()),
							"favorite", Boolean.toString(favorite));
							
		builder.appendTextNode("title", post.getTitle());
		builder.appendTextNode("description", StringUtils.ellipsizeText(post.getText()));
		
		builder.openElement("recipients");
		for (EntityView entityView : recipients) {
			builder.appendEmptyNode("recipient",
					                "recipientId", entityView.getIdentifyingGuid().toString()); 
		}
		builder.closeElement();
		
		builder.closeElement();
	}
}
