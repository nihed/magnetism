package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Person;
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
public class PostInfo {
	private Post post;
	private String url;
	private boolean viewerHasViewed;
	private PersonInfo posterInfo;
	private List<Object> recipients;
	
	/**
	 * Create a new PostInfo object.
	 * 
	 * @param spider an IdentitySpider object
	 * @param viewer the person viewing the post, may be null
	 * @param p the post to view
	 * @param ppd information about the relationship of the viewer to the post, must be
	 *        null if viewer is null.
	 */
	public PostInfo(IdentitySpider spider, Person viewer, Post p, PersonPostData ppd) {
		post = p;
		
		for (Resource r : post.getResources()) {
			if (r instanceof LinkResource) {
				LinkResource link = (LinkResource)r;
				url = link.getUrl();
				break;
			}
		}
		
	    posterInfo = new PersonInfo(spider, viewer, post.getPoster());
		
		recipients = new ArrayList<Object>();
		
		recipients.addAll(post.getGroupRecipients());
		
		for (Person recipient : post.getPersonRecipients()) {
			recipients.add(new PersonInfo(spider, viewer, recipient));
		}
		
		viewerHasViewed = ppd != null;
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
	
	public PersonInfo getPosterInfo() {
		return posterInfo;
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
