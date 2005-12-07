package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.jms.ObjectMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.botcom.BotTaskJoinRoom;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.persistence.AimResource;
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
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SigninSystem;
import com.dumbhippo.server.TokenExpiredException;
import com.dumbhippo.server.TokenSystem;
import com.dumbhippo.server.TokenUnknownException;
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
	
	@EJB
	private SigninSystem signinSystem;
	
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
	
	private void returnPersonsXml(XmlBuilder xml, Viewpoint viewpoint, Set<PersonView> persons) {
		if (persons != null) {
			for (PersonView p : persons) {				

				StringBuilder sb = new StringBuilder();

				String emailsStr = null;
				Collection<EmailResource> emails = p.getAllEmails();

				for (EmailResource e : emails) {
					sb.append(e.getEmail());
					sb.append(",");
				}
				if (sb.length() > 0) {
					sb.deleteCharAt(sb.length() - 1);
					emailsStr = sb.toString();
				}
				
				sb.setLength(0);
				
				String aimsStr = null;
				Collection<AimResource> aims = p.getAllAims();
				for (AimResource a : aims) {
					sb.append(a.getScreenName());
					sb.append(",");
				}
				if (sb.length() > 0) {
					sb.deleteCharAt(sb.length() - 1);
					aimsStr = sb.toString();
				}
				
				EmailResource primaryEmail = p.getEmail();
				AimResource primaryAim = p.getAim();
				
				String hasAccount = p.getUser() != null ? "true" : "false";
				
				String display = p.getName();
				if (p.getUser() != null && p.getUser().equals(viewpoint.getViewer())) {
					display = display + " (myself)";
				}
				
				xml.appendTextNode("person", null,
						"id", p.getContact() != null ? p.getContact().getId() : p.getUser().getId(),
						"display", display,
						"hasAccount", hasAccount,
						"email", primaryEmail != null ? primaryEmail.getEmail() : null,
						"aim", primaryAim != null ? primaryAim.getScreenName() : null,
						"emails", emailsStr,
						"aims", aimsStr);
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
					
					User user = member.getUser(); // can return null
					if (user != null && viewpoint.getViewer().equals(user))
						continue; // skip ourselves
					
					if (sampleMembers.length() > 0)
						sampleMembers.append(" ");
				
					String shortName = member.getTruncatedName();
					sampleMembers.append(shortName);
				}
				
				xml.appendTextNode("group", null, "id", g.getId(), "display", g.getName(), "sampleMembers", sampleMembers.toString(), "count", Integer.toString(members.size()));
			}
		}
	}
	
	private void returnObjects(OutputStream out, HttpResponseData contentType, Viewpoint viewpoint, Set<PersonView> persons, Set<Group> groups) throws IOException {
		XmlBuilder xml = new XmlBuilder();

		startReturnObjectsXml(contentType, xml);
		
		if (persons != null)
			returnPersonsXml(xml, viewpoint, persons);
		if (groups != null)
			returnGroupsXml(xml, viewpoint, groups);
		
		endReturnObjectsXml(out, xml);
	}

	public void getAddableContacts(OutputStream out, HttpResponseData contentType, User user, String groupId) throws IOException {
		Viewpoint viewpoint = new Viewpoint(user);
		
		Set<PersonView> persons = groupSystem.findAddableContacts(viewpoint, user, groupId, PersonViewExtra.ALL_RESOURCES);
		
		returnObjects(out, contentType, viewpoint, persons, null);
	}
	
	public void getContactsAndGroups(OutputStream out, HttpResponseData contentType, User user) throws IOException {
		Viewpoint viewpoint = new Viewpoint(user);
		
		Set<PersonView> persons = identitySpider.getContacts(viewpoint, user, true, PersonViewExtra.ALL_RESOURCES);
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
		PersonView contactView = identitySpider.getPersonView(viewpoint, contact, PersonViewExtra.ALL_RESOURCES);
		returnPersonsXml(xml, viewpoint, Collections.singleton(contactView));
		
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

		// FIXME if sending to a public group with secret=true, we want to expand the group instead of 
		// sending to the group ...
		PostVisibility visibility = secret ? PostVisibility.RECIPIENTS_ONLY : PostVisibility.ANONYMOUSLY_PUBLIC;
		
		// this is what can throw ParseException
		Set<GuidPersistable> recipients = identitySpider.lookupGuidStrings(GuidPersistable.class, recipientGuids);
		
		postingBoard.doLinkPost(user, visibility, title, description, url, recipients, false);
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
		String title = group.getName() + " (invitation from " + selfView.getName() + ")";
			
		PostVisibility visibility = group.getAccess() == GroupAccess.SECRET ? PostVisibility.RECIPIENTS_ONLY : PostVisibility.ANONYMOUSLY_PUBLIC;
		
		postingBoard.doLinkPost(user, visibility, title, description, url, recipients, true);		
	}
	
	public void doRenamePerson(User user, String name) {
		// We can't use merge() here because of a bug in Hiberaate with merge()
		// and the inverse side of a OneToOne relationship.
		// http://opensource2.atlassian.com/projects/hibernate/browse/HHH-1004
		User attachedUser = em.find(User.class, user.getId());
		attachedUser.setNickname(name);
	}
	
	public void doCreateGroup(OutputStream out, HttpResponseData contentType, User user, String name, String members, boolean secret) throws IOException, ParseException, GuidNotFoundException {
		Set<String> memberGuids = splitIdList(members);
		
		Set<Person> memberPeople = identitySpider.lookupGuidStrings(Person.class, memberGuids);
		
		Group group = groupSystem.createGroup(user, name, secret ? GroupAccess.PUBLIC_INVITE : GroupAccess.SECRET);
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
		PersonView contactView = identitySpider.getPersonView(viewpoint, contact, PersonViewExtra.ALL_RESOURCES);

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

	public void handleRedirect(User user, String url, String postId, String inviteKey) throws HumanVisibleException {
		
		InvitationToken invitation = null;
		
		if (user == null && inviteKey != null) {
			Token token = null;
			try {
				token = tokenSystem.getTokenByKey(inviteKey);
			} catch (TokenExpiredException e) {
				throw new HumanVisibleException("Your invitation to DumbHippo has expired! Ask the person who sent you this to invite you again.");
			} catch (TokenUnknownException e) {
				throw new HumanVisibleException("We can't find a valid invitation to DumbHippo for you. Ask a friend to invite you!");
			}
			assert token != null;
			if (token instanceof InvitationToken)
				invitation = (InvitationToken) token;
			else
				throw new HumanVisibleException("We can't find a valid invitation to DumbHippo for you. Ask a friend to invite you!");
			assert invitation != null;
		}
		
		// FIXME obviously we should redirect you to login and then come back...
		if (user == null && invitation == null) {
			throw new HumanVisibleException("Do you need to <a href=\"/home\">log in</a>?", true);
		}

		Post post;
		try {
			post = identitySpider.lookupGuidString(Post.class, postId);
		} catch (ParseException e) {
			throw new HumanVisibleException("Which post did you come from? (post's ID was \"" + XmlBuilder.escape(postId) + "\")", true);
		} catch (GuidNotFoundException e) {
			throw new HumanVisibleException("Which post did you come from? (post's ID was \"" + XmlBuilder.escape(postId) + "\")", true);
		}
		
		if (user != null) {
			
		}
		
		if (user != null) {
			postingBoard.postViewedBy(post.getId(), user);
		} else {
			logger.debug("not yet handling a merely-invited person hitting the redirect page");
		}
	}

	public void doSendLoginLinkEmail(String address) throws IOException, HumanVisibleException {
		signinSystem.sendSigninLink(address);
	}

	public void doSendLoginLinkAim(String address) throws IOException, HumanVisibleException {
		signinSystem.sendSigninLink(address);
	}

	public void doSetAccountDisabled(User user, boolean disabled) throws IOException, HumanVisibleException {
		identitySpider.setAccountDisabled(user, disabled);
	}
	
	public void doRequestJoinRoom(String chatRoomName) throws IOException {	
		// TODO: specify a specific bot to handle request, or somehow make sure that 
		//  all join requests for a particular room get directed to the same bot.  
		//  Otherwise the bots will take over and humans with perish from the earth.
		BotTaskJoinRoom joinTask = new BotTaskJoinRoom(null, chatRoomName);
		JmsProducer producer = new JmsProducer(AimQueueConsumerBean.OUTGOING_QUEUE, true);
		ObjectMessage jmsMessage = producer.createObjectMessage(joinTask);
		producer.send(jmsMessage);
	}

	public void doSetPassword(User user, String password) throws IOException, HumanVisibleException {
		password = password.trim();
		if (password.length() == 0) {
			password = null;
		}
		signinSystem.setPassword(user, password);
	}
}
