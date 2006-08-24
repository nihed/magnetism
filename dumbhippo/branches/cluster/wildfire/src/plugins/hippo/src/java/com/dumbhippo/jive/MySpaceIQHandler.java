package com.dumbhippo.jive;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.tree.DefaultElement;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;

import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.MessengerGlueRemote.MySpaceBlogCommentInfo;
import com.dumbhippo.server.MessengerGlueRemote.MySpaceContactInfo;
import com.dumbhippo.server.util.EJBUtil;

public class MySpaceIQHandler extends AbstractIQHandler {

	private IQHandlerInfo info;
	
	public MySpaceIQHandler() {
		super("DumbHippo MySpace IQ Handler");
		Log.debug("creating MySpace IQ handler");
		info = new IQHandlerInfo("myspace", "http://dumbhippo.com/protocol/myspace");
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		Log.debug("handling IQ packet " + packet);
		IQ reply = IQ.createResultIQ(packet);
		Element iq = packet.getChildElement();
		
		String username = packet.getFrom().getNode();

		String type = iq.attributeValue("type");
		if (type == null) {
			makeError(reply, "No type attribute on the root iq element for myspace message");
			return reply;
		}
		
		if (type.equals("getName")) {
			handleGetName(iq, username, reply);
		} else if (type.equals("addBlogComment")) {
			handleAddBlogComment(iq, username, reply);
		} else if (type.equals("getBlogComments")) {
			handleGetBlogComments(iq, username, reply);
		} else if (type.equals("getContacts")) {
			handleGetContacts(iq, username, reply);
		} else if (type.equals("notifyContactComment")) {
			handleNotifyContactComment(iq, username, reply);					
		} else {
			makeError(reply, "Unknown myspace IQ type");

		}
		Log.debug("returning IQ reply " + reply);		
		return reply;		
	}

	private void handleNotifyContactComment(Element iq, String username, IQ reply) {
		MessengerGlueRemote glue = EJBUtil.defaultLookupRemote(MessengerGlueRemote.class);
		String mySpaceName = iq.attributeValue("name");
		Log.debug("notifing of contact comment with id " + mySpaceName);
		glue.notifyNewMySpaceContactComment(username, mySpaceName);
	}

	private void handleGetBlogComments(Element iq, String username, IQ reply) {
		MessengerGlueRemote glue = EJBUtil.defaultLookupRemote(MessengerGlueRemote.class);
		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("mySpaceInfo", "http://dumbhippo.com/protocol/myspace"); 
		reply.setChildElement(childElement);		
		for (MySpaceBlogCommentInfo comment : glue.getMySpaceBlogComments(username)) {
			Element commentElt = new DefaultElement("comment");
			childElement.add(commentElt);
			Element val = new DefaultElement("commentId");
			val.addText(Long.toString(comment.getCommentId()));
			commentElt.add(val);
			val = new DefaultElement("posterId");
			val.addText(Long.toString(comment.getPosterId()));
			commentElt.add(val);		
		}
	}
	
	private void handleGetContacts(Element iq, String username, IQ reply) {
		MessengerGlueRemote glue = EJBUtil.defaultLookupRemote(MessengerGlueRemote.class);
		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("mySpaceInfo", "http://dumbhippo.com/protocol/myspace"); 
		reply.setChildElement(childElement);		
		for (MySpaceContactInfo contact : glue.getContactMySpaceNames(username)) {
			Element contactElt = new DefaultElement("contact");
			childElement.add(contactElt);
			contactElt.addAttribute("name", contact.getUsername());
			contactElt.addAttribute("friendID", contact.getFriendId());
		}
	}	

	private void handleGetName(Element iq, String username, IQ reply) {
		MessengerGlueRemote glue = EJBUtil.defaultLookupRemote(MessengerGlueRemote.class);
		
		String name = glue.getMySpaceName(username);
		
		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("mySpaceInfo", "http://dumbhippo.com/protocol/myspace"); 
		childElement.addAttribute("mySpaceName", name);
		Log.debug("Returning myspace name: " + name);
		reply.setChildElement(childElement);
	}
	
	private void handleAddBlogComment(Element iq, String username, IQ reply) {
		MessengerGlueRemote glue = EJBUtil.defaultLookupRemote(MessengerGlueRemote.class);
		Long commentId = null;
		Long posterId = null;
		for (Object argObj : iq.elements()) {
        	Node node = (Node) argObj;
        	
        	Log.debug("parsing arg node " + node);
        	
        	if (node.getNodeType() == Node.ELEMENT_NODE) {
        		Element element = (Element) node;
        		
        		if (element.getName().equals("commentId")) {
        			commentId = Long.parseLong(element.getText());
        		} else if (element.getName().equals("posterId")) {
        			posterId = Long.parseLong(element.getText());
        		} else {
        			makeError(reply, "Unknown node type: " + element.getName());
        			return;
        		}
        	}
        }		
		if (commentId == null || posterId == null) {
			makeError(reply, "Missing parameter for AddBlogComment");
			return;		
		}
		Log.debug("adding blog comment with id " + commentId);
		glue.addMySpaceBlogComment(username, commentId, posterId);
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
}
