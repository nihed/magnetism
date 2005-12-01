package com.dumbhippo.server;

import java.util.List;

import com.dumbhippo.StringUtils;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.postinfo.PostInfoType;
import com.dumbhippo.server.formatters.AmazonFormatter;
import com.dumbhippo.server.formatters.DefaultFormatter;
import com.dumbhippo.server.formatters.EbayFormatter;
import com.dumbhippo.server.formatters.PostFormatter;

/**
 * @author otaylor
 *
 * This is a class encapsulating information about a Post that can be
 * returned out of the session tier and used by web pages; only the
 * constructor accesses lazily loaded fields of the Post; all of its
 * properties and methods are safe to access at any point.
 */
public class PostView {
	private Post post;
	private String url;
	private boolean viewerHasViewed;
	private PersonView posterView;
	private List<Object> recipients;
	private String search;
	private PostFormatter formatter;
	
	/**
	 * Create a new PostView object.
	 * 
	 * @param p the post to view
	 * @param poster the person who posted the post
	 * @param ppd information about the relationship of the viewer to the post, may be null
	 * @param recipientList the list of (visible) recipients of the post
	 */
	public PostView(Post p, PersonView poster, PersonPostData ppd, List<Object>recipientList) {
		post = p;
		posterView = poster;
		viewerHasViewed = ppd != null;
		recipients = recipientList;
		
		for (Resource r : post.getResources()) {
			if (r instanceof LinkResource) {
				LinkResource link = (LinkResource)r;
				url = link.getUrl();
				break;
			}
		}
	}
	
	private PostFormatter getFormatter() {
		if (formatter != null)
			return formatter;
		
		PostInfo postInfo = post.getPostInfo();
		PostInfoType type = postInfo != null ? postInfo.getType() : PostInfoType.GENERIC;
		
		// FIXME the formatters are stateless so we could share the instances
		if (type == PostInfoType.AMAZON) {
			formatter = new AmazonFormatter();
		} else if (type == PostInfoType.EBAY) {
			formatter = new EbayFormatter();
		} else {
			formatter = new DefaultFormatter();
		}
		
		assert formatter != null;
		return formatter;
	}
	
	public String getTitle() {
		String title = post.getExplicitTitle();
		if (title != null && !title.equals(""))
			return title;
		else
			return url;
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
	
	public String highlightSearchWords(String html) {
		if (search == null)
			return html;
	
		String[] terms = StringUtils.splitWords(search);
		if (terms.length == 0)
			return html;
		
		for (String t : terms) {
			html = html.replace(t, "<span style=\"font-weight: bold; color: red;\">" + t + "</span>");
		}
		return html;
	}
	
	public String getTitleAsHtml() {
		return getFormatter().getTitleAsHtml(this);
	}
	
	public String getTextAsHtml() {
		return getFormatter().getTextAsHtml(this);		
	}
}
