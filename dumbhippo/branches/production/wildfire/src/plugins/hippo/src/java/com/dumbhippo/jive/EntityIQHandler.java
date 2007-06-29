package com.dumbhippo.jive;

import java.util.Collections;
import java.util.Set;

import javax.ejb.EJB;

import org.jivesoftware.util.Log;
import org.xmpp.packet.Message;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.live.LiveEventListener;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.UserChangedEvent;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.UserViewpoint;

/** 
 * Notifies when an entity changes; currently this is just self, although
 * it is intended for future expansion where a client could register
 * interest in a set of entities.
 *
 */
@IQHandler(namespace=EntityIQHandler.ENTITY_NAMESPACE)
public class EntityIQHandler extends AnnotatedIQHandler implements LiveEventListener<UserChangedEvent> {
	static final String ENTITY_NAMESPACE = "http://dumbhippo.com/protocol/entity";
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PersonViewer personViewer;
	
	public EntityIQHandler() {
		super("Hippo entity IQ Handler");
		Log.debug("creating EntityIQHandler");
	}
	
	public void onEvent(UserChangedEvent event) {
		User user = identitySpider.lookupUser(event.getUserId());
		PersonView view = personViewer.getPersonView(new UserViewpoint(user), user);
		
		Set<Guid> interested = Collections.singleton(user.getGuid());
		
		Message message = new Message();
		message.setType(Message.Type.headline);		
		XmlBuilder xml = new XmlBuilder();
		xml.openElement("entitiesChanged", "xmlns", ENTITY_NAMESPACE);		
		view.writeToXmlBuilder(xml);
		xml.closeElement();
		message.getElement().add(XmlParser.elementFromXml(xml.toString()));
		
		for (Guid guid : interested)
			MessageSender.getInstance().sendMessage(guid, message);		
	}
	
	@Override
	public void start() throws IllegalStateException {
		super.start();
		LiveState.addEventListener(UserChangedEvent.class, this);		
	}

	@Override
	public void stop() {
		super.stop();	
		LiveState.removeEventListener(UserChangedEvent.class, this);			
	}		
}
