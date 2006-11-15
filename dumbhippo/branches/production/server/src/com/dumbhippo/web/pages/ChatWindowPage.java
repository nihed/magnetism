/* -*- tab-width: 4; indent-tabs-mode: t -*- */
package com.dumbhippo.web.pages;


import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.web.Signin;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.WebEJBUtil;

/**
 * Backing bean for chatwindow.jsp
 * 
 * Which properties are set depends on the kind of chat (post, group, etc. ...)
 * 
 */

public class ChatWindowPage {
    static private final Logger logger = GlobalSetup.getLogger(ChatWindowPage.class);	
    
    @Signin
	private SigninBean signin;
	
    private GroupSystem groupSystem;
    private PostingBoard postBoard;
    private PostView post;
	private GroupView group;
    
    public ChatWindowPage() {
    	groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
    }
	
    public SigninBean getSignin() {
		return signin;
    }

    public String getPostId() {
    	if (post == null)
    		return null;
    	else
    		return post.getPost().getId();
    }

    public String getGroupId() {
    	if (group == null)
    		return null;
    	else
    		return group.getGroup().getId();
    }
    
    public boolean isAboutGroup() {
    	return getGroup() != null;
    }
    
    public boolean isAboutPost() {
    	return getPost() != null;
    }
    
    public boolean isAboutSomething() {
    	return isAboutGroup() || isAboutPost();
    }
    
    protected void setPost(PostView post) {
		this.post = post;
		logger.debug("chatting about post: {}", getPostId());
		if (signin.isValid()) {
			UserViewpoint viewpoint = (UserViewpoint)signin.getViewpoint();
			postBoard.postViewedBy(getPostId(), viewpoint.getViewer());
		}
    }

    public void setPostId(String postId) {
    	logger.debug("Setting postId {}", postId);
		if (postId == null) {
			post = null;
		} else {
			try {
				String oldId = getPostId();
				if (oldId != null && oldId.equals(postId))
					; // nothing to do
				else
					post = postBoard.loadPost(signin.getViewpoint(), new Guid(postId));
			} catch (NotFoundException e) {
				post = null;
			} catch (ParseException e) {
				post = null;
			}
		}
    }

    public void setGroupId(String groupId) {
    	logger.debug("Setting groupId {}", groupId);
		if (groupId == null) {
			group = null;
		} else {
			try {
				String oldId = getGroupId();
				if (oldId != null && oldId.equals(groupId))
					; // nothing to do
				else
					group = groupSystem.loadGroup(signin.getViewpoint(), new Guid(groupId));
			} catch (NotFoundException e) {
				logger.debug("unknown group id {}", groupId);
				return;
			} catch (ParseException e) {
				group = null;
			}
		}
    }
    
    /**
     * Some callers of this page might not know what kind of chat it is, so they 
     * call this... it's important that if this is called with null though, 
     * it doesn't unset an earlier-provided ID
     * @param someId a chat ID of unknown kind
     */
    public void setChatId(String someId) {
    	logger.debug("Setting chatId {}", someId);
    	// call both of these, so we detect a guid collision if any 
    	if (post == null) // don't overwrite a postId
    		setPostId(someId);
    	if (group == null) // don't overwrite a groupId
    		setGroupId(someId);
    }
    
    public String getChatId() {
    	if (post != null)
    		return getPostId();
    	else if (group != null)
    		return getGroupId();
    	else
    		return null;
    }
    
    public PostView getPost() {
		return post;
    }
    
    public Group getGroup() {
    	// TODO: make web page take GroupView, not group
    	if (group == null)
    		return null;
    		
    	return group.getGroup();
    }
    
    public String getTitle() {
    	if (post != null)
    		return post.getTitle();
    	else if (group != null)
    		return group.getGroup().getName();
    	else
    		return "<Unknown Chat>";
    }
    
    public String getTitleAsHtml() {
    	if (post != null)
    		return post.getTitleAsHtml();
    	else if (group != null)
    		return XmlBuilder.escape(getTitle());
    	else
    		return "&lt;Unknown Chat&gt;";
    }
}
