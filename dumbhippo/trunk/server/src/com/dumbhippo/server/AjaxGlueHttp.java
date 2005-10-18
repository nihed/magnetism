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
public interface AjaxGlueHttp {
	
	@HttpContentTypes("text/xml")
	@HttpParams({"entryContents"})
	public void getFriendCompletions(OutputStream out, String contentType, String entryContents) throws IOException;
}
