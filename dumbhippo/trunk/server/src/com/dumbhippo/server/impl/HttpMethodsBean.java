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

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.BeanUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.Configuration;
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
import com.dumbhippo.server.PostIndexer;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.PromotionCode;
import com.dumbhippo.server.SigninSystem;
import com.dumbhippo.server.TrackIndexer;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class HttpMethodsBean implements HttpMethods, Serializable {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup
			.getLogger(HttpMethodsBean.class);

	private static final long serialVersionUID = 0L;

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
	
	@EJB
	private ClaimVerifier claimVerifier;
	
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
				if (p.getUser() != null && viewpoint.isOfUser(p.getUser())) {
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
				//logger.debug(members.size() + " members of " + g.getName());
				for (PersonView member : members) {


					User user = member.getUser(); // can return null
					if (user != null && viewpoint.isOfUser(user))
						continue; // skip ourselves

					String shortName = member.getTruncatedName();					
					
					if (sampleMembers.length() + shortName.length() > PersonView.MAX_SHORT_NAME_LENGTH * 3) {
						sampleMembers.append(" ...");
						break;
					} else {	
						if (sampleMembers.length() > 0)
							sampleMembers.append(" ");
						sampleMembers.append(shortName);
					}
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
			HttpResponseData contentType, UserViewpoint viewpoint, String groupId)
			throws IOException {
		Set<PersonView> persons = groupSystem.findAddableContacts(viewpoint,
				viewpoint.getViewer(), groupId, PersonViewExtra.ALL_RESOURCES);

		returnObjects(out, contentType, viewpoint, persons, null);
	}

	public void getContactsAndGroups(OutputStream out,
			HttpResponseData contentType, UserViewpoint viewpoint) throws IOException {

		Set<PersonView> persons = identitySpider.getContacts(viewpoint, viewpoint.getViewer(),
				true, PersonViewExtra.ALL_RESOURCES);
		Set<Group> groups = groupSystem.findRawGroups(viewpoint, viewpoint.getViewer());

		returnObjects(out, contentType, viewpoint, persons, groups);
	}

	public void doCreateOrGetContact(OutputStream out,
			HttpResponseData contentType, UserViewpoint viewpoint, String email)
			throws IOException {
		XmlBuilder xml = new XmlBuilder();

		startReturnObjectsXml(contentType, xml);

		EmailResource resource = identitySpider.getEmail(email);
		Person contact = identitySpider.createContact(viewpoint.getViewer(), resource);
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

	public void doShareLink(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String title, String url,
			String recipientIds, String description, boolean isPublic,
			String postInfoXml) throws ParseException, NotFoundException,
			SAXException, MalformedURLException, IOException {
		Set<String> recipientGuids = splitIdList(recipientIds);

		PostVisibility visibility = isPublic ? PostVisibility.ATTRIBUTED_PUBLIC 
				: PostVisibility.RECIPIENTS_ONLY;

		PostInfo info;
		if (postInfoXml != null)
			info = PostInfo.parse(postInfoXml);
		else
			info = null;

		// this is what can throw ParseException
		Set<GuidPersistable> recipients = identitySpider.lookupGuidStrings(
				GuidPersistable.class, recipientGuids);

		URL urlObject = postingBoard.parsePostURL(url);

		Post post = postingBoard.doLinkPost(viewpoint.getViewer(), visibility, title, description,
							urlObject, recipients, false, info);
		XmlBuilder xml = new XmlBuilder();
		xml.openElement("post", "id", post.getId());
		xml.closeElement();
		out.write(xml.getBytes());
	}

	public void doShareGroup(UserViewpoint viewpoint, String groupId, String recipientIds,
			String description) throws ParseException, NotFoundException {

		Set<String> recipientGuids = splitIdList(recipientIds);

		Group group = groupSystem.lookupGroupById(viewpoint, groupId);
		if (group == null)
			throw new RuntimeException("No such group");

		// this is what can throw ParseException
		Set<GuidPersistable> recipients = identitySpider.lookupGuidStrings(
				GuidPersistable.class, recipientGuids);

		postingBoard.doShareGroupPost(viewpoint.getViewer(), group, null, description, recipients,
				true);
	}

	public void doRenamePerson(UserViewpoint viewpoint, String name) {
		viewpoint.getViewer().setNickname(name);
	}

	public void doCreateGroup(OutputStream out, HttpResponseData contentType,
			UserViewpoint viewpoint, String name, String members, boolean secret)
			throws IOException, ParseException, NotFoundException {
		Set<String> memberGuids = splitIdList(members);

		Set<Person> memberPeople = identitySpider.lookupGuidStrings(
				Person.class, memberGuids);

		Group group = 
			groupSystem.createGroup(viewpoint.getViewer(), name,
			 	                    secret ? GroupAccess.SECRET : GroupAccess.PUBLIC_INVITE);
		for (Person p : memberPeople)
			groupSystem.addMember(viewpoint.getViewer(), group, p);

		returnObjects(out, contentType, viewpoint, null, Collections
				.singleton(group));
	}

	public void doAddMembers(OutputStream out, HttpResponseData contentType,
			UserViewpoint viewpoint, String groupId, String memberIds) throws IOException,
			ParseException, NotFoundException {
		Set<String> memberGuids = splitIdList(memberIds);

		Set<Person> memberPeople = identitySpider.lookupGuidStrings(
				Person.class, memberGuids);

		Group group = groupSystem.lookupGroupById(viewpoint, groupId);
		if (group == null)
			throw new RuntimeException("No such group");
		for (Person p : memberPeople)
			groupSystem.addMember(viewpoint.getViewer(), group, p);

		returnObjects(out, contentType, viewpoint, null, Collections
				.singleton(group));
	}

	public void doAddContact(OutputStream out, HttpResponseData contentType,
			UserViewpoint viewpoint, String email) throws IOException {
		EmailResource emailResource = identitySpider.getEmail(email);
		Contact contact = identitySpider.createContact(viewpoint.getViewer(), emailResource);
		PersonView contactView = identitySpider.getPersonView(viewpoint,
				contact, PersonViewExtra.ALL_RESOURCES);

		returnObjects(out, contentType, viewpoint, Collections
				.singleton(contactView), null);
	}

	public void doJoinGroup(UserViewpoint viewpoint, String groupId) {
		try {
			Group group = groupSystem.lookupGroupById(viewpoint, groupId);
			groupSystem.addMember(viewpoint.getViewer(), group, viewpoint.getViewer());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public void doLeaveGroup(UserViewpoint viewpoint, String groupId) {
		try {
			Group group = groupSystem.lookupGroupById(viewpoint, groupId);
			groupSystem.removeMember(viewpoint.getViewer(), group, viewpoint.getViewer());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public void doAddContactPerson(UserViewpoint viewpoint, String contactId) {
		try {
			Person contact = identitySpider.lookupGuidString(Person.class,
					contactId);
			identitySpider.addContactPerson(viewpoint.getViewer(), contact);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (NotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}

	public void doRemoveContactPerson(UserViewpoint viewpoint, String contactId) {
		try {
			Person contact = identitySpider.lookupGuidString(Person.class,
					contactId);
			identitySpider.removeContactPerson(viewpoint.getViewer(), contact);
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

	public void doSendClaimLinkEmail(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException {
		claimVerifier.sendClaimVerifierLink(viewpoint, viewpoint.getViewer(), address);
	}
	
	public void doSendClaimLinkAim(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException {
		claimVerifier.sendClaimVerifierLink(viewpoint, viewpoint.getViewer(), address);
	}

	public void doRemoveClaimEmail(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException {
		Resource resource = identitySpider.getEmail(address);
		identitySpider.removeVerifiedOwnershipClaim(viewpoint, viewpoint.getViewer(), resource);
	}

	public void doRemoveClaimAim(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException {
		Resource resource = identitySpider.lookupAim(address);
		if (resource == null)
			throw new HumanVisibleException("That AIM screen name isn't associated with any account");
		identitySpider.removeVerifiedOwnershipClaim(viewpoint, viewpoint.getViewer(), resource);
	}
	
	public void doSetAccountDisabled(UserViewpoint viewpoint, boolean disabled)
			throws IOException, HumanVisibleException {
		identitySpider.setAccountDisabled(viewpoint.getViewer(), disabled);
	}

	public void doSetPassword(UserViewpoint viewpoint, String password) throws IOException,
			HumanVisibleException {
		password = password.trim();
		if (password.length() == 0) {
			password = null;
		}
		signinSystem.setPassword(viewpoint.getViewer(), password);
	}

	public void doSetMySpaceName(UserViewpoint viewpoint, String name) throws IOException {
		identitySpider.setMySpaceName(viewpoint.getViewer(), name);
	}

	public void doSetMusicSharingEnabled(UserViewpoint viewpoint, boolean enabled)
			throws IOException {
		identitySpider.setMusicSharingEnabled(viewpoint.getViewer(), enabled);
	}
	
	public void doSetNotifyPublicShares(UserViewpoint viewpoint, boolean notify)
		throws IOException {
		identitySpider.setNotifyPublicShares(viewpoint.getViewer(), notify);
	}	

	public void doSetBio(UserViewpoint viewpoint, String bio) {
		identitySpider.setBio(viewpoint, viewpoint.getViewer(), bio);
	}

	public void doSetMusicBio(UserViewpoint viewpoint, String bio) {
		identitySpider.setMusicBio(viewpoint, viewpoint.getViewer(), bio);
	}
	
	public void getNowPlaying(OutputStream out, HttpResponseData contentType,
			String who, String theme) throws IOException {
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
		
		NowPlayingTheme themeObject;
		if (theme == null) {
			try {
				// this falls back to "any random theme" if user doesn't have one
				themeObject = musicSystem.getCurrentNowPlayingTheme(whoUser);
			} catch (NotFoundException e) {
				// happens only if no themes are in the system
				themeObject = null;
			}
		} else {
			try {
				themeObject = musicSystem.lookupNowPlayingTheme(theme);
			} catch (ParseException e) {
				throw new RuntimeException("bad theme argument", e);
			} catch (NotFoundException e) {
				throw new RuntimeException("bad theme argument", e);
			}
		}
		
		if (themeObject == null) {
			// create a non-persistent theme object just for this call; will have 
			// sane default values
			logger.debug("No now playing themes in system or invalid theme id, using a placeholder/temporary theme object");
			themeObject = new NowPlayingTheme(null, whoUser);
		}
		
		TrackView tv;
		try {
			// FIXME this is from the system viewpoint for now, but
			// should really be from an "anonymous" viewpoint
			tv = musicSystem.getCurrentTrackView(null, whoUser);
		} catch (NotFoundException e) {
			tv = null;
		}
		
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		xml.openElement("nowPlaying");
		
		xml.openElement("song");
		if (tv != null) {
			xml.appendTextNode("image", tv.getSmallImageUrl());
			xml.appendTextNode("title", tv.getName());
			xml.appendTextNode("artist", tv.getArtist());
			xml.appendTextNode("album", tv.getAlbum());
			xml.appendTextNode("stillPlaying", Boolean.toString(tv.isNowPlaying()));
		} else {
			xml.appendTextNode("title", "Song Title");
			xml.appendTextNode("artist", "Artist");
			xml.appendTextNode("album", "Album");
			xml.appendTextNode("stillPlaying", "false");
		}
		xml.closeElement();
		
		if (themeObject != null) {
			xml.openElement("theme");
			xml.appendTextNode("activeImageUrl", themeObject.getActiveImageRelativeUrl());
			xml.appendTextNode("inactiveImageUrl", themeObject.getInactiveImageRelativeUrl());
			xml.appendTextNode("text", null, "what", "album", "color", themeObject.getAlbumTextColor(),
					"fontSize", Integer.toString(themeObject.getAlbumTextFontSize()),
					"x", Integer.toString(themeObject.getAlbumTextX()),
					"y", Integer.toString(themeObject.getAlbumTextY()));
			xml.appendTextNode("text", null, "what", "artist", "color", themeObject.getArtistTextColor(),
					"fontSize", Integer.toString(themeObject.getArtistTextFontSize()),
					"x", Integer.toString(themeObject.getArtistTextX()),
					"y", Integer.toString(themeObject.getArtistTextY()));
			xml.appendTextNode("text", null, "what", "title", "color", themeObject.getTitleTextColor(),
					"fontSize", Integer.toString(themeObject.getTitleTextFontSize()),
					"x", Integer.toString(themeObject.getTitleTextX()),
					"y", Integer.toString(themeObject.getTitleTextY()));
			xml.appendTextNode("text", null, "what", "status", "color", themeObject.getStatusTextColor(),
					"fontSize", Integer.toString(themeObject.getStatusTextFontSize()),
					"x", Integer.toString(themeObject.getStatusTextX()),
					"y", Integer.toString(themeObject.getStatusTextY()));
			xml.appendTextNode("albumArt", null, "x", Integer.toString(themeObject.getAlbumArtX()),
					"y", Integer.toString(themeObject.getAlbumArtY()));
			xml.closeElement();
		}
		
		xml.closeElement();
		out.write(xml.getBytes());
		out.flush();
	}
	
	public void doCreateNewNowPlayingTheme(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String basedOn)
		throws IOException {
		NowPlayingTheme basedOnObject;
		if (basedOn != null) {
			try {
				basedOnObject = musicSystem.lookupNowPlayingTheme(basedOn);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			} catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
		} else {
			basedOnObject = null;
		}
		
		NowPlayingTheme theme = musicSystem.createNewNowPlayingTheme(viewpoint, basedOnObject);
		out.write(theme.getId().getBytes());
		out.flush();
	}
	
	public void doSetNowPlayingTheme(UserViewpoint viewpoint, String themeId) throws IOException {
		NowPlayingTheme theme;
		try {
			theme = musicSystem.lookupNowPlayingTheme(themeId);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		musicSystem.setCurrentNowPlayingTheme(viewpoint, viewpoint.getViewer(), theme);
	}
	
	public void doModifyNowPlayingTheme(UserViewpoint viewpoint, String themeId, String key, String value) throws IOException {
		NowPlayingTheme theme;
		try {
			theme = musicSystem.lookupNowPlayingTheme(themeId);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		final String[] blacklist = { "id", "guid", "creator", "basedOn", "creationDate" };
		for (String b : blacklist) {
			if (key.equals(b))
				throw new RuntimeException("property " + b + " can't be changed");
		}
		
		if (!viewpoint.isOfUser(theme.getCreator())) {
			throw new RuntimeException("can only modify your own themes");
		}
		
		BeanUtils.setValue(theme, key, value);
	}
	
	public void doInviteSelf(OutputStream out, HttpResponseData contentType, String address, String promotion) throws IOException {
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");

		String note = null;
		
		Character character;
		
		PromotionCode promotionCode = null;
		
		try {
			promotionCode = PromotionCode.check(promotion);
			switch (promotionCode) {
			case MUSIC_INVITE_PAGE_200602:
				character = Character.MUSIC_GEEK;
				break;
			case GENERIC_LANDING_200606:
				character = Character.MUGSHOT;
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
				note = invitationSystem.sendEmailInvitation(new UserViewpoint(inviter), promotionCode, address,
							"Mugshot Beacon Download", "Hey!\n\nClick here to get the Mugshot Music Radar and Link Swarm.");
				if (note == null)
					note = "Your invitation is on its way (check your email)";
			}
		}
		
		if (note == null)
			throw new RuntimeException("bug! note was null in InviteSelf");
		
		//logger.debug("invite self message: '{}'", note);
		
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		xml.openElement("inviteSelfReply");
		xml.appendTextNode("message", note);
		xml.closeElement();
		out.write(xml.getBytes());
		out.flush();
	}
	
	public void doSendEmailInvitation(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String address, String subject, String message) throws IOException
	{
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");
		
		address = address.trim();
		
		// This error won't get back to the client
		if (address.equals("") || !address.contains("@")) 
			throw new RuntimeException("Missing or invalid email address");

		String note = invitationSystem.sendEmailInvitation(viewpoint, null, address, subject, message);
		
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		xml.openElement("sendEmailInvitationReply");
		xml.appendTextNode("message", note);
		xml.closeElement();
		out.write(xml.getBytes());
		out.flush();
	}
	
	public void doSendGroupInvitation(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String groupId, String inviteeId, String inviteeAddress, String subject, String message) throws IOException
	{
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");
		
		Contact contact;
		
		if (inviteeId != null) {
			try {
				contact = identitySpider.lookupGuidString(Contact.class, inviteeId);
			} catch (ParseException e) {
				throw new RuntimeException("bad invitee guid", e);
			} catch (NotFoundException e) {
				throw new RuntimeException("no such invitee guid", e);
			}
			
		} else if (inviteeAddress != null) {
			inviteeAddress = inviteeAddress.trim();

			if (inviteeAddress.equals("") || !inviteeAddress.contains("@")) 
				throw new RuntimeException("Missing or invalid email address");

			EmailResource resource = identitySpider.getEmail(inviteeAddress);
			contact = identitySpider.createContact(viewpoint.getViewer(), resource);
		} else {
			throw new RuntimeException("inviteeId and inviteeAddress can't both be null");
		}
		
		Group group;
		try {
			group = groupSystem.lookupGroupById(viewpoint, groupId);
		} catch (NotFoundException e) {
			throw new RuntimeException("No such group");
		}

		GuidPersistable recipient = (GuidPersistable)contact;
		Set<GuidPersistable> recipients = Collections.singleton(recipient);
		try {
			postingBoard.doShareGroupPost(viewpoint.getViewer(), group, subject, message, recipients, true);
		} catch (NotFoundException e) {
			throw new RuntimeException("doShareGroup unxpectedly couldn't find contact recipient");
		}
		
		PersonView contactView = identitySpider.getPersonView(viewpoint, contact, PersonViewExtra.PRIMARY_RESOURCE);
		
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		xml.openElement("sendEmailInvitationReply");
		xml.appendTextNode("message", contactView.getName() + " has been invited to the group " + group.getName());
		xml.closeElement();
		out.write(xml.getBytes());
		out.flush();
	}

	public void doSendRepairEmail(UserViewpoint viewpoint, String userId)
	{
		if (!identitySpider.isAdministrator(viewpoint.getViewer())) {
			throw new RuntimeException("Only administrators can send repair links");
		}
		
		User user;
		try {
			user = identitySpider.lookupGuidString(User.class, userId);
		} catch (ParseException e) {
			throw new RuntimeException("bad guid", e);
		} catch (NotFoundException e) {
			throw new RuntimeException("no such person", e);
		}
		
		try {
			signinSystem.sendRepairLink(user);
		} catch (HumanVisibleException e) {
			throw new RuntimeException("Error sending repair link", e);
		}
	}

	public void doReindexAll(UserViewpoint viewpoint) 
	{
		if (!identitySpider.isAdministrator(viewpoint.getViewer())) {
			throw new RuntimeException("Only administrators can recreate the search indices");
		}
		
		PostIndexer.getInstance().reindex();
		TrackIndexer.getInstance().reindex();
	}
	
	public void doSetFavoritePost(UserViewpoint viewpoint, String postId, boolean favorite) {
		Guid postGuid;
		try {
			postGuid = new Guid(postId);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		Post post;
		try {
			post = postingBoard.loadRawPost(viewpoint, postGuid);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		postingBoard.setFavoritePost(viewpoint, post, favorite);
	}
	
	public void getRandomBio(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint) throws IOException {
		if (contentType != HttpResponseData.TEXT)
			throw new IllegalArgumentException("only support TEXT replies");

		out.write(StringUtils.getBytes("I was born in the year 1903, in a shack."));
		out.flush();
	}

	public void getRandomMusicBio(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint) throws IOException {
		if (contentType != HttpResponseData.TEXT)
			throw new IllegalArgumentException("only support TEXT replies");
	
		out.write(StringUtils.getBytes("Polka makes me perky!"));
		out.flush();
	}
	
	public void doSetStockPhoto(UserViewpoint viewpoint, String photo) {
		identitySpider.setStockPhoto(viewpoint, viewpoint.getViewer(), photo);
	}
	
	public void getUserPhoto(OutputStream out, HttpResponseData contentType, String userId, String size)
		throws IOException {
		if (contentType != HttpResponseData.TEXT)
			throw new IllegalArgumentException("only support TEXT replies");
		
		User user;
		try {
			user = identitySpider.lookupGuidString(User.class, userId);
		} catch (ParseException e) {
			throw new RuntimeException("bad guid", e);
		} catch (NotFoundException e) {
			throw new RuntimeException("no such person", e);
		}

		int sizeValue = Integer.parseInt(size);
		switch (sizeValue) {
		case Configuration.SHOT_SMALL_SIZE:
		case Configuration.SHOT_MEDIUM_SIZE:
		case Configuration.SHOT_LARGE_SIZE:
			break;
		default:
			throw new RuntimeException("invalid photo size");
		}
		
		String url = user.getPhotoUrl(sizeValue);
		
		out.write(StringUtils.getBytes(url));
		out.flush();
	}
}
