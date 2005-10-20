package com.dumbhippo.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostingBoard;

@Local
public interface HttpMethods {
	
	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams({"entryContents"})	
	public void getFriendCompletions(OutputStream out, HttpResponseData contentType, String entryContents) throws IOException;
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams({"url", "recipients", "description"})	
	public void doShareLink(Person user, String url, String recipientIds, String description);
}
