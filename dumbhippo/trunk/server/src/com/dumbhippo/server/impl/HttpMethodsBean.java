package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

@Stateless
public class HttpMethodsBean implements HttpMethods, Serializable {
	
	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(HttpMethodsBean.class);
	
	private static final long serialVersionUID = 0L;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PostingBoard postingBoard;

	@EJB
	private GroupSystem groupSystem;

	static private void startReturnObjectsXml(HttpResponseData contentType, XmlBuilder xml) {
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");

		xml.appendStandaloneFragmentHeader();
		
		xml.append("<objects>");
	}
	
	static private void endReturnObjectsXml(OutputStream out, XmlBuilder xml) throws IOException {
		xml.append("</objects>");
		
		out.write(xml.toString().getBytes());
	}
	
	static private void returnPersonsXml(XmlBuilder xml, Set<Person> persons) {
		if (persons != null) {
			for (Person p : persons) {
				xml.appendTextNode("person", null, "id", p.getId(), "display", p.getName().getFullName());
			}
		}		
	}
	
	static private void returnGroupsXml(XmlBuilder xml, Set<Group> groups) {
		if (groups != null) {
			for (Group g : groups) {
				xml.appendTextNode("group", null, "id", g.getId(), "display", g.getName());
			}
		}
	}
	
	static private void returnCompletionXml(XmlBuilder xml, GuidPersistable object, String completes) {
		xml.appendTextNode("completion", null, "id", object.getId(), "text", completes);
	}
	
	static private void returnObjects(OutputStream out, HttpResponseData contentType, Set<Person> persons, Set<Group> groups) throws IOException {
		XmlBuilder xml = new XmlBuilder();		

		startReturnObjectsXml(contentType, xml);
		
		returnPersonsXml(xml, persons);
		returnGroupsXml(xml, groups);
		
		endReturnObjectsXml(out, xml);
	}
	
	/* (non-Javadoc)
	 * @see com.dumbhippo.server.AjaxGlueHttp#getFriendCompletions(java.io.OutputStream, java.lang.String, java.lang.String)
	 */
	public void getFriendCompletions(OutputStream out, HttpResponseData contentType, Person user, String entryContents) throws IOException {
	
		XmlBuilder xml = new XmlBuilder();

		startReturnObjectsXml(contentType, xml);
			
		if (entryContents != null) {
			Set<Person> contacts = identitySpider.getContacts(user);
			Set<Group> groups = groupSystem.findGroups(user);

			// it's important that empty string returns all completions, otherwise
			// the arrow on the combobox doesn't drop down anything when it's empty
			
			for (Person c : contacts) {
				String completion = null;

				PersonView view = identitySpider.getViewpoint(user, c);
				String humanReadable = view.getHumanReadableName();
				EmailResource email = view.getEmail();
				if (humanReadable.startsWith(entryContents)) {
					completion = humanReadable;
				} else if (email.getEmail().startsWith(entryContents)) {
					completion = email.getEmail();
				} else if (c.getId().startsWith(entryContents)) {
					completion = c.getId();
				}
				
				if (completion != null) {
					returnPersonsXml(xml, Collections.singleton(c));
					returnCompletionXml(xml, c, completion);
				}
			}

			for (Group g : groups) {
				String completion = null;

				if (g.getName().startsWith(entryContents)) {
					completion = g.getName();
				} else if (g.getId().startsWith(entryContents)) {
					completion = g.getId();
				}
				
				if (completion != null) {
					returnGroupsXml(xml, Collections.singleton(g));
					returnCompletionXml(xml, g, completion);
				}
			}
		}

		endReturnObjectsXml(out, xml);
	}
	
	static private Set<String> splitIdList(String list) {
		Set<String> ret;
		
		// string.split returns a single empty string if the string we split is length 0, unfortunately
		if (list.length() > 0) {
			ret = new HashSet<String>(Arrays.asList(list.split(",")));
		} else {
			ret = Collections.emptySet();
		}
		
		return ret;
	}
	
	public void doShareLink(Person user, String url, String recipientIds, String description) throws ParseException, GuidNotFoundException {
		Set<String> recipientGuids = splitIdList(recipientIds);

		postingBoard.createURLPost(user, null, description, url, recipientGuids);
	}

	public void doCreateGroup(OutputStream out, HttpResponseData contentType, Person user, String name, String members) throws IOException, ParseException, GuidNotFoundException {
				
		Set<String> memberGuids = splitIdList(members);
		
		Set<Person> memberPeople = identitySpider.lookupGuidStrings(Person.class, memberGuids);
		
		Group group = groupSystem.createGroup(user, name);
		group.addMember(user);
		group.addMembers(memberPeople);
		
		returnObjects(out, contentType, null, Collections.singleton(group));
	}

	public void doAddContact(OutputStream out, HttpResponseData contentType, Person user, String email) throws IOException {
		EmailResource emailResource = identitySpider.getEmail(email);
		Person contact = identitySpider.createContact(user, emailResource);

		returnObjects(out, contentType, Collections.singleton(contact), null);
	}
}
