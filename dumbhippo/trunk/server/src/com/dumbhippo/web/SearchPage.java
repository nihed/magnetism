package com.dumbhippo.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

public class SearchPage {
	static private final Log logger = GlobalSetup.getLog(SearchPage.class);
	
	static private final int DEFAULT_COUNT = 15;
	static private final int MAX_COUNT = 50;
	
	@Signin
	private SigninBean signin;

	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	
	private String searchText;
	private int start;
	private int count;
	private String posterId;
	private String recipientId;
	private String groupId;
	
	private ListBean<PostView> posts;
	private Person recipient;
	private Person poster;
	private Group group;

	
	public SearchPage() {
		searchText = "";
		start = 0;
		count = DEFAULT_COUNT;
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}

	public Person getRecipient() {
		if (recipient == null) {
			if (recipientId != null && recipientId.length() > 0) {
				try {
					recipient = identitySpider.lookupGuidString(Person.class, recipientId);
				} catch (ParseException e) {
					logger.trace("bad recipientId", e);
				} catch (GuidNotFoundException e) {
					logger.trace("bad recipientId", e);
				}
			}
		}
		return recipient;
	}
	
	public Person getPoster() {
		if (poster == null) {
			if (posterId != null && posterId.length() > 0) {
				try {
					poster = identitySpider.lookupGuidString(Person.class, posterId);
				} catch (ParseException e) {
					logger.trace("bad posterId", e);
				} catch (GuidNotFoundException e) {
					logger.trace("bad posterId", e);
				}
			}
		}
		return poster;
	}
	
	public Group getGroup() {
		if (group == null) {
			if (groupId != null && groupId.length() > 0) {
				try {
					group = identitySpider.lookupGuidString(Group.class, groupId);
				} catch (ParseException e) {
					logger.trace("bad groupId", e);
				} catch (GuidNotFoundException e) {
					logger.trace("bad groupId", e);
				}
			}
		}
		return group;
	}
	
	public ListBean<PostView> getPosts() {
		
		if (posts != null)
			return posts;	
		
		List<PostView> results;
		
		// FIXME rather than putting these in a priority order we could merge them someway, 
		// but who wants to think about that...
		// we always ask for getCount() + 1 so we can tell if we got them all
		if (getPoster() != null)
			results = postBoard.getPostsFor(signin.getViewpoint(), getPoster(), searchText, getStart(), getCount() + 1);
		else if (getRecipient() != null)
			results = postBoard.getReceivedPosts(signin.getViewpoint(), getRecipient(), searchText, getStart(), getCount() + 1);
		else if (getGroup() != null)
			results = postBoard.getGroupPosts(signin.getViewpoint(), getGroup(), searchText, getStart(), getCount() + 1);
		else
			results = new ArrayList<PostView>(); // FIXME some kind of global search
		
		if (results == null)
			throw new IllegalStateException("should not have gotten null results in search");
		
		posts = new ListBean<PostView>(results);
		
		return posts;
	}
	
	private String urlEncode(String in) {
		try {
			return URLEncoder.encode(in, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// it's ridiculous that this is a checked exception
			throw new RuntimeException(e);
		}
	}
	
	private String getParams(int start) {
		if (start < 0)
			start = 0;
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("start=" + start);
		sb.append("&count=" + getCount());
		
		// these are untrusted from the params we got in the request
		
		if (groupId != null && groupId.length() > 0) {
			sb.append("&groupId=");
			sb.append(urlEncode(groupId));
		}
	
		if (posterId != null && posterId.length() > 0) {
			sb.append("&posterId=");
			sb.append(urlEncode(posterId));
		}

		if (recipientId != null && recipientId.length() > 0) {
			sb.append("&recipientId=");
			sb.append(urlEncode(recipientId));
		}
		
		return sb.toString();
	}
	
	public String getPreviousParams() {
		return getParams(getStart() - getCount());
	}
	
	public String getNextParams() {
		return getParams(getStart() + getCount());
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		if (count > MAX_COUNT || count < 1)
			this.count = MAX_COUNT;
		else
			this.count = count;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		if (searchText != null)
			searchText = searchText.trim();
		this.searchText = searchText;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		if (start < 0)
			start = 0;
		this.start = start;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		if (groupId != null)
			groupId = groupId.trim();
		this.groupId = groupId;
	}

	public String getPosterId() {
		return posterId;
	}

	public void setPosterId(String posterId) {
		if (posterId != null)
			posterId = posterId.trim();
		this.posterId = posterId;
	}

	public String getRecipientId() {
		return recipientId;
	}

	public void setRecipientId(String recipientId) {
		if (recipientId != null)
			recipientId = recipientId.trim();
		this.recipientId = recipientId;
	}
}
