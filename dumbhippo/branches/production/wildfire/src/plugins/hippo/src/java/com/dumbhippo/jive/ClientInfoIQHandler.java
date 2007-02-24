package com.dumbhippo.jive;

import javax.ejb.EJB;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;

import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.views.UserViewpoint;

@IQHandler(namespace=ClientInfoIQHandler.CLIENT_INFO_NAMESPACE)
public class ClientInfoIQHandler extends AnnotatedIQHandler {
	static final String CLIENT_INFO_NAMESPACE = "http://dumbhippo.com/protocol/clientinfo";
	
	@EJB
	AccountSystem accountSystem;
	
	@EJB
	TransactionRunner runner;

	public ClientInfoIQHandler() {
		super("Dumbhippo clientInfo IQ Handler");
		Log.debug("creating ClientInfoIQHandler");
	}

	@IQMethod(name="clientInfo", type=IQ.Type.get)
	public void getClientInfo(final UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Element child = request.getChildElement();
		
		final String platform = child.attributeValue("platform");
        if (platform == null) {
        	throw IQException.createBadRequest("clientInfo IQ missing platform attribute");
        }

        // optional distribution info
        final String distribution = child.attributeValue("distribution");
        
		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("clientInfo", CLIENT_INFO_NAMESPACE);
		if (platform.equals("windows")) {
			childElement.addAttribute("minimum", JiveGlobals.getXMLProperty("dumbhippo.client.windows.minimum"));
			childElement.addAttribute("current", JiveGlobals.getXMLProperty("dumbhippo.client.windows.current"));
			childElement.addAttribute("download", JiveGlobals.getXMLProperty("dumbhippo.client.windows.download"));
		} else if (platform.equals("linux")) {
			childElement.addAttribute("minimum", JiveGlobals.getXMLProperty("dumbhippo.client.linux.minimum"));
			childElement.addAttribute("current", JiveGlobals.getXMLProperty("dumbhippo.client.linux.current"));
			
			// Linux does not use a download url here, because it doesn't auto-download the new package
			// (perhaps it should, but for now it doesn't). We set the url anyway because 1) we might 
			// want to use it later and 2) older clients will break if the attribute is missing.
			if (distribution != null && distribution.equals("fedora5")) {
				childElement.addAttribute("download", JiveGlobals.getXMLProperty("dumbhippo.client.fedora5.download"));
			} else if (distribution != null && distribution.equals("fedora6")) {
				childElement.addAttribute("download", JiveGlobals.getXMLProperty("dumbhippo.client.fedora6.download"));
			} else {
				// We set the attribute anyway so older client versions don't throw an error.
				childElement.addAttribute("download", "http://example.com/notused");
			}
		} else {
			throw IQException.createBadRequest("clientInfo IQ: unrecognized platform: '" + platform + "'");
		}
		
		reply.setChildElement(childElement);
		
		runner.runTaskOnTransactionCommit(new Runnable() {
			public void run() {
				accountSystem.updateClientInfo(viewpoint, platform, distribution);
			}
		});
	}
}
