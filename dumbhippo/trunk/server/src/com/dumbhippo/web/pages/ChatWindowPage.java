/* -*- tab-width: 4; indent-tabs-mode: t -*- */
package com.dumbhippo.web.pages;


import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.server.blocks.MusicChatBlockHandler;
import com.dumbhippo.server.blocks.MusicChatBlockView;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.TrackView;
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
    private PersonViewer personViewer;
    private PostingBoard postBoard;
    private MusicSystem musicSystem;
    private Stacker stacker;
    private PostView post;
	private GroupView group;
	private TrackView track;
	private BlockView blockView;
    
    public ChatWindowPage() {
    	groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
    	personViewer = WebEJBUtil.defaultLookup(PersonViewer.class);
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		musicSystem =  WebEJBUtil.defaultLookup(MusicSystem.class);
		stacker =  WebEJBUtil.defaultLookup(Stacker.class);
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
    
    public String getTrackId() {
    	if (track == null)
    		return null;
    	else
    		return track.getPlayId();
    }
    
    public boolean isAboutGroup() {
    	return getGroup() != null;
    }
    
    public boolean isAboutPost() {
    	return getPost() != null;
    }
    
    public boolean isAboutTrack() {
    	return getTrack() != null;
    }
    
    public boolean isAboutSomething() {
    	return isAboutGroup() || isAboutPost() || isAboutTrack();
    }
    
    protected void setPost(PostView post) {
		this.post = post;
		logger.debug("chatting about post: {}", getPostId());
		if (signin.isValid()) {
			UserViewpoint viewpoint = (UserViewpoint)signin.getViewpoint();
			postBoard.postViewedBy(post.getPost(), viewpoint.getViewer());
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
				return;
			} catch (ParseException e) {
				group = null;
			}
		}
    }

    private BlockView getBlockView(TrackView trackView, TrackHistory trackHistory) {
    	MusicChatBlockHandler handler = WebEJBUtil.defaultLookup(MusicChatBlockHandler.class);
    	BlockKey key = handler.getKey(trackHistory);
    	MusicChatBlockView result;
    	
		try {
	    	result = (MusicChatBlockView)stacker.loadBlock(signin.getViewpoint(), key);
		} catch (NotFoundException e) {
			// If someone opens a chatwindow for a track before any quips/comments have
			// been added to it, the MUSIC_CHAT block doesn't yet exist; we want to 
			// avoid database changes when loading a JSP, so what we do is create a dummy 
			// block *not* persisted to the database, and manually create the BlockView
			// for that block. There is some danger here because the ID of the Block will
			// be random, so if any controls are present in the block (like a hush
			// link) they won't work. So we need to be careful that those controls don't
			// appear in the track page.
			
			PersonView personView = personViewer.getPersonView(signin.getViewpoint(), trackHistory.getUser());
			
			Block block = new Block(key);
			result = new MusicChatBlockView(signin.getViewpoint(), block, (UserBlockData)null, false);
			List<ChatMessageView> messages = Collections.emptyList();
			result.populate(personView, trackView, messages, 0);
		}
		
		return result;
    }
    
    public void setTrackId(String trackId) {
    	logger.debug("Setting trackId {}", trackId);
		if (trackId == null) {
			track = null;
		} else {
			try {
				String oldId = getTrackId();
				if (oldId != null && oldId.equals(trackId))
					; // nothing to do
				else {
					TrackHistory trackHistory = musicSystem.lookupTrackHistory(new Guid(trackId));
					track = musicSystem.getTrackView(trackHistory);
					
					blockView = getBlockView(track, trackHistory);
				}
			} catch (NotFoundException e) {
				return;
			} catch (ParseException e) {
				track = null;
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
    	if (track == null) // don't overwrite a trackId
    		setTrackId(someId);
    }
    
    public String getChatId() {
    	if (post != null)
    		return getPostId();
    	else if (group != null)
    		return getGroupId();
    	else if (track != null)
    		return getTrackId();
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

    public TrackView getTrack() {
    	return track;
    }
    
    public String getTitle() {
    	if (post != null)
    		return post.getTitle();
    	else if (group != null)
    		return group.getGroup().getName();
    	else if (track != null)
    		return track.getDisplayTitle();
    	else
    		return "<Unknown Chat>";
    }
    
    public String getTitleAsHtml() {
    	if (post != null)
    		return post.getTitleAsHtml();
    	else
    		return XmlBuilder.escape(getTitle());
    }
    
    public BlockView getBlock() {
    	return blockView;
    }
}
