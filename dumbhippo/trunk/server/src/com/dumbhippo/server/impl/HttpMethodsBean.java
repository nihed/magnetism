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
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;

import com.dumbhippo.FullName;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.Invitation;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.RedirectException;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

@Stateless
public class HttpMethodsBean implements HttpMethods, Serializable {
	
	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(HttpMethodsBean.class);
	
	private static final long serialVersionUID = 0L;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PostingBoard postingBoard;

	@EJB
	private GroupSystem groupSystem;

	@EJB
	private InvitationSystem invitationSystem;
	
	private void startReturnObjectsXml(HttpResponseData contentType, XmlBuilder xml) {
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");

		xml.appendStandaloneFragmentHeader();
		
		xml.append("<objects>");
	}
	
	private void endReturnObjectsXml(OutputStream out, XmlBuilder xml) throws IOException {
		xml.append("</objects>");
		
		out.write(xml.toString().getBytes());
	}
	
	private void returnPersonsXml(XmlBuilder xml, Person user, Set<Person> persons) {
		if (persons != null) {
			for (Person p : persons) {
				// FIXME this is mind-blowingly inefficient
				PersonView view = identitySpider.getViewpoint(user, p);
				String humanReadable = view.getHumanReadableName();
				xml.appendTextNode("person", null, "id", p.getId(), "display", humanReadable);
			}
		}		
	}
	
	private void returnGroupsXml(XmlBuilder xml, Person user, Set<Group> groups) {
		if (groups != null) {
			for (Group g : groups) {
				xml.appendTextNode("group", null, "id", g.getId(), "display", g.getName());
			}
		}
	}
	
	private void returnCompletionXml(XmlBuilder xml, GuidPersistable object, String completes) {
		xml.appendTextNode("completion", null, "id", object.getId(), "text", completes);
	}
	
	private void returnObjects(OutputStream out, HttpResponseData contentType, Person user, Set<Person> persons, Set<Group> groups) throws IOException {
		XmlBuilder xml = new XmlBuilder();		

		startReturnObjectsXml(contentType, xml);
		
		returnPersonsXml(xml, user, persons);
		returnGroupsXml(xml, user, groups);
		
		endReturnObjectsXml(out, xml);
	}

	private void implementCompletions(OutputStream out, HttpResponseData contentType, Person user,
			String entryContents, boolean createContact) throws IOException {

		XmlBuilder xml = new XmlBuilder();

		startReturnObjectsXml(contentType, xml);

		boolean hadCompletion = false;
		if (entryContents != null) {
			Set<Person> contacts = identitySpider.getContacts(user);
			Set<Group> groups = groupSystem.findGroups(user);

			// it's important that empty string returns all completions,
			// otherwise
			// the arrow on the combobox doesn't drop down anything when it's
			// empty

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
					hadCompletion = true;
					returnPersonsXml(xml, user, Collections.singleton(c));
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
					hadCompletion = true;
					returnGroupsXml(xml, user, Collections.singleton(g));
					returnCompletionXml(xml, g, completion);
				}
			}
		}

		if (createContact && !hadCompletion) {
			// Create a new contact to be the completion
			EmailResource email = identitySpider.getEmail(entryContents);
			Person contact = identitySpider.createContact(user, email);
			returnPersonsXml(xml, user, Collections.singleton(contact));
			returnCompletionXml(xml, contact, entryContents);
		}
		
		endReturnObjectsXml(out, xml);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dumbhippo.server.AjaxGlueHttp#getFriendCompletions(java.io.OutputStream,
	 *      java.lang.String, java.lang.String)
	 */
	public void getFriendCompletions(OutputStream out, HttpResponseData contentType, Person user, String entryContents) throws IOException {
		implementCompletions(out, contentType, user, entryContents, false);
	}

	public void doFriendCompletionsOrCreateContact(OutputStream out, HttpResponseData contentType, Person user, String entryContents) throws IOException {
		implementCompletions(out, contentType, user, entryContents, true);
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

	
	public void doRenamePerson(Person user, String name) {
		FullName fullname = FullName.parseHumanString(name);
		user.setName(fullname);
		em.merge(user);
	}
	
	public void doCreateGroup(OutputStream out, HttpResponseData contentType, Person user, String name, String members) throws IOException, ParseException, GuidNotFoundException {
				
		Set<String> memberGuids = splitIdList(members);
		
		Set<Person> memberPeople = identitySpider.lookupGuidStrings(Person.class, memberGuids);
		
		Group group = groupSystem.createGroup(user, name);
		group.addMember(user);
		group.addMembers(memberPeople);
		
		returnObjects(out, contentType, user, null, Collections.singleton(group));
	}

	public void doAddContact(OutputStream out, HttpResponseData contentType, Person user, String email) throws IOException {
		EmailResource emailResource = identitySpider.getEmail(email);
		Person contact = identitySpider.createContact(user, emailResource);

		returnObjects(out, contentType, user, Collections.singleton(contact), null);
	}
	
	public void doJoinGroup(Person user, String groupId) {
		try {
			Group group = identitySpider.lookupGuidString(Group.class, groupId);
			groupSystem.addMember(user, group, user);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (IdentitySpider.GuidNotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}
	
	public void doLeaveGroup(Person user, String groupId) {
		try {
			Group group = identitySpider.lookupGuidString(Group.class, groupId);
			groupSystem.removeMember(user, group, user);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (IdentitySpider.GuidNotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}
	
	public void doAddContactPerson(Person user, String contactId) {
		try {
			Person contact = identitySpider.lookupGuidString(Person.class, contactId);
			identitySpider.addContactPerson(user, contact);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (IdentitySpider.GuidNotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}
	
	public void doRemoveContactPerson(Person user, String contactId) {
		try {
			Person contact = identitySpider.lookupGuidString(Person.class, contactId);
			identitySpider.removeContactPerson(user, contact);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (IdentitySpider.GuidNotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}

	public void handleRedirect(Person user, String url, String postId, String inviteKey) throws RedirectException {
		
		Invitation invitation = null;
		
		if (user == null && inviteKey != null) {
			invitation = invitationSystem.lookupInvitationByKey(inviteKey);
		}
		
		// FIXME obviously we should redirect you to login and then come back...
		if (user == null && invitation == null) {
			throw new RedirectException("Do you need to <a href=\"/jsf/addclient.faces\">log in</a>?");
		}

		Post post;
		try {
			post = identitySpider.lookupGuidString(Post.class, postId);
		} catch (ParseException e) {
			throw new RedirectException("Which post did you come from? (post's ID was \"" + XmlBuilder.escape(postId) + "\")");
		} catch (GuidNotFoundException e) {
			throw new RedirectException("Which post did you come from? (post's ID was \"" + XmlBuilder.escape(postId) + "\")");
		}
		LinkResource link = identitySpider.getLink(url);
		
		logger.debug("FIXME mark " + (user != null ? user : invitation) + " as having gone to " + post + " specifically link " + url);
	}
}
