package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.Parser;
import bsh.TokenMgrError;

import com.dumbhippo.BeanUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.persistence.Validators;
import com.dumbhippo.persistence.WantsIn;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
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
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.TrackIndexer;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.WantsInSystem;
import com.dumbhippo.server.XmlMethodErrorCode;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.util.EJBUtil;

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
	private WantsInSystem wantsInSystem;
	
	@EJB
	private ClaimVerifier claimVerifier;
	
	@EJB
	private Configuration config;
	
	@EJB
	private FeedSystem feedSystem;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
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
								: null, "emails", emailsStr, "aims", aimsStr,
						"photoUrl", p.getSmallPhotoUrl());
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

				xml.appendTextNode("group", null, "id", g.getId(), 
						"display", g.getName(), 
						"photoUrl", g.getPhotoUrl60(),
						"sampleMembers", sampleMembers.toString(),
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

	// this could even be in HttpMethodServlet, would be nice sometime
	private Group parseGroupId(Viewpoint viewpoint, String groupId) throws XmlMethodException {
		try {
			return groupSystem.lookupGroupById(viewpoint, groupId);
		} catch (NotFoundException e) {
			throw new XmlMethodException(XmlMethodErrorCode.UNKNOWN_GROUP, "Unknown group");
		}
	}	
	
	// FIXME if we change doShareLink to be an "XMLMETHOD" then this can throw XmlMethodException directly
	private URL parseUserEnteredUrl(String url, boolean httpOnly) throws MalformedURLException {
		url = url.trim();
		URL urlObject;
		try {
			urlObject = new URL(url);
		} catch (MalformedURLException e) {
			if (!url.startsWith("http://")) {
				// let users type just "example.com" instead of "http://example.com"
				return parseUserEnteredUrl("http://" + url, httpOnly);	
			} else {
				throw e;
			}
		}
		if (httpOnly && !(urlObject.getProtocol().equals("http") || urlObject.getProtocol().equals("https")))
			throw new MalformedURLException("Invalid protocol in url " + url);
		return urlObject;
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

		EmailResource resource;
		try {
			resource = identitySpider.getEmail(email);
		} catch (ValidationException e) {
			// FIXME this probably needs displaying to the user 
			throw new RuntimeException(e);
		}
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

		PostInfo info;
		if (postInfoXml != null)
			info = PostInfo.parse(postInfoXml);
		else
			info = null;

		// this is what can throw ParseException
		Set<GuidPersistable> recipients = identitySpider.lookupGuidStrings(
				GuidPersistable.class, recipientGuids);

		URL urlObject = parseUserEnteredUrl(url, true);

		Post post = postingBoard.doLinkPost(viewpoint.getViewer(), isPublic, title, description,
							urlObject, recipients, PostingBoard.InviteRecipients.DONT_INVITE, info);
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
				PostingBoard.InviteRecipients.MUST_INVITE);
	}

	public void doRenamePerson(UserViewpoint viewpoint, String name) {
		viewpoint.getViewer().setNickname(name);
	}

	public void doCreateGroup(OutputStream out, HttpResponseData contentType,
			UserViewpoint viewpoint, String name, String members, boolean secret, String description)
			throws IOException, ParseException, NotFoundException {
		Set<Person> memberPeople;
		
		if (members != null) {
			Set<String> memberGuids = splitIdList(members);
			memberPeople = identitySpider.lookupGuidStrings(Person.class, memberGuids);
		} else {
			memberPeople = Collections.emptySet();
		}

		Group group = 
			groupSystem.createGroup(viewpoint.getViewer(), name,
			 	                    secret ? GroupAccess.SECRET : GroupAccess.PUBLIC_INVITE,
			 	                   	description);
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
		EmailResource emailResource;
		try {
			emailResource = identitySpider.getEmail(email);
		} catch (ValidationException e) {
			// FIXME needs displaying to the user
			throw new RuntimeException(e);
		}
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

	public void doRenameGroup(UserViewpoint viewpoint, String groupId, String name) {
		try {
			Group group = groupSystem.lookupGroupById(viewpoint, groupId);
			
			if (!groupSystem.canEditGroup(viewpoint, group))
				throw new RuntimeException("Only active members can edit a group");
			
			name = name.trim();
			if (name == "")
				throw new RuntimeException("Name is empty");
			
			group.setName(name);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}		
	}
	
	public void doSetGroupDescription(UserViewpoint viewpoint, String groupId, String description) {
		try {
			Group group = groupSystem.lookupGroupById(viewpoint, groupId);
			
			if (!groupSystem.canEditGroup(viewpoint, group))
				throw new RuntimeException("Only active members can edit a group");
			
			description = description.trim();
			if (description == "")
				throw new RuntimeException("Description is empty");
			
			group.setDescription(description);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}		
	}
	
	public void doSetGroupStockPhoto(UserViewpoint viewpoint, String groupId, String photo) {
		try {
			Group group = groupSystem.lookupGroupById(viewpoint, groupId);
			
			if (!groupSystem.canEditGroup(viewpoint, group))
				throw new RuntimeException("Only active members can edit a group");

			if (photo != null && !Validators.validateStockPhoto(photo))
				throw new RuntimeException("invalid stock photo name");
			
			group.setStockPhoto(photo);
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

	public void doSendLoginLinkEmail(XmlBuilder xml, String address) throws IOException, HumanVisibleException {
		signinSystem.sendSigninLinkEmail(address);
	}

	public void doSendClaimLinkEmail(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException {
		claimVerifier.sendClaimVerifierLink(viewpoint, viewpoint.getViewer(), address);
	}
	
	public void doSendClaimLinkAim(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException {
		claimVerifier.sendClaimVerifierLink(viewpoint, viewpoint.getViewer(), address);
	}

	public void doRemoveClaimEmail(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException {
		Resource resource = identitySpider.lookupEmail(address);
		if (resource == null)
			return; // doesn't exist anyhow
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
			String image = tv.getSmallImageUrl();
			
			// flash embed needs an absolute url
			if (image != null && image.startsWith("/")) {
				String baseurl = config.getProperty(HippoProperty.BASEURL);
				image = baseurl + image;
			}
			xml.appendTextNode("image", image);
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
			String activeUrl = themeObject.getActiveImageRelativeUrl();
			xml.appendTextNode("activeImageUrl", activeUrl);
			String inactiveUrl = themeObject.getInactiveImageRelativeUrl();
			xml.appendTextNode("inactiveImageUrl", inactiveUrl);
			// Append degraded-mode (no alpha, lossy) images for Flash 7 ; nobody 
			// else should use these as they will be unreliable and suck.
			// Flash 7 can't load GIF or PNG, only JPEG.
			if (activeUrl != null)
				xml.appendTextNode("activeImageUrlFlash7", activeUrl + ".jpg");
			if (inactiveUrl != null)
				xml.appendTextNode("inactiveImageUrlFlash7", inactiveUrl + ".jpg");			
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

	private void writeMessageReply(OutputStream out, String nodeName, String message) throws IOException {
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		xml.openElement(nodeName);
		xml.appendTextNode("message", message);
		xml.closeElement();
		out.write(xml.getBytes());
		out.flush();
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
				// not valid at the moment
				character = null;
				// character = Character.MUSIC_GEEK;
				break;
			case GENERIC_LANDING_200606:
                // not valid at the moment
				character = null;
				// character = Character.MUGSHOT;
				break;
			case SUMMIT_LANDING_200606:
				// not valid at the moment
				character = null;
				// character = Character.MUGSHOT;
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
				try {
					note = invitationSystem.sendEmailInvitation(new UserViewpoint(inviter), promotionCode, address,
								"Mugshot Download", "Hey!\n\nClick here to get the Mugshot Music Radar and Web Swarm.");
				} catch (ValidationException e) {
					// FIXME should be displayed to user somehow
					throw new RuntimeException("Invalid email address", e); 
				}
				if (note == null)
					note = "Your invitation is on its way (check your email)";
			}
		}
		
		if (note == null)
			throw new RuntimeException("bug! note was null in InviteSelf");
		
		//logger.debug("invite self message: '{}'", note);
		
		writeMessageReply(out, "inviteSelfReply", note);
	}
	
	public void doSendEmailInvitation(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String address, String subject, String message, String suggestedGroupIds) throws IOException {
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");
		
		address = address.trim();
		
		String note;
		try {
			note = invitationSystem.sendEmailInvitation(viewpoint, null, address, subject, message);
			
			Set<String> groupIdsSet = splitIdList(suggestedGroupIds);
			
		    EmailResource invitee = identitySpider.getEmail(address);

			// this will try to findContact first, which should normally return an existing contact
			// because the viewer has just sent the invitation to the system to the invitee 
			Contact contact = identitySpider.createContact(viewpoint.getViewer(), invitee);
			
			for (String groupId : groupIdsSet) {
			    Group groupToSuggest = groupSystem.lookupGroupById(viewpoint, groupId);
			    groupSystem.addMember(viewpoint.getViewer(), groupToSuggest, contact);
			}
		} catch (NotFoundException e) {
				throw new RuntimeException("Group with a given id not found " + e);	
		} catch (ValidationException e) {
			// FIXME This error won't get back to the client			
			throw new RuntimeException("Missing or invalid email address", e);			
		}
		
		writeMessageReply(out, "sendEmailInvitationReply", note);
	}
	
	
	public void doInviteWantsIn(String countToInvite, String subject, String message, String suggestedGroupIds) throws IOException {	
		logger.debug("Got into doInviteWantsIn");
		int countToInviteValue = Integer.parseInt(countToInvite);
		
		String note = null;
		
		Character character = Character.MUGSHOT;
		User inviter = identitySpider.getCharacter(character);
			
		if (!inviter.getAccount().canSendInvitations(countToInviteValue)) {
            logger.debug("Mugshot character does not have enough invitations to invite {} people.", countToInviteValue);
        } else {
        	
        	List<WantsIn> wantsInList = wantsInSystem.getWantsInWithoutInvites(countToInviteValue);
        	
    		for (WantsIn wantsIn : wantsInList) {    			
                // this does NOT check whether the account has invitations left,
                // that's why we do it above.
			    try {
					note = invitationSystem.sendEmailInvitation(new UserViewpoint(inviter), null, wantsIn.getAddress(),
					                                            subject, message);
				} catch (ValidationException e) {
					// continue here, so we don't abort the whole thing if we got a bogus email address in the db
					// (historically our validation is not so hot)
					logger.warn("Tried to invite WantsIn with invalid email address", e);
				}
			    if (note == null) {
                    logger.debug("Invitation for {} is on its way", wantsIn.getAddress());
                    wantsIn.setInvitationSent(true);
			    } else {
				    logger.debug("Trying to send an invitation to {} produced the following note: {}", wantsIn.getAddress(), note);
				    if (note.contains(InvitationSystem.INVITATION_SUCCESS_STRING)) {
	                    wantsIn.setInvitationSent(true);				    	
				    }
			    }
			    
				Set<String> groupIdsSet = splitIdList(suggestedGroupIds);
				
			    EmailResource invitee;
				try {
				    invitee = identitySpider.getEmail(wantsIn.getAddress());
				} catch (ValidationException e) {
					throw new RuntimeException("Missing or invalid email address", e);
				}

				// this will try to findContact first, which should normally return an existing contact
				// because Mugshot has already sent the invitation to the system to the invitee 
				Contact contact = identitySpider.createContact(inviter, invitee);

				try {
				    for (String groupId : groupIdsSet) {
				        Group groupToSuggest = groupSystem.lookupGroupById(new UserViewpoint(inviter), groupId);
				        groupSystem.addMember(inviter, groupToSuggest, contact);
				    }
				} catch (NotFoundException e) {
					throw new RuntimeException("Group with a given id not found " + e);
				}	    	
				
    		}
        }
	}
	
	public void doSendGroupInvitation(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String groupId, String inviteeId, String inviteeAddress, String subject, String message) throws IOException
	{
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");
		
		Contact contact;
		
		Group group;
		try {
			group = groupSystem.lookupGroupById(viewpoint, groupId);
		} catch (NotFoundException e) {
			throw new RuntimeException("No such group");
		}

		if (inviteeId != null) {
			try {
				contact = identitySpider.lookupGuidString(Contact.class, inviteeId);
			} catch (ParseException e) {
				throw new RuntimeException("bad invitee guid", e);
			} catch (NotFoundException e) {
				throw new RuntimeException("no such invitee guid", e);
			}
			
		} else if (inviteeAddress != null) { 
			EmailResource resource;
			try {
				resource = identitySpider.getEmail(inviteeAddress);
			} catch (ValidationException e) {
				throw new RuntimeException("Missing or invalid email address", e);
			}

			// If the recipient isn't yet a Mugshot member, make sure we can invite
			// them to the system before sending out an email; the resulting email
			// is very confusing if it is an invitation; this check is to produce
			// a nice message back to the user; we do another check when actually
			// sending out the invitation to prevent race conditions
			User user = identitySpider.getUser(resource);
			if (user == null && viewpoint.getViewer().getAccount().getInvitations() == 0) {
				String note = "Sorry, " + inviteeAddress + " isn't a Mugshot member yet";
				writeMessageReply(out, "sendGroupInvitationReply", note);
			
				return;
			}

			contact = identitySpider.createContact(viewpoint.getViewer(), resource);
		} else {
			throw new RuntimeException("inviteeId and inviteeAddress can't both be null");
		}
		
		GuidPersistable recipient = (GuidPersistable)contact;
		Set<GuidPersistable> recipients = Collections.singleton(recipient);
		try {
			postingBoard.doShareGroupPost(viewpoint.getViewer(), group, subject, message, recipients, PostingBoard.InviteRecipients.MUST_INVITE);
		} catch (NotFoundException e) {
			throw new RuntimeException("doShareGroup unxpectedly couldn't find contact recipient");
		}
		
		// let's find out if we were inviting to the group or inviting to follow the group
		boolean adderCanAdd = groupSystem.canAddMembers(viewpoint.getViewer(), group);
		
		PersonView contactView = identitySpider.getPersonView(viewpoint, contact, PersonViewExtra.PRIMARY_RESOURCE);

		String note;
		if (adderCanAdd) {
		    note = contactView.getName() + " has been invited to the group " + group.getName();
		} else {
			note = contactView.getName() + " has been invited to follow the group " + group.getName();
		}
		
		writeMessageReply(out, "sendGroupInvitationReply", note);
	}

	public void doSuggestGroups(UserViewpoint viewpoint, String address, String suggestedGroupIds, String desuggestedGroupIds) {
		Set<String> suggestedGroupIdsSet = splitIdList(suggestedGroupIds);
		Set<String> desuggestedGroupIdsSet = splitIdList(desuggestedGroupIds);
		
		address = address.trim();

		EmailResource invitee;
		try {
		    invitee = identitySpider.getEmail(address);
		} catch (ValidationException e) {
			throw new RuntimeException("Missing or invalid email address", e);
		}

		// this will try to findContact first, which should normally return an existing contact
		// if the viewer has already sent the invitation to the system to the invitee 
		Contact contact = identitySpider.createContact(viewpoint.getViewer(), invitee);

		try {
		    for (String groupId : suggestedGroupIdsSet) {
		        Group groupToSuggest = groupSystem.lookupGroupById(viewpoint, groupId);
		        groupSystem.addMember(viewpoint.getViewer(), groupToSuggest, contact);
		    }
		    
		    for (String groupId : desuggestedGroupIdsSet) {
		    	Group groupToDesuggest = groupSystem.lookupGroupById(viewpoint, groupId);
		        groupSystem.removeMember(viewpoint.getViewer(), groupToDesuggest, contact);
		    }		    
		    
		} catch (NotFoundException e) {
			throw new RuntimeException("Group with a given id not found " + e);
		}	    	
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
	
	public void getGroupPhoto(OutputStream out, HttpResponseData contentType, String groupId, String size)
		throws IOException {
		if (contentType != HttpResponseData.TEXT)
			throw new IllegalArgumentException("only support TEXT replies");
		
		Group group;
		try {
			group = groupSystem.lookupGroupById(SystemViewpoint.getInstance(), groupId);
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
		
		String url = group.getPhotoUrl(sizeValue);
		
		out.write(StringUtils.getBytes(url));
		out.flush();
	}
	
	public void doAcceptTerms(UserViewpoint viewpoint) {
		viewpoint.getViewer().getAccount().setHasAcceptedTerms(true);
	}
	
	public void doSetAdminDisabled(UserViewpoint viewpoint, String userId, boolean disabled) {
		if (!identitySpider.isAdministrator(viewpoint.getViewer())) {
			throw new RuntimeException("Only administrators can administratively disable/enable accounts");
		}
		
		User user;
		try {
			user = identitySpider.lookupGuidString(User.class, userId);
		} catch (ParseException e) {
			throw new RuntimeException("bad guid", e);
		} catch (NotFoundException e) {
			throw new RuntimeException("no such person", e);
		}
		
		user.getAccount().setAdminDisabled(disabled);
	}
	
	private void writeException(XmlBuilder xml, StringWriter clientOut, Throwable t) throws IOException {
		xml.openElement("result", "type", "exception");
		xml.appendTextNode("output", clientOut.toString());		
		xml.appendTextNode("message", t.getMessage());
		StringWriter buf = new StringWriter();
		t.printStackTrace(new PrintWriter(buf));
		xml.appendTextNode("trace", buf.toString());
		xml.closeElement();
	}
	
	private void writeSuccess(XmlBuilder xml, StringWriter clientOut, Object result) throws IOException {
		xml.openElement("result", "type", "success");
		xml.appendTextNode("retval", result != null ? result.toString() : "null", "class", result != null ? result.getClass().getCanonicalName() : "null");
		if (result != null) {
			xml.openElement("retvalReflection");
			for (Method m : result.getClass().getMethods()) {
				xml.openElement("method", "name", m.getName(), "return", m.getReturnType().getSimpleName());
				for (Class param : m.getParameterTypes()) {
					xml.appendTextNode("param", param.getSimpleName());
				}
				xml.closeElement();
			}
			xml.closeElement();
		}
		xml.appendTextNode("output", clientOut.toString());
		xml.closeElement();
	}
	
	public class Server implements Serializable {
		private static final long serialVersionUID = 1L;

		private String defaultPackage = "com.dumbhippo.server";
		
		public Object getLiveState() {
			return LiveState.getInstance();
		}
		
		public User getUser(String id) throws NotFoundException{
			Guid guid;
			try {
				guid = new Guid(id);
				return identitySpider.lookupGuid(User.class, guid); 				
			} catch (ParseException e) {
				return identitySpider.lookupUserByEmail(SystemViewpoint.getInstance(), id);
			}
		}
		
		public Object getEJB(String name) throws ClassNotFoundException, NamingException {
			if (!name.contains(".")) {
				name = defaultPackage + "." + name;
			}
			return EJBUtil.uncheckedDynamicLookup(name);
		}		
	}
	
	private Interpreter makeInterpreter(StringWriter out) {
		Interpreter bsh = new Interpreter();

		try {
			bsh.set("server", new Server());
			bsh.set("out", out);
			bsh.set("em", em);
			
			// This makes us override private/protected etc
			bsh.eval("setAccessibility(true);");
		
			// Some handy primitives
			bsh.eval("user(str) { return server.getUser(str); };");
			bsh.eval("guid(str) { return new com.dumbhippo.identity20.Guid(str); }");
			
			// Some default bindings
			bsh.eval("identitySpider = server.getEJB(\"IdentitySpider\");");
			bsh.eval("systemView = com.dumbhippo.server.SystemViewpoint.getInstance();");			
		} catch (EvalError e) {
			throw new RuntimeException(e);
		}
		
		return bsh;
	}

	public void doAdminShellExec(XmlBuilder xml, UserViewpoint viewpoint, HttpServletRequest request, boolean parseOnly, String command) throws IOException, HumanVisibleException {
		StringWriter clientOut = new StringWriter();
		if (parseOnly) {
			Parser parser = new Parser(new StringReader(command));
			try {
				while (!parser.Line())
					;
				writeSuccess(xml, clientOut, null);
			} catch (bsh.ParseException e) {
				writeException(xml, clientOut, e);
			} catch (TokenMgrError e) {
				writeException(xml, clientOut, e);
			}
			return;
		}
		
		Interpreter bsh = makeInterpreter(clientOut);

		try {
			Object result = bsh.eval(command);
			bsh.set("result", result);
			writeSuccess(xml, clientOut, result);
		} catch (EvalError e) {
			writeException(xml, clientOut, e);
		}
	}
	
	private Feed getFeedFromUserEnteredUrl(String url) throws XmlMethodException {
		URL urlObject;
		try {
			urlObject = parseUserEnteredUrl(url, true);
		} catch (MalformedURLException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "Invalid URL: " + e.getMessage());
		}
		LinkResource link = identitySpider.getLink(urlObject);
		Feed feed = feedSystem.getFeed(link);
		return feed;
	}
	
	public void getFeedDump(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String url) throws HumanVisibleException, IOException {
		try {
			PrintStream printer = new PrintStream(out);
			
			Feed feed = getFeedFromUserEnteredUrl(url);
			feedSystem.updateFeed(feed);
			
			printer.println("Link: " + feed.getLink().getUrl());
			printer.println("Title: " + feed.getTitle());
			printer.print("Last fetched: " + feed.getLastFetched());
			if (feed.getLastFetchSucceeded())
				printer.println(" (succeeded)");
			else
				printer.println(" (failed)");
			printer.println();
			
			List<FeedEntry> entries = feedSystem.getCurrentEntries(feed);
			for (FeedEntry entry : entries) {
				printer.println("Guid: " + entry.getEntryGuid());
				printer.println("Link: " + entry.getLink());
				printer.println("Title: " + entry.getTitle());
				printer.println("Date: " + entry.getDate());
				printer.println("Description: " + entry.getDescription());
				printer.println();
			}
			
			printer.flush();
			
		} catch (XmlMethodException e) {
			throw new HumanVisibleException(e.getCodeString() + ": " + e.getMessage());
		}
	}

	public void doFeedPreview(XmlBuilder xml, UserViewpoint viewpoint, String url) throws XmlMethodException {
		Feed feed = getFeedFromUserEnteredUrl(url);
		feedSystem.updateFeed(feed);

		// format deliberately kept a little bit similar to RSS
		// (element names title, link, item for example)
		
		xml.openElement("feedPreview");
		xml.appendTextNode("title", feed.getTitle());
		xml.appendTextNode("link", feed.getLink().getUrl());
				
		List<FeedEntry> entries = feedSystem.getCurrentEntries(feed);
		
		xml.openElement("items");
		int count = 0;
		for (FeedEntry entry : entries) {
			if (count > 2)
				break;
			xml.openElement("item");
			xml.appendTextNode("title", entry.getTitle());
			xml.appendTextNode("link", entry.getLink().getUrl());
			xml.closeElement();
			++count;
		}
		xml.closeElement();
		xml.closeElement();
	}
	
	public void doAddGroupFeed(XmlBuilder xml, UserViewpoint viewpoint, String groupId, String url) throws XmlMethodException {
		Group group = parseGroupId(viewpoint, groupId);
		Feed feed = getFeedFromUserEnteredUrl(url);
		
		feedSystem.addGroupFeed(group, feed);
	}

	public void doRemoveGroupFeed(XmlBuilder xml, UserViewpoint viewpoint, String groupId, String url) throws XmlMethodException {
		Group group = parseGroupId(viewpoint, groupId);
		Feed feed = getFeedFromUserEnteredUrl(url);
		
		feedSystem.removeGroupFeed(group, feed);		
	}
}
