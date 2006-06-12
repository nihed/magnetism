package com.dumbhippo.web.pages;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.Signin;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.WebEJBUtil;

public class SearchPage {
	static private final Logger logger = GlobalSetup.getLogger(SearchPage.class);
	
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
	private User recipient;
	private User poster;
	private Group group;

	
	public SearchPage() {
		searchText = "";
		start = 0;
		count = DEFAULT_COUNT;
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}

	public User getRecipient() {
		if (recipient == null) {
			if (recipientId != null && recipientId.length() > 0) {
				try {
					recipient = identitySpider.lookupGuidString(User.class, recipientId);
				} catch (ParseException e) {
					logger.debug("bad recipientId", e);
				} catch (NotFoundException e) {
					logger.debug("bad recipientId", e);
				}
			}
		}
		return recipient;
	}
	
	public User getPoster() {
		if (poster == null) {
			if (posterId != null && posterId.length() > 0) {
				try {
					poster = identitySpider.lookupGuidString(User.class, posterId);
				} catch (ParseException e) {
					logger.debug("bad posterId", e);
				} catch (NotFoundException e) {
					logger.debug("bad posterId", e);
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
					logger.debug("bad groupId", e);
				} catch (NotFoundException e) {
					logger.debug("bad groupId", e);
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
		else if (getRecipient() != null && signin.isValid()) {
			UserViewpoint userViewpoint = (UserViewpoint)signin.getViewpoint();
			results = postBoard.getReceivedPosts(userViewpoint, getRecipient(), searchText, getStart(), getCount() + 1);
		} else if (getGroup() != null)
			results = postBoard.getGroupPosts(signin.getViewpoint(), getGroup(), searchText, getStart(), getCount() + 1);
		else
			results = new ArrayList<PostView>(); // FIXME some kind of global search
		
		if (results == null)
			throw new IllegalStateException("should not have gotten null results in search");
		
		posts = new ListBean<PostView>(results);
		
		return posts;
	}
	
	private String getParams(int start) {
		if (start < 0)
			start = 0;
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("start=" + start);
		sb.append("&count=" + getCount());
		
		// these are untrusted from the params we got in the request
		
		if (groupId != null && groupId.length() > 0) {
			sb.append("&group=");
			sb.append(StringUtils.urlEncode(groupId));
		}
	
		if (posterId != null && posterId.length() > 0) {
			sb.append("&poster=");
			sb.append(StringUtils.urlEncode(posterId));
		}

		if (recipientId != null && recipientId.length() > 0) {
			sb.append("&recipient=");
			sb.append(StringUtils.urlEncode(recipientId));
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
