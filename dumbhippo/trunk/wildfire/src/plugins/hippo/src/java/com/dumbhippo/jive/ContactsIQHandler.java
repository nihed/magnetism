package com.dumbhippo.jive;

import java.util.List;
import java.util.Set;

import javax.ejb.EJB;

import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;

import com.dumbhippo.TypeUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.ViewStreamBuilder;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.ObjectView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.ViewStream;

/** 
 * IQ handler for getting / monitoring your social network (groups, contacts)
 * 
 * @author Havoc Pennington
 *
 */
@IQHandler(namespace=ContactsIQHandler.CONTACTS_NAMESPACE)
public class ContactsIQHandler extends AnnotatedIQHandler {
	static final String CONTACTS_NAMESPACE = "http://dumbhippo.com/protocol/contacts";
	
	@EJB
	private GroupSystem groupSystem;
	
	@EJB
	private PersonViewer personViewer;
	
	@EJB
	private ViewStreamBuilder viewStreamBuilder;
	
	public ContactsIQHandler() {
		super("Hippo contacts IQ Handler");
		Log.debug("creating ContactsIQHandler");
	}
	
	@IQMethod(name="contacts", type=IQ.Type.get)
	public void getContacts(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		List<PersonView> persons = personViewer.getContacts(viewpoint, viewpoint.getViewer(),
				0, -1);
		// Add the user themself to the list of returned contacts (whether or not the
		// viewer is in their own contact list getContacts() strips it out.)
		persons.add(personViewer.getPersonView(viewpoint, viewpoint.getViewer(), PersonViewExtra.CONTACT_STATUS));
		
		ViewStream stream = viewStreamBuilder.buildStream(viewpoint, TypeUtils.castList(ObjectView.class, persons));
		
		XmlBuilder xml = new XmlBuilder();
		
		xml.openElement("contacts",
			    "xmlns", CONTACTS_NAMESPACE);
		
		stream.writeToXmlBuilder(xml);
		
		xml.closeElement();
		
		reply.setChildElement(XmlParser.elementFromXml(xml.toString()));
	}
	
	@IQMethod(name="groups", type=IQ.Type.get)
	public void getGroups(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Set<GroupView> groups = groupSystem.findGroups(viewpoint, viewpoint.getViewer());
		
		ViewStream stream = viewStreamBuilder.buildStream(viewpoint, TypeUtils.castSet(ObjectView.class, groups));
		
		XmlBuilder xml = new XmlBuilder();
		
		xml.openElement("groups",
			    "xmlns", CONTACTS_NAMESPACE);
		
		stream.writeToXmlBuilder(xml);
		
		xml.closeElement();
		
		reply.setChildElement(XmlParser.elementFromXml(xml.toString()));
	}
}
