package com.dumbhippo.server;

import java.io.IOException;
import java.io.OutputStream;

import javax.ejb.Local;

/**
 * Methods publicly exported to the internet via http GET and POST
 * 
 * @author hp
 *
 */
@Local
public interface AjaxGlueHttp extends LoginRequired {
	
	@HttpContentTypes(HttpResponseData.XML)
	@HttpParams({"entryContents"})
	public void getFriendCompletions(OutputStream out, HttpResponseData contentType, String entryContents) throws IOException;
	
	@HttpContentTypes(HttpResponseData.NONE)
	@HttpParams({"url", "recipients", "description"})
	public void doShareLink(String url, String recipients, String description);
}
