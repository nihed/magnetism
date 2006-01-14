package com.dumbhippo.services;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.dumbhippo.GlobalSetup;

public class AbstractWebServices<SaxHandlerT extends DefaultHandler> {
	
	static private final Log logger = GlobalSetup.getLog(AbstractWebServices.class);
	
	private SAXParserFactory saxFactory;
	private int timeoutMilliseconds;
	
	protected AbstractWebServices(int timeoutMilliseconds) {
		this.timeoutMilliseconds = timeoutMilliseconds;
	}
	
	protected SAXParser newSAXParser() {
		if (saxFactory == null)
			saxFactory = SAXParserFactory.newInstance();
		try {
			return saxFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			logger.trace(e);
			throw new RuntimeException(e);
		} catch (SAXException e) {
			logger.trace(e);
			throw new RuntimeException(e);
		}
	}

	protected SaxHandlerT parseUrl(SaxHandlerT handler, String url) {
		SAXParser parser = newSAXParser();
		try {
			URL u = new URL(url);
			URLConnection connection = u.openConnection();

			connection.setConnectTimeout(timeoutMilliseconds);
			connection.setReadTimeout(timeoutMilliseconds);
			connection.setAllowUserInteraction(false);
			
			parser.parse(connection.getInputStream(), handler);
			logger.debug("Successfully parsed web service URL contents");
		} catch (SAXException e) {
			logger.warn("parse error on web server reply", e);
			return null;
		} catch (IOException e) {
			logger.warn("IO error talking to web server", e);
			return null;
		}
		
		return handler;
	}
}
