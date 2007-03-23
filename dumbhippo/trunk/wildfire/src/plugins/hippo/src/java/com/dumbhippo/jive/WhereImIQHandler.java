package com.dumbhippo.jive;

import java.util.Collections;
import java.util.Set;

import javax.ejb.EJB;

import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.live.ExternalAccountChangedEvent;
import com.dumbhippo.live.LiveEventListener;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.UserViewpoint;

/** 
 * IQ handler for getting external account locations
 * 
 * @author Colin Walters
 *
 */
@IQHandler(namespace=WhereImIQHandler.WHEREIM_NAMESPACE)
public class WhereImIQHandler extends AnnotatedIQHandler implements LiveEventListener<ExternalAccountChangedEvent> {
	static final String WHEREIM_NAMESPACE = "http://dumbhippo.com/protocol/whereim";
	
	@EJB
	private ExternalAccountSystem externalAccountSystem;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PersonViewer personViewer;
	
	public WhereImIQHandler() {
		super("Hippo WhereIm IQ Handler");
	}
	
	private Element externalAccountViewsToElement(Set<ExternalAccountView> views) {

		XmlBuilder xml = new XmlBuilder();
		
		xml.openElement("whereim", "xmlns", WHEREIM_NAMESPACE);
		
		for (ExternalAccountView external : views) {
			external.writeToXmlBuilder(xml);
		}
		
		xml.closeElement();		
		
		return XmlParser.elementFromXml(xml.toString());
	}
	
	@IQMethod(name="whereim", type=IQ.Type.get)
	public void getWhereIm(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		String userGuid = request.getChildElement().attributeValue("who");
		PersonView personView;
		User user = null;
		
		if (userGuid == null)
			user = viewpoint.getViewer();
		else {
			try {
				user = identitySpider.lookupGuidString(User.class, userGuid);
			} catch (ParseException e) {
	        	throw IQException.createBadRequest("Invalid user");				
			} catch (NotFoundException e) {
	        	throw IQException.createBadRequest("Invalid user");				
			}
		}
		
		personView = personViewer.getPersonView(viewpoint, user, PersonViewExtra.EXTERNAL_ACCOUNTS, PersonViewExtra.CONTACT_STATUS);
		
		Set<ExternalAccountView> externalAccountViews = personView.getExternalAccountViews();		

		reply.setChildElement(externalAccountViewsToElement(externalAccountViews));
	}

	public void onEvent(ExternalAccountChangedEvent event) {
		User user = identitySpider.lookupUser(event.getUserId());
		UserViewpoint viewpoint = new UserViewpoint(user);		
		ExternalAccount acct;
		try {
			acct = externalAccountSystem.lookupExternalAccount(viewpoint, user, event.getType());
		} catch (NotFoundException e) {
			throw new RuntimeException("Couldn't find changed external account for user " + user, e);
		}
		ExternalAccountView view = externalAccountSystem.getExternalAccountView(viewpoint, 
				                                                                acct);

		Message message = new Message();
		message.setType(Message.Type.headline);
		message.getElement().add(externalAccountViewsToElement(Collections.singleton(view)));
		MessageSender.getInstance().sendMessage(user.getGuid(), message);		
	}
	
	@Override
	public void start() throws IllegalStateException {
		super.start();
		LiveState.addEventListener(ExternalAccountChangedEvent.class, this);		
	}

	@Override
	public void stop() {
		super.stop();	
		LiveState.removeEventListener(ExternalAccountChangedEvent.class, this);			
	}		
}
