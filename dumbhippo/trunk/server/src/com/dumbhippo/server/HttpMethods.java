package com.dumbhippo.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.ejb.Local;

import org.xml.sax.SAXException;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

/**
 * - Methods must be named getFoo or doFoo
 *  - the args to a method are: OutputStream,HttpResponseData pair; User
 * logged in user; http params
 *  - the OutputStream,HttpResponseData can be omitted if you only return
 * content type NONE
 *  - if you have the User arg then you require login for the method to work
 * 
 */
@Local
public interface HttpMethods {

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "groupId", "inviteeId" })
	public void getAddableContacts(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, Group group, String inviteeId)
			throws IOException;

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { })
	public void getContactsAndGroups(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint)
			throws IOException;

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "email" })
	public void doCreateOrGetContact(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String email)
			throws IOException;
	
	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "name", "members", "secret", "open", "description" })
	@HttpOptions( optionalParams = { "members", "description" } )
	public void doCreateGroup(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String name, String memberIds, boolean secret, boolean open, String description)
			throws IOException, ParseException, NotFoundException;

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "groupId", "members" })
	public void doAddMembers(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, Group group, String memberIds)
			throws IOException, ParseException, NotFoundException;

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "title", "url", "recipients", "description", "isPublic", "postInfoXml" })
	@HttpOptions( optionalParams = { "postInfoXml" } )
	public void doShareLink(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String title, String url, String recipientIds, String description, boolean isPublic, String postInfoXml) throws ParseException,
			NotFoundException, SAXException, MalformedURLException, IOException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId", "recipients", "description" })
	public void doShareGroup(UserViewpoint viewpoint, Group group, String recipientIds, String description) throws ParseException,
			NotFoundException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "name" })
	public void doRenamePerson(UserViewpoint viewpoint, String name);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "contactId" })
	public void doAddContactPerson(UserViewpoint viewpoint, String contactId);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "contactObjectId" })
	public void doRemoveContactObject(UserViewpoint viewpoint, String contactObjectId);

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "resourceId" })
	public void doRemoveInvitedContact(UserViewpoint viewpoint, String resourceId);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId" })
	public void doJoinGroup(UserViewpoint viewpoint, Group group);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId" })
	public void doLeaveGroup(UserViewpoint viewpoint, Group group);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId", "open" })
	public void doSetGroupMembershipPolicy(UserViewpoint viewpoint, Group group, boolean open);	
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId", "name" })
	public void doRenameGroup(UserViewpoint viewpoint, Group group, String name);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId", "description" })
	public void doSetGroupDescription(UserViewpoint viewpoint, Group group, String name);

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId", "photo" })
	public void doSetGroupStockPhoto(UserViewpoint viewpoint, Group group, String photo);

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "email" })
	public void doAddContact(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String email) throws IOException;

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "address" })
	public void doSendLoginLinkEmail(XmlBuilder xml, String address) throws IOException, HumanVisibleException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "address" })
	public void doSendClaimLinkEmail(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "address" })
	public void doSendClaimLinkAim(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException;
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "address" })
	public void doRemoveClaimEmail(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "address" })
	public void doRemoveClaimAim(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException;	

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( {} )	
	public void doDisableFacebookSession(UserViewpoint viewpoint) throws IOException, HumanVisibleException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "disabled" })
	@HttpOptions( allowDisabledAccount = true )
	public void doSetAccountDisabled(UserViewpoint viewpoint, boolean disabled) throws IOException, HumanVisibleException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "password" })
	public void doSetPassword(UserViewpoint viewpoint, String password) throws IOException, HumanVisibleException;
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "enabled" })
	public void doSetMusicSharingEnabled(UserViewpoint viewpoint, boolean enabled) throws IOException;
	
	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "who", "theme" })
	@HttpOptions( optionalParams = { "theme" } )
	public void getNowPlaying(OutputStream out, HttpResponseData contentType, String who, String theme)
			throws IOException;

	@HttpContentTypes(HttpResponseData.TEXT)
	@HttpParams( { "basedOn" })
	@HttpOptions( optionalParams = { "basedOn" } )
	public void doCreateNewNowPlayingTheme(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String basedOn)
			throws IOException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "theme" })
	public void doSetNowPlayingTheme(UserViewpoint viewpoint, String themeId) throws IOException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "theme", "key", "value" })
	public void doModifyNowPlayingTheme(UserViewpoint viewpoint, String themeId, String key, String value) throws IOException;
	
	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "address", "promotion" })
	public void doInviteSelf(OutputStream out, HttpResponseData contentType, String address, String promotion) throws IOException;
	
	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "address", "subject", "message", "suggestedGroupIds" })
	public void doSendEmailInvitation(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String address, String subject, String message, String suggestedGroupIds) throws IOException;
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "countToInvite", "subject", "message", "suggestedGroupIds" })	
    @HttpOptions( adminOnly = true )
	public void doInviteWantsIn(String countToInvite, String subject, String message, String suggestedGroupIds) throws IOException;
	
	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "groupId", "inviteeId", "inviteeAddress", "subject", "message" })
	@HttpOptions( optionalParams = { "inviteeId", "inviteeAddress" } )
	public void doSendGroupInvitation(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, Group group, String inviteeId, String inviteeAddress, String subject, String message) throws IOException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "address", "suggestedGroupIds", "desuggestedGroupIds" })
	public void doSuggestGroups(UserViewpoint viewpoint, String address, String suggestedGroupIds, String desuggestedGroupIds);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "userId" })
	public void doSendRepairEmail(UserViewpoint viewpoint, String userId);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( {} )
	@HttpOptions( adminOnly = true, transaction = false )	
	public void doReindexAll(UserViewpoint viewpoint);
	
	@HttpContentTypes(HttpResponseData.TEXT)
	@HttpParams( {  })
	public void getRandomBio(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint)
		throws IOException;

	@HttpContentTypes(HttpResponseData.TEXT)
	@HttpParams( {  })
	public void getRandomMusicBio(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint)
		throws IOException;
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "bio" })
	public void doSetBio(UserViewpoint viewpoint, String bio);

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "musicbio" })
	public void doSetMusicBio(UserViewpoint viewpoint, String bio);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "photo" })
	public void doSetStockPhoto(UserViewpoint viewpoint, String photo);
	
	@HttpContentTypes(HttpResponseData.TEXT)
	@HttpParams( { "userId", "size" })
	public void getUserPhoto(OutputStream out, HttpResponseData contentType, String userId, String size)
		throws IOException;

	@HttpContentTypes(HttpResponseData.TEXT)
	@HttpParams( { "groupId", "size" })
	public void getGroupPhoto(OutputStream out, HttpResponseData contentType, Group group, String size)
		throws IOException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( {} )
	@HttpOptions( allowDisabledAccount = true )
	public void doAcceptTerms(UserViewpoint viewpoint);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "needsDownload" } )
	public void doSetNeedsDownload(UserViewpoint viewpoint, boolean needsDownload);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "userId", "disabled" } )
	@HttpOptions( adminOnly = true )
	public void doSetAdminDisabled(UserViewpoint viewpoint, String userId, boolean disabled);

	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "newFeaturesFlag" } )
	@HttpOptions( adminOnly = true )
	public void doSetNewFeatures(UserViewpoint viewpoint, boolean newFeaturesFlag);
	
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "parseOnly", "transaction", "command" } )
	@HttpOptions( adminOnly = true, transaction = false)	
	public void doAdminShellExec(XmlBuilder xml, UserViewpoint viewpoint, boolean parseOnly, boolean transaction, String command) throws IOException, HumanVisibleException;

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "url" })
	public void doFeedPreview(XmlBuilder xml, UserViewpoint viewpoint, String url) throws XmlMethodException;
	
	@HttpContentTypes(HttpResponseData.TEXT)
	@HttpParams( { "url" })
	public void getFeedDump(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String url) throws HumanVisibleException, IOException;

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "groupId", "url" })
	public void doAddGroupFeed(XmlBuilder xml, UserViewpoint viewpoint, Group group, String url) throws XmlMethodException;
	
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "groupId", "url" })
	public void doRemoveGroupFeed(XmlBuilder xml, UserViewpoint viewpoint, Group group, URL url) throws XmlMethodException;

	/**
	 * Mark an external account as "hated" and give an optional quip about why.
	 * If the quip is missing or empty it's taken as "delete any quip"
	 * 
	 * @param xml
	 * @param viewpoint
	 * @param type
	 * @param quip
	 * @throws XmlMethodException
	 */
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "type", "quip" })
	@HttpOptions( optionalParams = { "quip" } )
	public void doHateExternalAccount(XmlBuilder xml, UserViewpoint viewpoint, String type, String quip) throws XmlMethodException;	

	/**
	 * Mark an external account as "indifferent" which effectively hides both any hate-quip or 
	 * account information from your profile. i.e. this is the same as "removing" an external account.
	 * We don't really remove it though, i.e. the quip/account-info are remembered in case you
	 * switch back to those states.
	 * 
	 * @param xml
	 * @param viewpoint
	 * @param type
	 * @throws XmlMethodException
	 */
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "type" })
	public void doRemoveExternalAccount(XmlBuilder xml, UserViewpoint viewpoint, String type) throws XmlMethodException;	
	
	
	/**
	 * Given an email address, try to lookup the associated flickr account information.
	 * 
	 * @param xml
	 * @param viewpoint
	 * @param email the flickr email address
	 * @throws XmlMethodException
	 */
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "email" })
	public void doFindFlickrAccount(XmlBuilder xml, UserViewpoint viewpoint, String email) throws XmlMethodException;
	
	/**
	 * Adds a flickr account with the given nsid and Flickr email address. Automatically marks
	 * Flickr as "loved" instead of "hated"
	 * 
	 * To get the NSID, you would ask someone for their Flickr email address, then use it to call 
	 * FindFlickrAccount, then you have both email and nsid.
	 * 
	 * @param xml
	 * @param viewpoint
	 * @param nsid
	 * @param email
	 * @throws XmlMethodException
	 */
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "nsid", "email" })
	public void doSetFlickrAccount(XmlBuilder xml, UserViewpoint viewpoint, String nsid, String email) throws XmlMethodException;	
	
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "name" })
	public void doSetMySpaceName(XmlBuilder xml, UserViewpoint viewpoint, String name) throws XmlMethodException;	
	
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "urlOrName" })
	public void doSetLinkedInProfile(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException;

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "urlOrName" })
	public void doSetYouTubeName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException;
	
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "urlOrName" })	
	public void doSetLastFmName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException;	

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "urlOrName" })	
	public void doSetDeliciousName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException;	

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "urlOrName" })	
	public void doSetTwitterName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException;	

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "urlOrName" })	
	public void doSetDiggName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException;

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "urlOrName" })	
	public void doSetRedditName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException;

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "url" })
	public void doSetNetflixFeedUrl(XmlBuilder xml, UserViewpoint viewpoint, String url) throws XmlMethodException;

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "url" })
	public void doSetGoogleReaderUrl(XmlBuilder xml, UserViewpoint viewpoint, String url) throws XmlMethodException;	
	
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "urlOrName" })
	public void doSetPicasaName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException;

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "urlOrUserId" })
	public void doSetAmazonUrl(XmlBuilder xml, UserViewpoint viewpoint, String urlOrUserId) throws XmlMethodException;
	
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "url" })
	public void doSetWebsite(XmlBuilder xml, UserViewpoint viewpoint, URL url) throws XmlMethodException;
	
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "url" })
	public void doSetRhapsodyHistoryFeed(XmlBuilder xml, UserViewpoint viewpoint, String url) throws XmlMethodException;
    
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "url" })
	public void doSetBlog(XmlBuilder xml, UserViewpoint viewpoint, URL url) throws XmlMethodException;	
	
 	@HttpContentTypes(HttpResponseData.XMLMETHOD)
 	@HttpParams( { "filename" })
 	@HttpOptions( optionalParams = { "filename" } )
 	public void getStatisticsSets(XmlBuilder xml, UserViewpoint viewpoint, String filename) throws IOException, XmlMethodException;
 
 	@HttpContentTypes(HttpResponseData.XMLMETHOD)
 	@HttpParams( { "filename", "columns", "start", "end", "timescale" })
 	@HttpOptions( optionalParams = { "filename", "start", "end", "timescale" } )
 	public void getStatistics(XmlBuilder xml, UserViewpoint viewpoint, String filename, String columns, String start, String end, String timescale) throws IOException, XmlMethodException;
 		
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "fileId" })
	@HttpOptions(transaction=false)
	public void doDeleteFile(XmlBuilder xml, UserViewpoint viewpoint, Guid fileId) throws XmlMethodException;
	
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "who", "includeStack", "participantOnly" })
	@HttpOptions( optionalParams = { "includeStack", "participantOnly" } )
	public void getUserSummary(XmlBuilder xml, Viewpoint viewpoint, User who, boolean includeStack, boolean participantOnly) throws XmlMethodException;

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams({ "category" })
	@HttpOptions( optionalParams = { "category" } )	
	public void getPopularApplications(XmlBuilder xml, Viewpoint viewpoint, String category) throws XmlMethodException;
	
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams({ "search" })	
	public void getSearchApplications(XmlBuilder xml, Viewpoint viewpoint, String search) throws XmlMethodException;
	
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams({ })	
	public void getAllApplications(XmlBuilder xml, Viewpoint viewpoint) throws XmlMethodException;		
	
	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "who", "participantOnly" })
	@HttpOptions( optionalParams = { "participantOnly" } )
	public void getUserRSS(OutputStream out, HttpResponseData contentType, User who, boolean participantOnly) throws IOException, XmlMethodException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "enabled" })
	public void doSetApplicationUsageEnabled(UserViewpoint viewpoint, boolean enabled) throws IOException, HumanVisibleException;

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "applicationId", "version", "comment" } )
	public void doRevertApplication(XmlBuilder xml, UserViewpoint viewpoint, String applicationId, Guid version, String comment) throws XmlMethodException;

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { } )
	public void getApplicationEditRSS(OutputStream out, HttpResponseData contentType, Viewpoint viewpoint) throws IOException, XmlMethodException;

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "chatId", "text", "sentiment" })
	public void doAddChatMessage(XmlBuilder xml, UserViewpoint viewpoint, Guid chatId, String text, String sentiment) throws XmlMethodException;
}
