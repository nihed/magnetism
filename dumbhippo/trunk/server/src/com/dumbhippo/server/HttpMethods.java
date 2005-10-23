package com.dumbhippo.server;

import java.io.IOException;
import java.io.OutputStream;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

/**
 *  - Methods must be named getFoo or doFoo
 *  
 *  - the args to a method are: OutputStream,HttpResponseData pair; Person
 * logged in user; http params
 * 
 *  - the OutputStream,HttpResponseData can be omitted if you only return
 * content type NONE
 * 
 *  - if you have the Person arg then you require login for the method to work
 * 
 */
@Local
public interface HttpMethods {

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "entryContents" })
	public void getFriendCompletions(OutputStream out, HttpResponseData contentType, String entryContents)
			throws IOException;

	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams( { "name", "members" })
	public void doCreateGroup(OutputStream out, HttpResponseData contentType, Person user, String name, String memberIds)
			throws IOException, ParseException, GuidNotFoundException;

	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams( { "url", "recipients", "description" })
	public void doShareLink(Person user, String url, String recipientIds, String description) throws ParseException, GuidNotFoundException;
}
