package com.dumbhippo.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;

import org.xml.sax.SAXException;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid.ParseException;

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
	@HttpParams( { "groupId" })
	public void getAddableContacts(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String groupId)
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
	@HttpParams( { "name", "members", "secret", "description" })
	@HttpOptions( optionalParams = { "members", "description" } )
	public void doCreateGroup(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String name, String memberIds, boolean secret, String description)
			throws IOException, ParseException, NotFoundException;

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "groupId", "members" })
	public void doAddMembers(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String groupId, String memberIds)
			throws IOException, ParseException, NotFoundException;

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "title", "url", "recipients", "description", "isPublic", "postInfoXml" })
	@HttpOptions( optionalParams = { "postInfoXml" } )
	public void doShareLink(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String title, String url, String recipientIds, String description, boolean isPublic, String postInfoXml) throws ParseException,
			NotFoundException, SAXException, MalformedURLException, IOException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId", "recipients", "description" })
	public void doShareGroup(UserViewpoint viewpoint, String groupId, String recipientIds, String description) throws ParseException,
			NotFoundException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "name" })
	@HttpOptions(invalidatesSession = true)
	public void doRenamePerson(UserViewpoint viewpoint, String name);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "contactId" })
	public void doAddContactPerson(UserViewpoint viewpoint, String contactId);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "contactId" })
	public void doRemoveContactPerson(UserViewpoint viewpoint, String contactId);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId" })
	public void doJoinGroup(UserViewpoint viewpoint, String groupId);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId" })
	public void doLeaveGroup(UserViewpoint viewpoint, String groupId);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId", "name" })
	public void doRenameGroup(UserViewpoint viewpoint, String groupId, String name);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId", "description" })
	public void doSetGroupDescription(UserViewpoint viewpoint, String groupId, String name);

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId", "photo" })
	public void doSetGroupStockPhoto(UserViewpoint viewpoint, String groupId, String photo);

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "email" })
	public void doAddContact(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String email) throws IOException;

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "address" })
	public void doSendLoginLinkEmail(XmlBuilder xml, String address) throws IOException, HumanVisibleException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "address" })
	public void doSendLoginLinkAim(String address) throws IOException, HumanVisibleException;

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
	@HttpParams( { "disabled" })
	@HttpOptions( allowDisabledAccount = true )
	public void doSetAccountDisabled(UserViewpoint viewpoint, boolean disabled) throws IOException, HumanVisibleException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "password" })
	public void doSetPassword(UserViewpoint viewpoint, String password) throws IOException, HumanVisibleException;
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "name" })
	public void doSetMySpaceName(UserViewpoint viewpoint, String name) throws IOException;
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "enabled" })
	public void doSetMusicSharingEnabled(UserViewpoint viewpoint, boolean enabled) throws IOException;
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "notify" })
	public void doSetNotifyPublicShares(UserViewpoint viewpoint, boolean notify) throws IOException;
	
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
	public void doSendGroupInvitation(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String groupId, String inviteeId, String inviteeAddress, String subject, String message) throws IOException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "address", "suggestedGroupIds", "desuggestedGroupIds" })
	public void doSuggestGroups(UserViewpoint viewpoint, String address, String suggestedGroupIds, String desuggestedGroupIds);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "userId" })
	public void doSendRepairEmail(UserViewpoint viewpoint, String userId);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( {} )
	public void doReindexAll(UserViewpoint viewpoint);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "postId", "favorite" })
	public void doSetFavoritePost(UserViewpoint viewpoint, String postId, boolean favorite);
	
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
	public void getGroupPhoto(OutputStream out, HttpResponseData contentType, String groupId, String size)
		throws IOException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( {} )
	@HttpOptions( allowDisabledAccount = true )
	public void doAcceptTerms(UserViewpoint viewpoint);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "userId", "disabled" } )
	@HttpOptions( adminOnly = true )
	public void doSetAdminDisabled(UserViewpoint viewpoint, String userId, boolean disabled);
		
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "parseOnly", "command" } )
	@HttpOptions( adminOnly = true )
	public void doAdminShellExec(XmlBuilder xml, UserViewpoint viewpoint, HttpServletRequest request, boolean parseOnly, String command) throws IOException, HumanVisibleException;

	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "url" })
	public void doFeedPreview(XmlBuilder xml, UserViewpoint viewpoint, String url) throws XmlMethodException;
	
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "groupId", "url" })
	public void doAddGroupFeed(XmlBuilder xml, UserViewpoint viewpoint, String groupId, String url) throws XmlMethodException;
	
	@HttpContentTypes(HttpResponseData.XMLMETHOD)
	@HttpParams( { "groupId", "url" })
	public void doRemoveGroupFeed(XmlBuilder xml, UserViewpoint viewpoint, String groupId, String url) throws XmlMethodException;	
}
