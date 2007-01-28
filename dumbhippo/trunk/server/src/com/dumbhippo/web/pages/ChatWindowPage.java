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
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.server.blocks.GroupChatBlockHandler;
import com.dumbhippo.server.blocks.GroupChatBlockView;
import com.dumbhippo.server.blocks.MusicChatBlockHandler;
import com.dumbhippo.server.blocks.MusicChatBlockView;
import com.dumbhippo.server.blocks.PostBlockHandler;
import com.dumbhippo.server.blocks.PostBlockView;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.TrackView;
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
	
    private PersonViewer personViewer;
    private MusicSystem musicSystem;
    private Stacker stacker;
	private BlockView blockView;
    
    public ChatWindowPage() {
    	personViewer = WebEJBUtil.defaultLookup(PersonViewer.class);
		musicSystem =  WebEJBUtil.defaultLookup(MusicSystem.class);
		stacker =  WebEJBUtil.defaultLookup(Stacker.class);
    }
	
    public SigninBean getSignin() {
		return signin;
    }

    public boolean isAboutSomething() {
    	return blockView != null;
    }
    
    private PostBlockView loadPostBlockView(Guid postId) throws NotFoundException {
    	PostBlockHandler handler = WebEJBUtil.defaultLookup(PostBlockHandler.class);
    	BlockKey key = handler.getKey(postId);
    	
    	PostBlockView result = (PostBlockView)stacker.loadBlock(signin.getViewpoint(), key);
    		
    	return result;
    }
    
    public void setPostId(String postId) {
    	logger.debug("Setting postId {}", postId);
		try {
			blockView = loadPostBlockView(new Guid(postId));
		} catch (NotFoundException e) {
			// Not a post ID
		} catch (ParseException e) {
		}
    }

    private GroupChatBlockView loadGroupChatBlockView(Guid postId) throws NotFoundException {
    	GroupChatBlockHandler handler = WebEJBUtil.defaultLookup(GroupChatBlockHandler.class);
    	BlockKey key = handler.getKey(postId);
    	
    	GroupChatBlockView result = (GroupChatBlockView)stacker.loadBlock(signin.getViewpoint(), key);
    		
    	return result;
    }
    
    public void setGroupId(String groupId) {
    	logger.debug("Setting groupId {}", groupId);
		try {
			blockView = loadGroupChatBlockView(new Guid(groupId));
		} catch (NotFoundException e) {
			// Not a group ID
		} catch (ParseException e) {
		}
    }

    private MusicChatBlockView loadMusicChatBlockView(Guid trackId) throws NotFoundException {
		MusicChatBlockHandler handler = WebEJBUtil.defaultLookup(MusicChatBlockHandler.class);
    	
		TrackHistory trackHistory = musicSystem.lookupTrackHistory(trackId);

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
			
			TrackView trackView = musicSystem.getTrackView(trackHistory);
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
		try {
			blockView = loadMusicChatBlockView(new Guid(trackId));
		} catch (NotFoundException e) {
			// Not a track ID
		} catch (ParseException e) {
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
    	if (blockView == null) // don't overwrite a postId
    		setPostId(someId);
    	if (blockView == null) // don't overwrite a groupId
    		setGroupId(someId);
    	if (blockView == null) // don't overwrite a trackId
    		setTrackId(someId);
    }
    
    private PostView getPost() {
    	if (blockView.getBlockType() != BlockType.POST)
    		throw new RuntimeException("Not a post");
    		
		return ((PostBlockView)blockView).getPostView();
    }
    
    private GroupView getGroup() {
    	if (blockView.getBlockType() != BlockType.GROUP_CHAT)
    		throw new RuntimeException("Not a group");
    	
		return ((GroupChatBlockView)blockView).getGroupView();
    }

    private TrackView getTrack() {
    	if (blockView.getBlockType() != BlockType.MUSIC_CHAT)
    		throw new RuntimeException("Not a music chat");
		
		return ((MusicChatBlockView)blockView).getTrack();
    }
    
    public String getChatId() {
    	if (blockView != null) {
    		switch (blockView.getBlockType()) {
    		case GROUP_CHAT:
    			return getGroup().getGroup().getId();
    		case MUSIC_CHAT:
    			return getTrack().getPlayId();
    		case POST:
    			return getPost().getPost().getId();
			default:
    		}
    	}
    	
    	return null;
    }

    public String getTitle() {
    	if (blockView != null) {
    		switch (blockView.getBlockType()) {
    		case GROUP_CHAT:
    			return getGroup().getName();
    		case MUSIC_CHAT:
    			return getTrack().getDisplayTitle();
    		case POST:
    			return getPost().getTitle();
			default:
    		}
    	}
    	
    	return "<Unknown chat>";
    }
    
    public String getTitleAsHtml() {
    	if (getPost() != null)
    		return getPost().getTitleAsHtml();
    	else
    		return XmlBuilder.escape(getTitle());
    }
    
    public BlockView getBlock() {
    	return blockView;
    }
}
