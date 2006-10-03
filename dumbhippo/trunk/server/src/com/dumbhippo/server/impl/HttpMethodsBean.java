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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
import com.dumbhippo.persistence.ChatMessage;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.FacebookEventType;
import com.dumbhippo.persistence.FacebookPhotoData;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.StorageState;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.persistence.WantsIn;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.search.SearchSystem;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookTracker;
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
import com.dumbhippo.server.NowPlayingThemeSystem;
import com.dumbhippo.server.PermissionDeniedException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.PromotionCode;
import com.dumbhippo.server.SharedFileSystem;
import com.dumbhippo.server.SigninSystem;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.WantsInSystem;
import com.dumbhippo.server.XmlMethodErrorCode;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.util.FeedScraper;
import com.dumbhippo.server.views.BlockView;
import com.dumbhippo.server.views.EntityView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.services.FlickrUser;
import com.dumbhippo.services.FlickrWebServices;
import com.dumbhippo.statistics.ColumnDescription;
import com.dumbhippo.statistics.ColumnMap;
import com.dumbhippo.statistics.Row;
import com.dumbhippo.statistics.StatisticsService;
import com.dumbhippo.statistics.StatisticsSet;
import com.dumbhippo.statistics.Timescale;

@Stateless
public class HttpMethodsBean implements HttpMethods, Serializable {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup
			.getLogger(HttpMethodsBean.class);

	private static final long serialVersionUID = 0L;

	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PersonViewer personViewer;

	@EJB
	private PostingBoard postingBoard;
	
	@EJB
	private AccountSystem accountSystem;

	@EJB
	private FeedSystem feedSystem;

	@EJB
	private GroupSystem groupSystem;

	@EJB
	private SigninSystem signinSystem;

	@EJB
	private MusicSystem musicSystem;

	@EJB
	private NowPlayingThemeSystem nowPlayingSystem;
	
	@EJB
	private InvitationSystem invitationSystem;
	
	@EJB 
	private WantsInSystem wantsInSystem;
	
	@EJB
	private ClaimVerifier claimVerifier;
	
	@EJB
	private Configuration config;
	
	@EJB
	private ExternalAccountSystem externalAccountSystem;
	
	@EJB
	private FacebookTracker facebookTracker;
	
	@EJB
	private SearchSystem searchSystem;
	
	@EJB
	private Stacker stacker;
	
	@EJB
	private SharedFileSystem sharedFileSystem;
	
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
	
	private void throwIfUrlNotHttp(URL url) throws XmlMethodException {
		if (!(url.getProtocol().equals("http") || url.getProtocol().equals("https")))
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "URL must be http or https: '" + url.toExternalForm() + "'");
	}
	
	// FIXME if we change doShareLink to be an "XMLMETHOD" then this can throw XmlMethodException directly
	// FIXME this method is deprecated; just make your method take an URL then use throwIfUrlNotHttp as required
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

		Set<PersonView> persons = personViewer.getContacts(viewpoint, viewpoint.getViewer(),
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
		PersonView contactView = personViewer.getPersonView(viewpoint,
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
		PersonView contactView = personViewer.getPersonView(viewpoint,
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
						
			group.setName(name);
			searchSystem.indexGroup(group, true);
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
			
			group.setDescription(description);
			searchSystem.indexGroup(group, true);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}		
	}
	
	public void doSetGroupStockPhoto(UserViewpoint viewpoint, String groupId, String photo) {
		try {
			Group group = groupSystem.lookupGroupById(viewpoint, groupId);

			groupSystem.setStockPhoto(viewpoint, group, photo);
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
	
	private void returnTrackXml(XmlBuilder xml, TrackView tv) {
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
				themeObject = nowPlayingSystem.getCurrentNowPlayingTheme(whoUser);
			} catch (NotFoundException e) {
				// happens only if no themes are in the system
				themeObject = null;
			}
		} else {
			try {
				themeObject = nowPlayingSystem.lookupNowPlayingTheme(theme);
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
		
		returnTrackXml(xml, tv);
		
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
				basedOnObject = nowPlayingSystem.lookupNowPlayingTheme(basedOn);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			} catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
		} else {
			basedOnObject = null;
		}
		
		NowPlayingTheme theme = nowPlayingSystem.createNewNowPlayingTheme(viewpoint, basedOnObject);
		out.write(theme.getId().getBytes());
		out.flush();
	}
	
	public void doSetNowPlayingTheme(UserViewpoint viewpoint, String themeId) throws IOException {
		NowPlayingTheme theme;
		try {
			theme = nowPlayingSystem.lookupNowPlayingTheme(themeId);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		nowPlayingSystem.setCurrentNowPlayingTheme(viewpoint, viewpoint.getViewer(), theme);
	}
	
	public void doModifyNowPlayingTheme(UserViewpoint viewpoint, String themeId, String key, String value) throws IOException {
		NowPlayingTheme theme;
		try {
			theme = nowPlayingSystem.lookupNowPlayingTheme(themeId);
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
			case OPEN_SIGNUP_200609:
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
			User inviter = accountSystem.getCharacter(character);
			
			try {
				if (!inviter.getAccount().canSendInvitations(1)) {
					wantsInSystem.addWantsIn(address);				    
					note = "Sorry, someone got there first! No more invitations available right now. We saved your address and will let you know when we have room for more.";
				} else {
					// this does NOT check whether the account has invitations left,
					// that's why we do it above.
					note = invitationSystem.sendEmailInvitation(new UserViewpoint(inviter), promotionCode, address,
								"Mugshot Download", "Hey!\n\nClick here to get the Mugshot Music Radar and Web Swarm.");
					if (note == null)
						note = "Your invitation is on its way (check your email)";
				}
			} catch (ValidationException e) {
			    // FIXME should be displayed to user somehow
				// "Something went wrong! Reload the page and try again." message we display looks ok for now
			    throw new RuntimeException("Invalid email address", e);				
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
		User inviter = accountSystem.getCharacter(character);
			
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
		
		GuidPersistable recipient = contact;
		Set<GuidPersistable> recipients = Collections.singleton(recipient);
		try {
			postingBoard.doShareGroupPost(viewpoint.getViewer(), group, subject, message, recipients, PostingBoard.InviteRecipients.MUST_INVITE);
		} catch (NotFoundException e) {
			throw new RuntimeException("doShareGroup unxpectedly couldn't find contact recipient");
		}
		
		// let's find out if we were inviting to the group or inviting to follow the group
		boolean adderCanAdd = groupSystem.canAddMembers(viewpoint.getViewer(), group);
		
		PersonView contactView = personViewer.getPersonView(viewpoint, contact, PersonViewExtra.PRIMARY_RESOURCE);

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
		
		searchSystem.reindexAll();
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
	
	public void doSetNewFeatures(UserViewpoint viewpoint, boolean newFeaturesFlag) {
		config.setProperty(HippoProperty.NEW_FEATURES.getKey(), Boolean.valueOf(newFeaturesFlag).toString());
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
			try {
				return EJBUtil.uncheckedDynamicLookupLocal(name);
			} catch (NameNotFoundException e) {
				return EJBUtil.uncheckedDynamicLookupRemote(name);
			}
		}		
	}
	
	private Interpreter makeInterpreter(PrintWriter out) {
		Interpreter bsh = new Interpreter();

		try {
			bsh.set("server", new Server());
			bsh.set("out", out);
			bsh.set("em", em);
			
			// This makes us override private/protected etc
			bsh.eval("setAccessibility(true);");
		
			// Some handy primitives
			bsh.eval("user(str) { return server.getUser(str); };");
			bsh.eval("ejb(str) { return server.getEJB(str); };");			
			bsh.eval("guid(str) { return new com.dumbhippo.identity20.Guid(str); }");
			
			// Some default bindings
			bsh.eval("identitySpider = server.getEJB(\"IdentitySpider\");");
			bsh.eval("systemView = com.dumbhippo.server.SystemViewpoint.getInstance();");			
		} catch (EvalError e) {
			throw new RuntimeException(e);
		}
		
		return bsh;
	}

	public void doAdminShellExec(XmlBuilder xml, UserViewpoint viewpoint, boolean parseOnly, String command) throws IOException, HumanVisibleException {
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
		
		PrintWriter pw = new PrintWriter(clientOut);
		Interpreter bsh = makeInterpreter(pw);
		pw.flush();

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
		return scrapeFeedFromUrl(urlObject);
	}
	
	private Feed scrapeFeedFromUrl(URL url) throws XmlMethodException {
		FeedScraper scraper = new FeedScraper();
		try {
			// This downloads the url contents, and if it's already an RSS feed then FeedSystem will do it again 
			// if it's not cached... but since 1) usually we'll be downloading html and not rss here and 2) many feeds
			// will be cached, it's really not worth making a mess to move the downloaded bytes from FeedScraper to FeedSystem
			scraper.analzyeURL(url);
		} catch (IOException e) {
			throw new XmlMethodException(XmlMethodErrorCode.NETWORK_ERROR, "Unable to contact the site (" + e.getMessage() + ")");
		}
		URL feedSource = scraper.getFeedSource();
		if (feedSource == null) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "Couldn't find a feed at " + url);
		}
		FeedSystem feedSystem = EJBUtil.defaultLookup(FeedSystem.class);			
		LinkResource link = identitySpider.getLink(feedSource);
		Feed feed = feedSystem.getFeed(link);
		return feed;
	}
	
	public void getFeedDump(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String url) throws HumanVisibleException, IOException {
		try {
			PrintStream printer = new PrintStream(out);
			FeedSystem feedSystem = EJBUtil.defaultLookup(FeedSystem.class);				
			
			Feed feed = getFeedFromUserEnteredUrl(url);
			feedSystem.updateFeed(feed);
			
			printer.println("Link: " + feed.getSource().getUrl());
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
		FeedSystem feedSystem = EJBUtil.defaultLookup(FeedSystem.class);			
		Feed feed = getFeedFromUserEnteredUrl(url);
		feedSystem.updateFeed(feed);

		// format deliberately kept a little bit similar to RSS
		// (element names title, link, item for example)
		
		xml.openElement("feedPreview");
		xml.appendTextNode("title", feed.getTitle());
		xml.appendTextNode("link", feed.getLink().getUrl());
		xml.appendTextNode("source", feed.getSource().getUrl());
				
		List<FeedEntry> entries = feedSystem.getCurrentEntries(feed);
		
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
		// close feedPreview
		xml.closeElement();
	}
	
	public void doAddGroupFeed(XmlBuilder xml, UserViewpoint viewpoint, String groupId, String url) throws XmlMethodException {
		FeedSystem feedSystem = EJBUtil.defaultLookup(FeedSystem.class);			
		Group group = parseGroupId(viewpoint, groupId);
		Feed feed = getFeedFromUserEnteredUrl(url);
		
		feedSystem.addGroupFeed(group, feed);
	}

	public void doRemoveGroupFeed(XmlBuilder xml, UserViewpoint viewpoint, String groupId, String url) throws XmlMethodException {
		FeedSystem feedSystem = EJBUtil.defaultLookup(FeedSystem.class);			
		Group group = parseGroupId(viewpoint, groupId);
		Feed feed = getFeedFromUserEnteredUrl(url);
		
		feedSystem.removeGroupFeed(group, feed);		
	}
	
	// FIXME this doesn't match the other external account manipulation methods exactly since its 
	// API predates them
	public void doSetRhapsodyHistoryFeed(XmlBuilder xml, UserViewpoint viewpoint, String urlStr) throws XmlMethodException {
		// empty string means unset the value
		if (urlStr.trim().length() == 0) {
			doRemoveExternalAccount(xml, viewpoint, ExternalAccountType.RHAPSODY.name());
			return;
		}

		// otherwise, set a new value
		
		URL url;
		try {
			url = parseUserEnteredUrl(urlStr, true);
		} catch (MalformedURLException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "Doesn't look like a Rhapsody RSS URL : " + e.getMessage());
		}
		
		String q = url.getQuery();
		if (q == null)
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "Doesn't look like a Rhapsody RSS URL: " + urlStr);
		
		int i = q.indexOf("rhapUserId=");
		int j = q.indexOf("&", i);
		if (i < 0 || j < 0 || i == j) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "Doesn't look like a Rhapsody RSS URL: " + url);
		}
		String rhapUserId = q.substring(i, j);
		Feed feed = scrapeFeedFromUrl(url);
		
		logger.debug("found feed: {}", feed);
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.RHAPSODY);
		
		try {
			external.setHandleValidating(rhapUserId);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}

		external.setSentiment(Sentiment.LOVE);
		external.setFeed(feed);
	}
	
	private ExternalAccountType parseExternalAccountType(String type) throws XmlMethodException {
		try {
			return ExternalAccountType.valueOf(type);
		} catch (IllegalArgumentException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Unknown external account type: " + type);
		}
	}

	private String parseEmail(String email) throws XmlMethodException {
		try {
			email = EmailResource.canonicalize(email);
			return email;
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Not a valid email address: '" + email + "'");
		}		
	}
	
	public void doHateExternalAccount(XmlBuilder xml, UserViewpoint viewpoint, String type, String quip) throws XmlMethodException {
		
		// FIXME if we do this to a MySpace account we're supposed to send notifications to the myspace tracker
		
		ExternalAccountType typeEnum = parseExternalAccountType(type);
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, typeEnum);
		external.setSentiment(Sentiment.HATE);
		if (quip != null) {
			quip = quip.trim();
			if (quip.length() == 0)
				quip = null;
		}
		external.setQuip(quip);
	}

	public void doRemoveExternalAccount(XmlBuilder xml, UserViewpoint viewpoint, String type) throws XmlMethodException {
		
		// FIXME if we do this to a MySpace account we're supposed to send notifications to the myspace tracker
		
		ExternalAccountType typeEnum = parseExternalAccountType(type);
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, typeEnum);
		external.setSentiment(Sentiment.INDIFFERENT);
	}

	public void doFindFlickrAccount(XmlBuilder xml, UserViewpoint viewpoint, String email) throws XmlMethodException {
		FlickrWebServices ws = new FlickrWebServices(8000, config);
		FlickrUser flickrUser = ws.lookupFlickrUserByEmail(email);
		if (flickrUser == null)
			throw new XmlMethodException(XmlMethodErrorCode.NOT_FOUND, "Flickr doesn't report a user with the email address '" + email + "'");
		xml.openElement("flickrUser");
		xml.appendTextNode("nsid", flickrUser.getId());
		xml.appendTextNode("username", flickrUser.getName());
		xml.closeElement();
	}

	public void doSetFlickrAccount(XmlBuilder xml, UserViewpoint viewpoint, String nsid, String email) throws XmlMethodException {
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.FLICKR);		

		email = parseEmail(email);
		
		external.setSentiment(Sentiment.LOVE);
		external.setHandle(nsid);
		external.setExtra(email);
	}
	
	public void doSetMySpaceName(XmlBuilder xml, UserViewpoint viewpoint, String name) throws XmlMethodException {
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.MYSPACE);
		String oldHandle = external.getHandle();
		try {
			external.setHandleValidating(name);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}

		external.setSentiment(Sentiment.LOVE);
		
		// FIXME in externalAccounts.setMySpaceName we don't clear the friend ID in this way... 
		// but might have side effect problems if we did.
		String newHandle = external.getHandle();
		if ((oldHandle == null && newHandle != null) ||
			(oldHandle != null && newHandle == null) ||
			(oldHandle != null && newHandle != null && !oldHandle.equals(newHandle))) {
			// kill the friend ID if the name changes
			external.setExtra(null);
		}
	}

	public void doSetYouTubeName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException {
		// Try to pull youtube name out of either a youtube profile url ("http://www.youtube.com/user/$username" || "http://www.youtube.com/profile?user=$username") or 
		// just try using the thing as a username directly
		String name = urlOrName.trim();
		int user = urlOrName.indexOf("/user/");
		if (user >= 0) {
			user += "/user/".length();
			name = urlOrName.substring(user);
		} else if ( (user = urlOrName.indexOf("/profile?user=")) >= 0) {
			user += "/profile?user=".length();
			name = urlOrName.substring(user);
		}
		
		if (name.startsWith("http://"))
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Enter your public profile URL or just your username");
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.YOUTUBE);
		try {
			external.setHandleValidating(name);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		external.setSentiment(Sentiment.LOVE);
		
		xml.appendTextNode("username", external.getHandle());
	}

	public void doSetLinkedInProfile(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException {
		// Try to pull linked in name out of either a linked in profile url ("http://www.linkedin.com/in/username") or 
		// just try using the thing as a username directly
		String name = urlOrName.trim();
		int i = urlOrName.indexOf("/in/");
		if (i >= 0) {
			i += "/in/".length();
			name = urlOrName.substring(i);
		}
		
		if (name.startsWith("http://"))
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Enter your public profile URL or just your username");
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.LINKED_IN);
		try {
			external.setHandleValidating(name);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		external.setSentiment(Sentiment.LOVE);
		
		xml.appendTextNode("username", external.getHandle());
	}

	public void doSetWebsite(XmlBuilder xml, UserViewpoint viewpoint, String url) throws XmlMethodException {
		// DO NOT cut and paste this block into similar external account methods. It's only here because
		// we don't use the "love hate" widget on /account for the website, and the javascript glue 
		// for the plain entries assumes this works.
		if (url.trim().length() == 0) {
			doRemoveExternalAccount(xml, viewpoint, "WEBSITE");
			try {
				ExternalAccount external = externalAccountSystem.lookupExternalAccount(viewpoint, viewpoint.getViewer(), ExternalAccountType.WEBSITE);
				// otherwise the website url would keep "coming back" since there's no visual indication of hate/indifferent status
				external.setHandle(null);
			} catch (NotFoundException e) {
			}
			return;
		}
		
		// the rest of this is more typical of a "set external account" http method
		
		URL urlObject;
		try {
			urlObject = parseUserEnteredUrl(url, true);
		} catch (MalformedURLException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "Invalid url (" + e.getMessage() + ")");
		}
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.WEBSITE);
		try {
			external.setHandleValidating(urlObject.toExternalForm());
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		external.setSentiment(Sentiment.LOVE);
	}

	public void doSetBlog(XmlBuilder xml, UserViewpoint viewpoint, URL url) throws XmlMethodException {
		throwIfUrlNotHttp(url);
		
		Feed feed = scrapeFeedFromUrl(url);
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.BLOG);
		
		try {
			external.setHandleValidating(url.toExternalForm());
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}

		external.setSentiment(Sentiment.LOVE);
		external.setFeed(feed);
	}
	
	private StatisticsService getStatisticsService() throws XmlMethodException {
		// This probably should be using JNDI, or MX, or EJB injection, but the static
		// member variable in StatisticsService is sufficient for now and simple
		StatisticsService service = StatisticsService.getInstance();
		if (service == null)
			throw new XmlMethodException(XmlMethodErrorCode.NOT_READY, "Statistics Service isn't started");
		
		return service;
	}
		
	public void getStatisticsSets(XmlBuilder xml, UserViewpoint viewpoint, String filename) throws IOException, XmlMethodException {
		List<StatisticsSet> sets;
		if (filename == null) {
		    sets = getStatisticsService().listSets();
		} else {
			try {
				sets = Collections.singletonList(getStatisticsService().getSet(filename));
			} catch (NotFoundException e) {
				throw new XmlMethodException(XmlMethodErrorCode.NOT_FOUND, e.getMessage());
			}
		}
		xml.openElement("statisticsSets");
		for (StatisticsSet set : sets) {			
			xml.openElement("statisticsSet");
			// Doing these as child nodes is a little weird to me; it would 
			// be easier on the javascript if they were attributes.
			xml.appendTextNode("current", set.isCurrent() ? "true" : "false");
			xml.appendTextNode("filename", set.getFilename());
			xml.appendTextNode("hostname", set.getHostName());
			xml.appendTextNode("startTime", Long.toString(set.getStartDate().getTime()));
			xml.appendTextNode("endTime", Long.toString(set.getEndDate().getTime()));
			xml.openElement("columns");
			for (ColumnDescription column : set.getColumns()) {
				xml.openElement("column", "id", column.getId(), "units", column.getUnits().name(), "type", column.getType().name());
				xml.appendTextNode("name", column.getName());
				xml.closeElement();
			}
			xml.closeElement();
			xml.closeElement();
			
		}
		xml.closeElement();
	}
	
	public void getStatistics(XmlBuilder xml, UserViewpoint viewpoint, String filename, String columns, String startString, String endString, String timescaleString) throws IOException, XmlMethodException {
		StatisticsSet set;

		try {
			if (filename != null)
				 set = getStatisticsService().getSet(filename);
			else
				set = getStatisticsService().getCurrentSet();
		} catch (NotFoundException e) {
			throw new XmlMethodException(XmlMethodErrorCode.NOT_FOUND, e.getMessage());
		}
		
		String[] columnNames = columns.split(",");
		// will maintain indexes of requested columns as they appear in the statistics set's columnMap 
		int[] columnIndexes = new int[columnNames.length];
		ColumnMap columnMap = set.getColumns();
		
		for (int i = 0; i < columnNames.length; i++) {
			String columnName = columnNames[i];
			try {
				columnIndexes[i] = columnMap.getIndex(columnName);
			} catch (NoSuchElementException e) {
				throw new XmlMethodException(XmlMethodErrorCode.NOT_FOUND, "Column '" + columnName + "' not found");
			}
		}
		
		Date start;
		if (startString != null) {
			try {
				start = new Date(Long.parseLong(startString));
			} catch (NumberFormatException e) {
				throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Bad start time '" + startString + "'");
			}
		} else {
			start = set.getStartDate();
		}
			
		Date end;
		if (endString != null) {
			try {
				end = new Date(Long.parseLong(endString));
			} catch (NumberFormatException e) {
				throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Bad end time '" + endString + "'");
			}			
		} else {
			end = set.getEndDate();
		}
		
		int timescaleSeconds;
		if (timescaleString != null) {
			try {
				timescaleSeconds = Integer.parseInt(timescaleString);
				
			} catch (NumberFormatException e) {
				throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Bad timescale seconds value '" + timescaleString + "'");
			}
		} else {
			// If the user doesn't specify a timescale, try to get about 100 points
			timescaleSeconds = (int)(end.getTime() - start.getTime()) / 1000 / 100;
		}
		
		Timescale timescale = Timescale.get(timescaleSeconds);
		
		xml.openElement("statistics", "timescale", Integer.toString(timescale.getSeconds()));
		
		Iterator<Row> iterator = set.getIterator(start, end, timescale, columnIndexes);
		while (iterator.hasNext()) {
			Row row = iterator.next();
			xml.openElement("row", "time", Long.toString(row.getDate().getTime()));
			for (int i = 0; i < columnIndexes.length; i++) {
				if (i != 0)
					xml.append(',');
				xml.append(Long.toString(row.value(i)));
			}
			xml.closeElement();
		}
		
		xml.closeElement();
	}
	
	
	private User parseUserId(String userId) throws XmlMethodException {
		try {
			return identitySpider.lookupGuidString(User.class, userId);
		} catch (ParseException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "bad userId " + userId);
		} catch (NotFoundException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "no such person " + userId);
		}
	}

	private Post parsePostId(Viewpoint viewpoint, String postId) throws XmlMethodException {
		try {
			return postingBoard.loadRawPost(viewpoint, new Guid(postId));
		} catch (ParseException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "bad postId " + postId);
		} catch (NotFoundException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "no such post " + postId);
		}
	}
	
	private void returnBlocks(XmlBuilder xml, UserViewpoint viewpoint, User user, List<BlockView> list) throws XmlMethodException {
		logger.debug("Returning {} blocks", list.size());
		
		CommonXmlWriter.writeBlocks(xml, viewpoint, user, list, null);
	}
	
	public void getBlocks(XmlBuilder xml, UserViewpoint viewpoint, String userId, String lastTimestampStr, String startStr, String countStr) throws XmlMethodException {
		long lastTimestamp;
		int start;
		int count;
		
		try {
			lastTimestamp = Long.parseLong(lastTimestampStr);
			start = Integer.parseInt(startStr);
			count = Integer.parseInt(countStr);
		} catch (NumberFormatException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "bad integer");
		}
		if (start < 0)
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "start must be >= 0");
		if (count < 1)
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "count must be > 0");
		if (lastTimestamp < 0)
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "lastTimestamp must be >= 0");
		
		User user;
		if (userId == null) {
			user = viewpoint.getViewer();
		} else {
			user = parseUserId(userId);
		}
		
		List<BlockView> list = stacker.getStack(viewpoint, user, lastTimestamp, start, count);
		returnBlocks(xml, viewpoint, user, list);
	}
	
	public void getBlock(XmlBuilder xml, UserViewpoint viewpoint, UserBlockData ubd) throws XmlMethodException, NotFoundException {
		returnBlocks(xml, viewpoint, viewpoint.getViewer(), Collections.singletonList(stacker.loadBlock(viewpoint, ubd)));
	}
	
	public void getMusicPersonSummary(XmlBuilder xml, UserViewpoint viewpoint, String userId) throws XmlMethodException {
		User musicPlayer = parseUserId(userId);
		// ALL_RESOURCES here is just because returnPersonsXml wants it, the javascript doesn't need it
		PersonView pv = personViewer.getPersonView(viewpoint, musicPlayer, PersonViewExtra.ALL_RESOURCES);
		List<TrackView> tracks = musicSystem.getLatestTrackViews(viewpoint, musicPlayer, 5);
		xml.openElement("musicPerson", "userId", musicPlayer.getId());
		returnPersonsXml(xml, viewpoint, Collections.singleton(pv));
		for (TrackView tv : tracks) {
			returnTrackXml(xml, tv);
		}
		xml.closeElement();
	}
	
	public void getExternalAccountSummary(XmlBuilder xml, UserViewpoint viewpoint, String userId, String accountType) throws XmlMethodException, NotFoundException {
		User user = parseUserId(userId);
		// ALL_RESOURCES here is just because returnPersonsXml wants it, the javascript doesn't need it
		PersonView pv = personViewer.getPersonView(viewpoint, user, PersonViewExtra.ALL_RESOURCES);
   		xml.openElement("accountUpdate", "userId", user.getId(), "accountType", accountType);
		returnPersonsXml(xml, viewpoint, Collections.singleton(pv));
		int accountTypeOrdinal = Integer.parseInt(accountType);
		if (accountTypeOrdinal == ExternalAccountType.BLOG.ordinal()) {
			ExternalAccount blogAccount = externalAccountSystem.lookupExternalAccount(viewpoint, user, ExternalAccountType.BLOG);  
			FeedEntry lastEntry = feedSystem.getLastEntry(blogAccount.getFeed());
			xml.appendTextNode("accountType", "Blog");
			xml.openElement("updateItem");
			xml.appendTextNode("updateTitle", lastEntry.getTitle());
			xml.appendTextNode("updateLink", lastEntry.getLink().getUrl());
			xml.appendTextNode("updateText", lastEntry.getDescription());
			xml.closeElement();
		} else if (accountTypeOrdinal == ExternalAccountType.FACEBOOK.ordinal()) {			
			FacebookAccount facebookAccount = facebookTracker.lookupFacebookAccount(viewpoint, user);
			xml.appendTextNode("accountType", "Facebook");
			int eventsToRequestCount = 3;
			if (!facebookAccount.isSessionKeyValid() && viewpoint.isOfUser(facebookAccount.getExternalAccount().getAccount().getOwner())) {
				xml.openElement("updateItem");
			    xml.appendTextNode("updateTitle", "Please re-login to Facebook to continue getting Facebook updates");
			    xml.appendTextNode("updateLink", "http://api.facebook.com/login.php?api_key=" + facebookTracker.getApiKey() +"&next=/account");
			    xml.appendTextNode("updateText", "");	
			    xml.closeElement();
			    eventsToRequestCount = 2;
			}
			List<FacebookEvent> facebookEvents = facebookTracker.getLatestEvents(viewpoint, facebookAccount, eventsToRequestCount);
			for (FacebookEvent facebookEvent : facebookEvents) {
				xml.openElement("updateItem");
				String pageName = "profile";
				String updateText = "";
				String multiple = "";
				if (facebookEvent.getCount() != 1)
					multiple = "s";
				if (facebookEvent.getEventType().equals(FacebookEventType.UNREAD_MESSAGES_UPDATE)) {
				    xml.appendTextNode("updateTitle", facebookEvent.getCount() + " unread message" + multiple);
				} else if (facebookEvent.getEventType().equals(FacebookEventType.NEW_WALL_MESSAGES_EVENT)) {
					xml.appendTextNode("updateTitle", facebookEvent.getCount() + " new wall message" + multiple);					
				} else if (facebookEvent.getEventType().equals(FacebookEventType.UNSEEN_POKES_UPDATE)) {
				    xml.appendTextNode("updateTitle", facebookEvent.getCount() + " unseen poke" + multiple);			
				} else if (facebookEvent.getEventType().equals(FacebookEventType.NEW_TAGGED_PHOTOS_EVENT)) {
					xml.appendTextNode("updateTitle", facebookEvent.getCount() + " new photo" + multiple);		
					pageName = "photo_search";
				} else if (facebookEvent.getEventType().equals(FacebookEventType.NEW_ALBUM_EVENT)) {
					xml.appendTextNode("updateTitle", "Created new album \"" + facebookEvent.getAlbum().getName() + "\"");
					if (!facebookEvent.getAlbum().getLocation().equals("")) {
						updateText = "Location: " + facebookEvent.getAlbum().getLocation() + " ";						
					}
					updateText = updateText + facebookEvent.getAlbum().getDescription();	
					pageName = "photos";
				} else if (facebookEvent.getEventType().equals(FacebookEventType.MODIFIED_ALBUM_EVENT)) {
					xml.appendTextNode("updateTitle", "Modified album \"" + facebookEvent.getAlbum().getName() + "\"");
					if (!facebookEvent.getAlbum().getLocation().equals("")) {
						updateText = "Location: " + facebookEvent.getAlbum().getLocation() + " ";						
					}
					updateText = updateText + facebookEvent.getAlbum().getDescription();	
					pageName = "photos";
				} else {
					throw new RuntimeException("Unexpected event type in HttpMethodsBean::getExternalAccountSummary(): " + facebookEvent.getEventType());
				}
			    xml.appendTextNode("updateLink", "http://www.facebook.com/" + pageName + ".php?uid=" + facebookAccount.getFacebookUserId() + "&api_key=" + facebookTracker.getApiKey());
			    xml.appendTextNode("updateText", updateText);
			    xml.appendTextNode("updateTimestamp", Long.toString(facebookEvent.getEventTimestampAsLong()));
			    if ((facebookEvent.getPhotos().size() > 0) || (facebookEvent.getAlbum() != null)) {
					xml.openElement("updatePhotos");								
					Set<FacebookPhotoData> photos;
					if (facebookEvent.getPhotos().size() > 0) {
						photos = facebookEvent.getPhotos();
					} else {
						photos = Collections.singleton(facebookEvent.getAlbum().getCoverPhoto());
					}
					for (FacebookPhotoData photoData : photos) {
						if (photoData.isValid()) {
						    xml.openElement("photo");
					        xml.appendTextNode("photoLink", photoData.getLink());
					        xml.appendTextNode("photoSource", photoData.getSource() + "&size=thumb");
					        xml.appendTextNode("photoCaption", photoData.getCaption());
					        xml.closeElement();
						} 
					}
					xml.closeElement();
				}			    
			    xml.closeElement();
			}
		}
		xml.closeElement();
	}
	
	private void writeChatMessage(XmlBuilder xml, ChatMessage m) {
		xml.appendTextNode("message", m.getMessageText(), "fromId", m.getFromUser().getId(),
				"fromNickname", m.getFromUser().getNickname(),
				"timestamp", Long.toString(m.getTimestamp().getTime()),
				"serial", Long.toString(m.getId()));		
	}
	
	public void getGroupChatSummary(XmlBuilder xml, UserViewpoint viewpoint, String groupId) throws XmlMethodException {
		Group group = parseGroupId(viewpoint, groupId);
		xml.openElement("groupChat", "groupId", group.getId());
		returnGroupsXml(xml, viewpoint, Collections.singleton(group));
		List<GroupMessage> messages = groupSystem.getNewestGroupMessages(group, 5);
		for (GroupMessage gm : messages) {
			writeChatMessage(xml, gm);
		}
		xml.closeElement();
	}
	
 	public void getPostSummary(XmlBuilder xml, UserViewpoint viewpoint, String postId) throws XmlMethodException {
 		Post post = parsePostId(viewpoint, postId);
 		
 		PostView pv = postingBoard.getPostView(viewpoint, post);
 		pv.writeToXmlBuilderOld(xml);
 		EntityView poster = pv.getPoster();
 		poster.writeToXmlBuilderOld(xml);
 		
		List<PostMessage> messages = postingBoard.getNewestPostMessages(post, 5);
		for (PostMessage pm : messages) {
			writeChatMessage(xml, pm);
		}
 	}
 	
 	public void doSetBlockHushed(XmlBuilder xml, UserViewpoint viewpoint, UserBlockData userBlockData, boolean hushed) throws XmlMethodException, NotFoundException {
 		if (hushed != userBlockData.isIgnored()) {
	 		userBlockData.setIgnored(hushed);
	 		if (hushed)
	 			userBlockData.setIgnoredTimestampAsLong(userBlockData.getBlock().getTimestampAsLong());
 		}
 		// send the new block data back, to avoid an extra round trip
 		returnBlocks(xml, viewpoint, viewpoint.getViewer(), Collections.singletonList(stacker.loadBlock(viewpoint, userBlockData)));
 	}
 	
 	@TransactionAttribute(TransactionAttributeType.NEVER)
 	public void doDeleteFile(XmlBuilder xml, UserViewpoint viewpoint, Guid fileId) throws XmlMethodException {
 		// Transaction 1 - set state to DELETING
 		try {
			sharedFileSystem.setFileState(viewpoint, fileId, StorageState.DELETING, -1);
		} catch (NotFoundException e) {
			throw new XmlMethodException(XmlMethodErrorCode.NOT_FOUND, "No such file");
		} catch (PermissionDeniedException e) {
			throw new XmlMethodException(XmlMethodErrorCode.FORBIDDEN, "You aren't allowed to delete this file");
		}
		try {
			// Outside transaction - remove from local or remote file storage
			sharedFileSystem.deleteFileOutsideDatabase(fileId);
		} catch (Exception e) {
			logger.error("Failed to delete file", e);
			try {
				// Transaction 2 - emergency revert of DELETING state
				sharedFileSystem.setFileState(viewpoint, fileId, StorageState.STORED, -1);
			} catch (Exception e2) {
				logger.error("Failed in last-ditch effort to fix state of file {}, must be set back to STORED manually",
						fileId);
				logger.error("Exception was", e2);
			}
			throw new XmlMethodException(XmlMethodErrorCode.FAILED, "Something went wrong while deleting this file");
		}
		// Transaction 3 - set the state to DELETED to indicate success
		try {
			sharedFileSystem.setFileState(viewpoint, fileId, StorageState.DELETED, -1);
		} catch (Exception e) {
			logger.error("Successfully deleted file {} but failed to set its state to DELETED", fileId);
			logger.error("Exception was", e);
			// don't return an error from the method, this will appear to have worked from user 
			// standpoint
		}
 	}
}
