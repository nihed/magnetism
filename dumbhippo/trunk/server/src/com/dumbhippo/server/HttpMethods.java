package com.dumbhippo.server;

import java.io.IOException;
import java.io.OutputStream;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

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
	public void getAddableContacts(OutputStream out, HttpResponseData contentType, User user, String groupId)
			throws IOException;

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { })
	public void getContactsAndGroups(OutputStream out, HttpResponseData contentType, User user)
			throws IOException;

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "email" })
	public void doCreateOrGetContact(OutputStream out, HttpResponseData contentType, User user, String email)
			throws IOException;
	
	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "name", "members", "secret" })
	public void doCreateGroup(OutputStream out, HttpResponseData contentType, User user, String name, String memberIds, boolean secret)
			throws IOException, ParseException, GuidNotFoundException;

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "groupId", "members" })
	public void doAddMembers(OutputStream out, HttpResponseData contentType, User user, String groupId, String memberIds)
			throws IOException, ParseException, GuidNotFoundException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "title", "url", "recipients", "description", "secret" })
	public void doShareLink(User user, String title, String url, String recipientIds, String description, boolean secret) throws ParseException,
			GuidNotFoundException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId", "recipients", "description" })
	public void doShareGroup(User user, String groupId, String recipientIds, String description) throws ParseException,
			GuidNotFoundException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "name" })
	@HttpOptions(invalidatesSession = true)
	public void doRenamePerson(User user, String name);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "contactId" })
	public void doAddContactPerson(User user, String contactId);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "contactId" })
	public void doRemoveContactPerson(User user, String contactId);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId" })
	public void doJoinGroup(User user, String groupId);
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "groupId" })
	public void doLeaveGroup(User user, String groupId);
	
	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "email" })
	public void doAddContact(OutputStream out, HttpResponseData contentType, User user, String email) throws IOException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "address" })
	public void doSendLoginLinkEmail(String address) throws IOException, HumanVisibleException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "address" })
	public void doSendLoginLinkAim(String address) throws IOException, HumanVisibleException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "disabled" })
	public void doSetAccountDisabled(User user, boolean disabled) throws IOException, HumanVisibleException;
	
	/**
	 * This one is not annotated since it's special-cased in the servlet.
	 * 
	 * @param user the logged-in user or null if not logged in
	 * @param url url to redirect to
	 * @param postId post the person found the url on
	 * @param inviteKey invitation key or null if person is registered
	 * @throws RedirectException if the ids are invalid for example
	 */
	public void handleRedirect(User user, String url, String postId, String inviteKey) throws HumanVisibleException;
}
