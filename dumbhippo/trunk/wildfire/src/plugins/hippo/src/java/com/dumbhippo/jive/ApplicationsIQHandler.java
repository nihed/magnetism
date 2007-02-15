package com.dumbhippo.jive;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.EJB;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.server.applications.ApplicationUsageProperties;
import com.dumbhippo.server.views.UserViewpoint;

@IQHandler(namespace=ApplicationsIQHandler.APPLICATIONS_NAMESPACE)
public class ApplicationsIQHandler extends AnnotatedIQHandler {
	static final String APPLICATIONS_NAMESPACE = "http://dumbhippo.com/protocol/applications"; 
	
	@EJB
	private ApplicationSystem applicationSystem;

	public ApplicationsIQHandler() {
		super("Hippo application usage IQ Handler");
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
}
