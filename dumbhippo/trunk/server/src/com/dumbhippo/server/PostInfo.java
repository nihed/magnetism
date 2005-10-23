package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Person;
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
	private String posterName;
	private String recipientSummary;
	
	public PostInfo(IdentitySpider spider, Person viewer, Post p) {
		post = p;
		
		for (Resource r : post.getResources()) {
			if (r instanceof LinkResource) {
				LinkResource link = (LinkResource)r;
				url = link.getUrl();
				break;
			}
		}
		
		PersonView posterView = spider.getViewpoint(viewer, post.getPoster());
		posterName = posterView.getHumanReadableName();
		
		StringBuffer summary = new StringBuffer();
		int count = 0;
		for (Person recipient : post.getRecipients()) {
			PersonView recipientView = spider.getViewpoint(viewer, recipient);
			
			if (count > 0)
				summary.append(", ");
			
			if (count == 5) {
				summary.append("...");
				break;
			} else {
				summary.append(recipientView.getHumanReadableName());
				count++;
			}
		}
		
		recipientSummary = summary.toString();
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
	
	public String getPosterName() {
		return posterName;
	}
	
	public String getRecipientSummary() {
		return recipientSummary;
	}
	
	public Post getPost() {
		return post;
	}
}
