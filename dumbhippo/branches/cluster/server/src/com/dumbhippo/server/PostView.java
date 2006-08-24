package com.dumbhippo.server;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJBContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.live.LivePost;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
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
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(PostView.class);
	
	private Context context;
	private Post post;
	private String url;
	private boolean viewerHasViewed;
	private EntityView posterView;
	private List<Object> recipients;
	private String search;
	private PostFormatter formatter;
	private Resource mailRecipient;
	private Viewpoint viewpoint;
	private LivePost livePost;
	private boolean favorite;
	private boolean ignored;
	private boolean toWorld;
	
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
	public PostView(EJBContext ejbContext, Post p, EntityView poster, PersonPostData ppd, List<Object>recipientList, Viewpoint viewpoint) {
		this(p, Context.WEB_BUBBLE);
		posterView = poster;
		viewerHasViewed = (ppd != null && ppd.getClickedDate() != null);
		recipients = recipientList;
		this.viewpoint = viewpoint;
		this.toWorld = p.isToWorld();
	
		if (viewpoint instanceof UserViewpoint) {
			User viewer = ((UserViewpoint) viewpoint).getViewer();
			this.favorite = viewer.getAccount().getFavoritePosts().contains(post);
			this.ignored = (ppd != null && ppd.isIgnored());
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

	public Collection<String> getSearchTerms() {
		if (search != null) {
			String[] terms = StringUtils.splitWords(search);
			return Arrays.asList(terms);
		} else {
			return Collections.emptyList();
		}
	}

	// CAUTION - used in both HttpMethods AND XMPP, migrate both xmpp client and javascript if you modify this
	public void writeToXmlBuilder(XmlBuilder builder) {
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
	
	public String toXml() {
		XmlBuilder builder = new XmlBuilder();
		writeToXmlBuilder(builder);
		return builder.toString();
	}
	
	public LivePost getLivePost() {
		if (livePost == null) {
			LiveState liveState = LiveState.getInstance();
			livePost = liveState.getLivePost(post.getGuid());
		}
		return livePost;
	}
}
