package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.User;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.PromotionCode;
import com.dumbhippo.server.SigninSystem;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class HttpMethodsBean implements HttpMethods, Serializable {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup
			.getLogger(HttpMethodsBean.class);

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
	private SigninSystem signinSystem;

	@EJB
	private MusicSystem musicSystem;

	@EJB
	private InvitationSystem invitationSystem;
	
	private void startReturnObjectsXml(HttpResponseData contentType,
			XmlBuilder xml) {
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");

		xml.appendStandaloneFragmentHeader();

		xml.append("<objects>");
	}

	private void endReturnObjectsXml(OutputStream out, XmlBuilder xml)
			throws IOException {
		xml.append("</objects>");

		out.write(xml.getBytes());
	}

	private void returnPersonsXml(XmlBuilder xml, Viewpoint viewpoint,
			Set<PersonView> persons) {
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
				if (p.getUser() != null
						&& p.getUser().equals(viewpoint.getViewer())) {
					display = display + " (myself)";
				}

				xml.appendTextNode("person", null, "id",
						p.getContact() != null ? p.getContact().getId() : p
								.getUser().getId(), "contactId",
						p.getContact() != null ? p.getContact().getId() : "",
						"userId", p.getUser() != null ? p.getUser().getId()
								: "", "display", display, "hasAccount",
						hasAccount, "email",
						primaryEmail != null ? primaryEmail.getEmail() : null,
						"aim", primaryAim != null ? primaryAim.getScreenName()
								: null, "emails", emailsStr, "aims", aimsStr);
			}
		}
	}

	private void returnGroupsXml(XmlBuilder xml, Viewpoint viewpoint,
			Set<Group> groups) {
		if (groups != null) {
			for (Group g : groups) {

				// FIXME with the right database query we can avoid getting
				// *all* the members to
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

				xml.appendTextNode("group", null, "id", g.getId(), "display", g
						.getName(), "sampleMembers", sampleMembers.toString(),
						"count", Integer.toString(members.size()));
			}
		}
	}

	private void returnObjects(OutputStream out, HttpResponseData contentType,
			Viewpoint viewpoint, Set<PersonView> persons, Set<Group> groups)
			throws IOException {
		XmlBuilder xml = new XmlBuilder();

		startReturnObjectsXml(contentType, xml);

		if (persons != null)
			returnPersonsXml(xml, viewpoint, persons);
		if (groups != null)
			returnGroupsXml(xml, viewpoint, groups);

		endReturnObjectsXml(out, xml);
	}

	public void getAddableContacts(OutputStream out,
			HttpResponseData contentType, User user, String groupId)
			throws IOException {
		Viewpoint viewpoint = new Viewpoint(user);

		Set<PersonView> persons = groupSystem.findAddableContacts(viewpoint,
				user, groupId, PersonViewExtra.ALL_RESOURCES);

		returnObjects(out, contentType, viewpoint, persons, null);
	}

	public void getContactsAndGroups(OutputStream out,
			HttpResponseData contentType, User user) throws IOException {
		Viewpoint viewpoint = new Viewpoint(user);

		Set<PersonView> persons = identitySpider.getContacts(viewpoint, user,
				true, PersonViewExtra.ALL_RESOURCES);
		Set<Group> groups = groupSystem.findRawGroups(viewpoint, user);

		returnObjects(out, contentType, viewpoint, persons, groups);
	}

	public void doCreateOrGetContact(OutputStream out,
			HttpResponseData contentType, User user, String email)
			throws IOException {
		XmlBuilder xml = new XmlBuilder();
		Viewpoint viewpoint = new Viewpoint(user);

		startReturnObjectsXml(contentType, xml);

		EmailResource resource = identitySpider.getEmail(email);
		Person contact = identitySpider.createContact(user, resource);
		PersonView contactView = identitySpider.getPersonView(viewpoint,
				contact, PersonViewExtra.ALL_RESOURCES);
		returnPersonsXml(xml, viewpoint, Collections.singleton(contactView));

		endReturnObjectsXml(out, xml);
	}

	static private Set<String> splitIdList(String list) {
		Set<String> ret;

		// string.split returns a single empty string if the string we split is
		// length 0, unfortunately
		if (list.length() > 0) {
			ret = new HashSet<String>(Arrays.asList(list.split(",")));
		} else {
			ret = Collections.emptySet();
		}

		return ret;
	}

	public void doShareLink(User user, String title, String url,
			String recipientIds, String description, boolean secret,
			String postInfoXml) throws ParseException, NotFoundException,
			SAXException, MalformedURLException {
		Set<String> recipientGuids = splitIdList(recipientIds);

		// FIXME if sending to a public group with secret=true, we want to
		// expand the group instead of
		// sending to the group ...
		PostVisibility visibility = secret ? PostVisibility.RECIPIENTS_ONLY
				: PostVisibility.ANONYMOUSLY_PUBLIC;

		PostInfo info;
		if (postInfoXml != null)
			info = PostInfo.parse(postInfoXml);
		else
			info = null;

		// this is what can throw ParseException
		Set<GuidPersistable> recipients = identitySpider.lookupGuidStrings(
				GuidPersistable.class, recipientGuids);

		URL urlObject = postingBoard.parsePostURL(url);

		postingBoard.doLinkPost(user, visibility, title, description,
				urlObject, recipients, false, info);
	}

	public void doShareGroup(User user, String groupId, String recipientIds,
			String description) throws ParseException, NotFoundException {
		Viewpoint viewpoint = new Viewpoint(user);

		Set<String> recipientGuids = splitIdList(recipientIds);

		Group group = groupSystem.lookupGroupById(viewpoint, groupId);
		if (group == null)
			throw new RuntimeException("No such group");

		// this is what can throw ParseException
		Set<GuidPersistable> recipients = identitySpider.lookupGuidStrings(
				GuidPersistable.class, recipientGuids);

		postingBoard.doShareGroupPost(user, group, description, recipients,
				true);
	}

	public void doRenamePerson(User user, String name) {
		// We can't use merge() here because of a bug in Hiberaate with merge()
		// and the inverse side of a OneToOne relationship.
		// http://opensource2.atlassian.com/projects/hibernate/browse/HHH-1004
		User attachedUser = em.find(User.class, user.getId());
		attachedUser.setNickname(name);
	}

	public void doCreateGroup(OutputStream out, HttpResponseData contentType,
			User user, String name, String members, boolean secret)
			throws IOException, ParseException, NotFoundException {
		Set<String> memberGuids = splitIdList(members);

		Set<Person> memberPeople = identitySpider.lookupGuidStrings(
				Person.class, memberGuids);

		Group group = groupSystem.createGroup(user, name,
				secret ? GroupAccess.PUBLIC_INVITE : GroupAccess.SECRET);
		for (Person p : memberPeople)
			groupSystem.addMember(user, group, p);

		Viewpoint viewpoint = new Viewpoint(user);
		returnObjects(out, contentType, viewpoint, null, Collections
				.singleton(group));
	}

	public void doAddMembers(OutputStream out, HttpResponseData contentType,
			User user, String groupId, String memberIds) throws IOException,
			ParseException, NotFoundException {
		Viewpoint viewpoint = new Viewpoint(user);

		Set<String> memberGuids = splitIdList(memberIds);

		Set<Person> memberPeople = identitySpider.lookupGuidStrings(
				Person.class, memberGuids);

		Group group = groupSystem.lookupGroupById(viewpoint, groupId);
		if (group == null)
			throw new RuntimeException("No such group");
		for (Person p : memberPeople)
			groupSystem.addMember(user, group, p);

		returnObjects(out, contentType, viewpoint, null, Collections
				.singleton(group));
	}

	public void doAddContact(OutputStream out, HttpResponseData contentType,
			User user, String email) throws IOException {
		EmailResource emailResource = identitySpider.getEmail(email);
		Contact contact = identitySpider.createContact(user, emailResource);
		Viewpoint viewpoint = new Viewpoint(user);
		PersonView contactView = identitySpider.getPersonView(viewpoint,
				contact, PersonViewExtra.ALL_RESOURCES);

		returnObjects(out, contentType, viewpoint, Collections
				.singleton(contactView), null);
	}

	public void doJoinGroup(User user, String groupId) {
		Viewpoint viewpoint = new Viewpoint(user);
		try {
			Group group = groupSystem.lookupGroupById(viewpoint, groupId);
			groupSystem.addMember(user, group, user);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public void doLeaveGroup(User user, String groupId) {
		Viewpoint viewpoint = new Viewpoint(user);
		try {
			Group group = groupSystem.lookupGroupById(viewpoint, groupId);
			groupSystem.removeMember(user, group, user);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public void doAddContactPerson(User user, String contactId) {
		try {
			Person contact = identitySpider.lookupGuidString(Person.class,
					contactId);
			identitySpider.addContactPerson(user, contact);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (NotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}

	public void doRemoveContactPerson(User user, String contactId) {
		try {
			Person contact = identitySpider.lookupGuidString(Person.class,
					contactId);
			identitySpider.removeContactPerson(user, contact);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (NotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}

	public void doSendLoginLinkEmail(String address) throws IOException,
			HumanVisibleException {
		signinSystem.sendSigninLink(address);
	}

	public void doSendLoginLinkAim(String address) throws IOException,
			HumanVisibleException {
		signinSystem.sendSigninLink(address);
	}

	public void doSetAccountDisabled(User user, boolean disabled)
			throws IOException, HumanVisibleException {
		identitySpider.setAccountDisabled(user, disabled);
	}

	public void doSetPassword(User user, String password) throws IOException,
			HumanVisibleException {
		password = password.trim();
		if (password.length() == 0) {
			password = null;
		}
		signinSystem.setPassword(user, password);
	}

	public void doSetMySpaceName(User user, String name) throws IOException {
		identitySpider.setMySpaceName(user, name);
	}

	public void doSetMusicSharingEnabled(User user, boolean enabled)
			throws IOException {
		identitySpider.setMusicSharingEnabled(user, enabled);
	}

	public void getCurrentTrack(OutputStream out, HttpResponseData contentType,
			String who) throws IOException {
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");

		User whoUser;
		try {
			whoUser = identitySpider.lookupGuidString(User.class, who);
		} catch (ParseException e) {
			throw new RuntimeException("bad guid", e);
		} catch (NotFoundException e) {
			throw new RuntimeException("no such person", e);
		}
		TrackView tv;
		try {
			// FIXME this is from the system viewpoint for now, but
			// should really be from an "anonymous" viewpoint
			tv = musicSystem.getCurrentTrackView(null, whoUser);
		} catch (NotFoundException e) {
			throw new RuntimeException("no current track for this person", e);
		}
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		xml.openElement("song");
		xml.appendTextNode("image", tv.getSmallImageUrl());
		xml.appendTextNode("songTitle", tv.getName());
		xml.appendTextNode("artist", tv.getArtist());
		xml.appendTextNode("album", tv.getAlbum());
		xml.closeElement();
		out.write(xml.getBytes());
		out.flush();
	}
	
	public void doInviteSelf(OutputStream out, HttpResponseData contentType, String address, String promotion) throws IOException {
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");

		String note = null;
		
		Character character;
		
		try {
			PromotionCode code = PromotionCode.check(promotion);
			switch (code) {
			case MUSIC_INVITE_PAGE_200602:
				character = Character.MUSIC_GEEK;
				break;
			default:
				character = null;
				break;
			}
		
		} catch (NotFoundException e) {
			character = null;
		}
		
		if (character == null) {
			note = "The limited-time offer has expired!";
		} else {
			User inviter = identitySpider.getCharacter(character);
			
			if (!inviter.getAccount().canSendInvitations(1)) {
				note = "Someone got there first! No more invitations available right now.";
			} else {
				// this does NOT check whether the account has invitations left,
				// that's why we do it above. 
				note = invitationSystem.sendEmailInvitation(inviter, address,
							"Welcome!", "Thanks for inviting yourself.");
				if (note == null)
					note = "Your invitation is on its way (check your email)";
			}
		}
		
		if (note == null)
			throw new RuntimeException("bug! note was null in InviteSelf");
		
		logger.debug("invite self message: '{}'", note);
		
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		xml.openElement("inviteSelfReply");
		xml.appendTextNode("message", note);
		xml.closeElement();
		out.write(xml.getBytes());
		out.flush();
	}
}
