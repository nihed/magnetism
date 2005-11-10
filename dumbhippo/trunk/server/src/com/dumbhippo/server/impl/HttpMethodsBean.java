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
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.Token;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.RedirectException;
import com.dumbhippo.server.TokenSystem;
import com.dumbhippo.server.Viewpoint;
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
	private Configuration configuration;

	@EJB
	private TokenSystem tokenSystem;
	
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
	
	private void returnPersonsXml(XmlBuilder xml, Set<PersonView> persons) {
		if (persons != null) {
			for (PersonView p : persons) {
				EmailResource email = p.getEmail();
				String hasAccount = p.getUser() != null ? "true" : "false";
				if (email != null) {
					xml.appendTextNode("person", null, "id", p.getPerson().getId(), "display", p.getHumanReadableName(),
							"hasAccount", hasAccount,
							"email", email.getEmail());
				} else {
					xml.appendTextNode("person", null, "id", p.getPerson().getId(), "display", p.getHumanReadableName(),
							"hasAccount", hasAccount);
				}
			}
		}
	}
	
	private void returnGroupsXml(XmlBuilder xml, Viewpoint viewpoint, Set<Group> groups) {
		if (groups != null) {
			for (Group g : groups) {
				
				// FIXME with the right database query we can avoid getting *all* the members to 
				// display just a few of them
				
				StringBuilder sampleMembers = new StringBuilder();
				Set<PersonView> members = groupSystem.getMembers(viewpoint, g);
				logger.debug(members.size() + " members of " + g.getName());
				for (PersonView member : members) {
					if (sampleMembers.length() > PersonView.MAX_SHORT_NAME_LENGTH * 5) {
						sampleMembers.append(" ...");
						break;
					} 
					
					if (sampleMembers.length() > 0)
						sampleMembers.append(" ");
				
					String shortName = member.getHumanReadableShortName();
					sampleMembers.append(shortName);
				}
				
				xml.appendTextNode("group", null, "id", g.getId(), "display", g.getName(), "sampleMembers", sampleMembers.toString());
			}
		}
	}
	
	private void returnObjects(OutputStream out, HttpResponseData contentType, Viewpoint viewpoint, Set<PersonView> persons, Set<Group> groups) throws IOException {
		XmlBuilder xml = new XmlBuilder();

		startReturnObjectsXml(contentType, xml);
		
		if (persons != null)
			returnPersonsXml(xml, persons);
		if (groups != null)
		returnGroupsXml(xml, viewpoint, groups);
		
		endReturnObjectsXml(out, xml);
	}

	public void getAddableContacts(OutputStream out, HttpResponseData contentType, User user, String groupId) throws IOException {
		Viewpoint viewpoint = new Viewpoint(user);
		
		Set<PersonView> persons = groupSystem.findAddableContacts(viewpoint, user, groupId);
		
		returnObjects(out, contentType, viewpoint, persons, null);
	}
	
	public void getContactsAndGroups(OutputStream out, HttpResponseData contentType, User user) throws IOException {
		Viewpoint viewpoint = new Viewpoint(user);
		
		Set<PersonView> persons = identitySpider.getContacts(viewpoint, user);
		Set<Group> groups = groupSystem.findRawGroups(viewpoint, user);
		
		returnObjects(out, contentType, viewpoint, persons, groups);
	}
	
	public void doCreateOrGetContact(OutputStream out, HttpResponseData contentType, User user,
			String email) throws IOException {

		XmlBuilder xml = new XmlBuilder();
		Viewpoint viewpoint = new Viewpoint(user);

		startReturnObjectsXml(contentType, xml);

		EmailResource resource = identitySpider.getEmail(email);
		Person contact = identitySpider.createContact(user, resource);
		PersonView contactView = identitySpider.getPersonView(viewpoint, contact);
		returnPersonsXml(xml, Collections.singleton(contactView));
		
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
	
	public void doShareLink(User user, String title, String url, String recipientIds, String description, boolean secret) throws ParseException, GuidNotFoundException {
		Set<String> recipientGuids = splitIdList(recipientIds);

		PostVisibility visibility = secret ? PostVisibility.RECIPIENTS_ONLY : PostVisibility.ANONYMOUSLY_PUBLIC;
		
		// this is what can throw ParseException
		Set<GuidPersistable> recipients = identitySpider.lookupGuidStrings(GuidPersistable.class, recipientGuids);
		
		postingBoard.doLinkPost(user, visibility, title, description, url, recipients);
	}

	public void doShareGroup(User user, String groupId, String recipientIds, String description) throws ParseException, GuidNotFoundException {
		Viewpoint viewpoint = new Viewpoint(user);
		
		Set<String> recipientGuids = splitIdList(recipientIds);
		
		Group group = groupSystem.lookupGroupById(viewpoint, groupId);
		if (group == null)
			throw new RuntimeException("No such group");
		
		// this is what can throw ParseException
		Set<GuidPersistable> recipients = identitySpider.lookupGuidStrings(GuidPersistable.class, recipientGuids);
		
		for (GuidPersistable r : recipients) {
			if (r instanceof Person)
				groupSystem.addMember(user, group, (Person)r);
			else
				throw new GuidNotFoundException(r.getId());
		}

		String baseurl = configuration.getProperty(HippoProperty.BASEURL);
		String url = baseurl + "/viewgroup?groupId=" + group.getId();
		
		PersonView selfView = identitySpider.getPersonView(viewpoint, user);
		String title = group.getName() + " (invitation from " + selfView.getHumanReadableName() + ")";
			
		PostVisibility visibility = group.getAccess() == GroupAccess.SECRET ? PostVisibility.RECIPIENTS_ONLY : PostVisibility.ANONYMOUSLY_PUBLIC;
		
		postingBoard.doLinkPost(user, visibility, title, description, url, recipients);		
	}
	
	public void doRenamePerson(User user, String name) {
		// We can't use merge() here because of a bug in Hiberaate with merge()
		// and the inverse side of a OneToOne relationship.
		// http://opensource2.atlassian.com/projects/hibernate/browse/HHH-1004
		User attachedUser = em.find(User.class, user.getId());
		FullName fullname = FullName.parseHumanString(name);
		attachedUser.setName(fullname);
	}
	
	public void doCreateGroup(OutputStream out, HttpResponseData contentType, User user, String name, String members) throws IOException, ParseException, GuidNotFoundException {
				
		Set<String> memberGuids = splitIdList(members);
		
		Set<Person> memberPeople = identitySpider.lookupGuidStrings(Person.class, memberGuids);
		
		Group group = groupSystem.createGroup(user, name);
		for (Person p : memberPeople)
			groupSystem.addMember(user, group, p);
		
		Viewpoint viewpoint = new Viewpoint(user);
		returnObjects(out, contentType, viewpoint, null, Collections.singleton(group));
	}

	public void doAddMembers(OutputStream out, HttpResponseData contentType, User user, String groupId, String memberIds)
			throws IOException, ParseException, GuidNotFoundException {
		Viewpoint viewpoint = new Viewpoint(user);
		
		Set<String> memberGuids = splitIdList(memberIds);
		
		Set<Person> memberPeople = identitySpider.lookupGuidStrings(Person.class, memberGuids);
		
		Group group = groupSystem.lookupGroupById(viewpoint, groupId);
		if (group == null)
			throw new RuntimeException("No such group");
		for (Person p : memberPeople)
			groupSystem.addMember(user, group, p);
		
		returnObjects(out, contentType, viewpoint, null, Collections.singleton(group));		
	}

	public void doAddContact(OutputStream out, HttpResponseData contentType, User user, String email) throws IOException {
		EmailResource emailResource = identitySpider.getEmail(email);
		Contact contact = identitySpider.createContact(user, emailResource);
		Viewpoint viewpoint = new Viewpoint(user);
		PersonView contactView = identitySpider.getPersonView(viewpoint, contact);

		returnObjects(out, contentType, viewpoint, Collections.singleton(contactView), null);
	}
	
	public void doJoinGroup(User user, String groupId) {
		Viewpoint viewpoint = new Viewpoint(user);
		Group group = groupSystem.lookupGroupById(viewpoint, groupId);
		if (group == null)
			throw new RuntimeException("No such group");
		groupSystem.addMember(user, group, user);
	}
	
	public void doLeaveGroup(User user, String groupId) {
		Viewpoint viewpoint = new Viewpoint(user);
		Group group = groupSystem.lookupGroupById(viewpoint, groupId);
		if (group == null)
			throw new RuntimeException("No such group");
		groupSystem.removeMember(user, group, user);
	}
	
	public void doAddContactPerson(User user, String contactId) {
		try {
			Person contact = identitySpider.lookupGuidString(Person.class, contactId);
			identitySpider.addContactPerson(user, contact);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (IdentitySpider.GuidNotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}
	
	public void doRemoveContactPerson(User user, String contactId) {
		try {
			Person contact = identitySpider.lookupGuidString(Person.class, contactId);
			identitySpider.removeContactPerson(user, contact);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (IdentitySpider.GuidNotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}

	public void handleRedirect(User user, String url, String postId, String inviteKey) throws RedirectException {
		
		InvitationToken invitation = null;
		
		if (user == null && inviteKey != null) {
			Token token = tokenSystem.lookupTokenByKey(inviteKey);
			if (token != null && token instanceof InvitationToken)
				invitation = (InvitationToken) token; 
		}
		
		// FIXME obviously we should redirect you to login and then come back...
		if (user == null && invitation == null) {
			throw new RedirectException("Do you need to <a href=\"/home\">log in</a>?");
		}

		Post post;
		try {
			post = identitySpider.lookupGuidString(Post.class, postId);
		} catch (ParseException e) {
			throw new RedirectException("Which post did you come from? (post's ID was \"" + XmlBuilder.escape(postId) + "\")");
		} catch (GuidNotFoundException e) {
			throw new RedirectException("Which post did you come from? (post's ID was \"" + XmlBuilder.escape(postId) + "\")");
		}
		
		if (user != null) {
			
		}
		
		if (user != null) {
			postingBoard.postClickedBy(post, user);
		} else {
			logger.debug("not yet handling a merely-invited person hitting the redirect page");
		}
	}
}
