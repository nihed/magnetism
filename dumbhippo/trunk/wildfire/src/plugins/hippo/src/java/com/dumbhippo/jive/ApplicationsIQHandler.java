package com.dumbhippo.jive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ejb.EJB;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.live.LiveEventListener;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.UserPrefChangedEvent;
import com.dumbhippo.persistence.Application;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.server.applications.ApplicationUsageProperties;
import com.dumbhippo.server.applications.ApplicationView;
import com.dumbhippo.server.views.UserViewpoint;

@IQHandler(namespace=ApplicationsIQHandler.APPLICATIONS_NAMESPACE)
public class ApplicationsIQHandler extends AnnotatedIQHandler  implements LiveEventListener<UserPrefChangedEvent> {
	static final String APPLICATIONS_NAMESPACE = "http://dumbhippo.com/protocol/applications";
	
	private static final int DEFAULT_ICON_SIZE = 32;
	
	@EJB
	private ApplicationSystem applicationSystem;

	@EJB
	private IdentitySpider identitySpider;
	
	public ApplicationsIQHandler() {
		super("Hippo application usage IQ Handler");
	}
	
	private void appendApplicationViews(Element elt, Collection<ApplicationView> views) {
		for (ApplicationView application : views) {
			XmlBuilder builder = new XmlBuilder();
			application.writeToXmlBuilder(builder);
			elt.add(XmlParser.elementFromXml(builder.toString()));			
		}		
	}

	@IQMethod(name="myTopApplications", type=IQ.Type.get)
	public void getMyTopApplications(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("myTopApplications", APPLICATIONS_NAMESPACE);
		
		Date since = applicationSystem.getMyApplicationUsageStart(viewpoint);
		if (since != null)
			childElement.addAttribute("since", "" + since.getTime());	
		
		childElement.addAttribute("enabled", ""+identitySpider.getApplicationUsageEnabled(viewpoint.getViewer()));

		Pageable<ApplicationView> pageable = new Pageable<ApplicationView>("applications");
		pageable.setPosition(0);
		pageable.setInitialPerPage(30);		
		applicationSystem.pageMyApplications(viewpoint, null, DEFAULT_ICON_SIZE, null, pageable);
		appendApplicationViews(childElement, pageable.getResults());
		
		reply.setChildElement(childElement);		
	}
	
	@IQMethod(name="pinned", type=IQ.Type.get)
	public void getPinned(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("pinned", APPLICATIONS_NAMESPACE);		
		List<Application> pinned = applicationSystem.getPinnedApplications(viewpoint.getViewer());
		List<ApplicationView> pinnedViewed = applicationSystem.viewApplications(viewpoint, pinned, DEFAULT_ICON_SIZE);
		appendApplicationViews(childElement, pinnedViewed);
		
		reply.setChildElement(childElement);			
	}

	@IQMethod(name="pinned", type=IQ.Type.set)
	public void setPinned(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Element child = request.getChildElement();
		List<String> applicationIds = new ArrayList<String>();
		
		String isPinnedFlag = child.attributeValue("isPinned");
		boolean isPinned = true;
		if (isPinnedFlag != null && isPinnedFlag.equals("false"))
			isPinned = false;
		
		final int limit = 100;  /* Arbitrary, mostly to mitigate broken clients */
		int count = 0;
		for (Iterator i = child.elementIterator(); i.hasNext(); ) {
			count++;			
			if (count > limit)
				break;
			Element subchild = (Element)i.next();
			if (subchild.getName().equals("appId")) {
				applicationIds.add(subchild.getText());
			}
		}
		
		applicationSystem.pinApplicationIds(viewpoint.getViewer(), applicationIds, isPinned);
	}

	@IQMethod(name="topApplications", type=IQ.Type.get)
	public void getTopApplications(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("topApplications", APPLICATIONS_NAMESPACE);
		
		int position = 0;
		String positionStr = request.getChildElement().attributeValue("start");
		if (positionStr != null)
			position = Integer.valueOf(positionStr);
		if (position < 0 || position > 4096) // Arbitrary but reasonable limit
			position = 0;
		
		Pageable<ApplicationView> pageable = new Pageable<ApplicationView>("applications");
		pageable.setPosition(position);
		pageable.setInitialPerPage(30);		
		applicationSystem.pagePopularApplications(null, 24, null, pageable);
		appendApplicationViews(childElement, pageable.getResults());
		
		reply.setChildElement(childElement);		
	}	
	
	@IQMethod(name="activeApplications", type=IQ.Type.set)
	public void setActiveApplications(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Element child = request.getChildElement();
		List<ApplicationUsageProperties> usages = new ArrayList<ApplicationUsageProperties>();
		
		for (Iterator i = child.elementIterator(); i.hasNext();) {
			Element subchild = (Element)i.next();
			if (subchild.getName().equals("application")) {
				ApplicationUsageProperties props = new ApplicationUsageProperties();
				
				String appId = subchild.attributeValue("appId");
				if (appId != null)
					props.setAppId(appId);

				String wmClass = subchild.attributeValue("wmClass");
				if (wmClass != null)
					props.setWmClass(wmClass);
				
				usages.add(props);
			}
		}
		
		applicationSystem.recordApplicationUsage(viewpoint, usages);
	}

	@IQMethod(name="titlePatterns", type=IQ.Type.get)
	public void getTitlePatterns(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("titlePatterns", APPLICATIONS_NAMESPACE);

		List<Application> applications = applicationSystem.getApplicationsWithTitlePatterns();
		for (Application application : applications) {
			Element applicationNode = childElement.addElement("application");
			applicationNode.addAttribute("appId", application.getId());
			applicationNode.setText(application.getTitlePatterns());
		}
		
		reply.setChildElement(childElement);
	}
	

	@Override
	public void start() throws IllegalStateException {
		super.start();
		LiveState.addEventListener(UserPrefChangedEvent.class, this);		
	}

	@Override
	public void stop() {
		super.stop();
		LiveState.removeEventListener(UserPrefChangedEvent.class, this);			
	}	

	public void onEvent(UserPrefChangedEvent event) {
		if (event.getKey().equals("applicationUsageEnabled") && event.getValue().equals("true")) {
			// When the user initially enables application tracking, we signal that user's
			// clients that they should temporarily upload application information with
			// high frequency, so the user gets immediate feedback about the way that
			// application usage tracking works.
			
			User user = identitySpider.lookupUser(event.getUserId()); 

			Message message = new Message();
			message.setType(Message.Type.headline);
			
			Document document = DocumentFactory.getInstance().createDocument();
			Element childElement = document.addElement("initialApplicationBurst", APPLICATIONS_NAMESPACE);

			message.getElement().add(childElement);
			MessageSender.getInstance().sendMessage(user.getGuid(), message);
		}
	}
}
