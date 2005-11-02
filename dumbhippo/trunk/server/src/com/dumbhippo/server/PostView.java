package com.dumbhippo.server;

import java.util.List;

import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;

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
}
