/* -*- tab-width: 4; indent-tabs-mode: t -*- */
package com.dumbhippo.web;


import java.util.ArrayList;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.server.ChatRoomStatusCache;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

/**
 * Displays a post in a frame with information about how it was shared
 * 
 * @author dff
 */

public class FramerPage {
    static private final Log logger = GlobalSetup.getLog(FramerPage.class);	
	
    private String postId;

    @Signin
		private SigninBean signin;
	
    private PostingBoard postBoard;
    private PostView post;
	private String chatroom;
	
    public FramerPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
    }
	
    public SigninBean getSignin() {
		return signin;
    }

    public String getPostId() {
		return postId;
    }

    public String getChatRoom() {

    	// title/url based chat room names temporarily disabled,
    	// as this approach doesn't give consistent names across
    	// sites...
    	
    	/*
    	String title = this.post.getTitle();
		String url = this.post.getUrl();
		if  ( title == url ) {
			// Get the domain name
			try {
				java.net.URL urlObject = new java.net.URL(url);
				title = urlObject.getHost();
			} catch (java.net.MalformedURLException e) {
				title = "DumbHippoChat";
				logger.debug("Couldn't parse the URL object, created generic chat room: " + title);
			}
		}
		// Remove spaces and any other weird characters, even "+" isn't handled right by our bot
		title = title.replaceAll("[^a-zA-Z0-9]","");
		if (title.length() >= 10)
			return title.substring(0, 10);
		else 
			return title;
	    */
    	
    	// use a consistent url->letter-only hashcode for chat room names
    	chatroom = "dh"+Math.abs(this.post.getUrl().hashCode());
    	chatroom = chatroom.replaceAll("0","a");
    	chatroom = chatroom.replaceAll("1","b");
    	chatroom = chatroom.replaceAll("2","c");
    	chatroom = chatroom.replaceAll("3","d");
    	chatroom = chatroom.replaceAll("4","e");
    	chatroom = chatroom.replaceAll("5","f");
    	chatroom = chatroom.replaceAll("6","g");
    	chatroom = chatroom.replaceAll("7","h");
    	chatroom = chatroom.replaceAll("8","i");
    	chatroom = chatroom.replaceAll("9","j");
    	return chatroom;
    }
    
    public String getChatRoomMembers() {
    	String roomname = this.getChatRoom();
    	ArrayList<String> members = (ArrayList<String>)(ChatRoomStatusCache.getChatRoomStatus(roomname));
    	logger.debug("chat room status cache lookup for '" + roomname + "' gave " + members);
    	if ((members == null) || (members.size() == 0)) {
    		return "Start a new chat!";
    	} else {	
    		String memberlist = "Already in chat room: ";
    		for (String mem: members) {
    			memberlist = memberlist + mem + " ";
    		}
    		return memberlist;
    	}
    }

    protected void setPost(PostView post) {
		this.post = post;
		this.postId = post.getPost().getId();
		logger.debug("viewing post: " + this.postId);
    }

    public void setPostId(String postId) throws ParseException, GuidNotFoundException {
		if (postId == null) {
			logger.debug("no post id");
			return;
		} else {
			// Fixme - don't backtrace if the user isn't authorized to view the post
			setPost(postBoard.loadPost(signin.getViewpoint(), new Guid(postId)));
		}
    }
	
    public PostView getPost() {
		return post;
    }
}
