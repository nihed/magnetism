package com.dumbhippo.services;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.URLUtils;

class AbstractXmlRequest<SaxHandlerT extends DefaultHandler> {
	
	static private final Logger logger = GlobalSetup.getLogger(AbstractXmlRequest.class);
	
	protected int timeoutMilliseconds;
	
	protected AbstractXmlRequest(int timeoutMilliseconds) {
		this.timeoutMilliseconds = timeoutMilliseconds;
	}
	
	protected SaxHandlerT parseUrl(SaxHandlerT handler, String url) {
		try {
			URLConnection connection = URLUtils.openConnection(new URL(url));
			connection.setConnectTimeout(timeoutMilliseconds);
			connection.setReadTimeout(timeoutMilliseconds);			
			
			EnumSaxHandler.parse(connection.getInputStream(), handler);
			//logger.debug("Successfully parsed web service URL contents");
		} catch (ServiceException e) {
			// if this trace is being logged for normal stuff, you need to 
			// make the appropriate SAX handler throw ServiceException 
			// with isUnexpected = false when it gets the unremarkable/normal error
			if (e.isUnexpected())
				logger.warn("unexpected error from web service", e);
			else
				logger.debug("normal/expected error from web service: {}", e.getMessage());
			return null;
		} catch (SAXException e) {
			// The Yahoo search services in particular sometimes send empty documents
			// for "no results"; special case this to avoid cluttering the logs with
			// backtraces.
			if (e.getMessage() != null && e.getMessage().indexOf("Premature end of file") >= 0)
				logger.warn("Web service sent an incomplete XML document");
			else
				logger.warn("parse error on web server reply", e);
			return null;
		} catch (IOException e) {
			logger.debug("IO error talking to web server: {}", e.getMessage());
			return null;
		}
		
		return handler;
	}
}
