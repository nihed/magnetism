package com.dumbhippo.jive;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError.Condition;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.LiveEventListener;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.PostCreatedEvent;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.MessengerGlue;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.util.EJBUtil;

public class RecentPostsIQHandler extends AbstractIQHandler implements LiveEventListener<PostCreatedEvent> {

	private IQHandlerInfo info;
	
	public RecentPostsIQHandler() {
		super("DumbHippo Recent Posts IQ Handler");
		
		Log.debug("creating Hotness handler");
		info = new IQHandlerInfo("recentPosts", "http://dumbhippo.com/protocol/post");
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		Log.debug("handling IQ packet " + packet);
		JID from = packet.getFrom();
		IQ reply = IQ.createResultIQ(packet);
		
		// id attribute is optional. It indicates a request for a particular
		// (not necessarily recent) post.
		Element element = packet.getChildElement();
		String id = element.attributeValue("id");
		
		MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
		
		String recentPostsString;
		try {
			Guid postId = (id == null ? null : new Guid(id));
			recentPostsString = glue.getPostsXml(Guid.parseTrustedJabberId(from.getNode()), 
					                             postId, null);
		} catch (ParseException e) {
			recentPostsString = null;
		}
		
		if (recentPostsString != null) {
			Document recentPostsDocument;
			try {
				recentPostsDocument = DocumentHelper.parseText(recentPostsString);
			} catch (DocumentException e) {
				throw new RuntimeException("Couldn't parse result from getRecentPostsXML()");
			}
		
			Element childElement = recentPostsDocument.getRootElement();
			childElement.detach();
			reply.setChildElement(childElement);
		} else {
			reply.setError(Condition.item_not_found);
		}
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
	
	////////////////////////////////////////////////////////////////////////////////
	//
	// We wedge handling of sending out notifications for newly created posts
	// into here. The only real excuse for this is that the notifications for
	// newly created posts will go away once the block-stacking system
	// is fully in place.
	
	@Override
	public void start() throws IllegalStateException {
		super.start();
		Log.debug("setting up UserPrefChangedEvent listener");
		LiveState.addEventListener(PostCreatedEvent.class, this);		
	}

	@Override
	public void stop() {
		super.stop();
		Log.debug("stopping UserPrefChangedEvent listener");		
		LiveState.removeEventListener(PostCreatedEvent.class, this);			
	}
	
	public void onEvent(PostCreatedEvent event) {
		MessageSender messageSender = MessageSender.getInstance();
		
		PostingBoard postingBoard = EJBUtil.defaultLookup(PostingBoard.class);
		Post post;
		try {
			post = postingBoard.loadRawPost(SystemViewpoint.getInstance(), event.getPostId());
		} catch (NotFoundException e) {
			Log.error("Got PostCreatedEvent for a non-existant post");
			return;
		}
		
		for (Resource resource : post.getExpandedRecipients()) {
			// Since we are sending out initial notifications, we only need
			// to worry about people who actually had accounts when the post
			// was created.
			if (!(resource instanceof Account))
				continue;
			
			// Avoid creating the payload for users not on this server
			User user = ((Account)resource).getOwner();
			if (!messageSender.userIsPresent(user.getGuid()))
				continue;
			
			messageSender.sendNewPostMessage(user, post);
		}
	}
}
