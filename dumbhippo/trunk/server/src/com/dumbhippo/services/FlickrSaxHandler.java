package com.dumbhippo.services;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;

public class FlickrSaxHandler extends EnumSaxHandler<FlickrSaxHandler.Element> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FlickrSaxHandler.class);
	
	// The enum names should match the xml element names (including case).
	// indentation roughly matches xml nesting ;-)
	enum Element {
		rsp,
			err,
			user,
				username,
			photos,
				photo,
		IGNORED // an element we don't care about
	};	

	private boolean failed;
	private String errorMessage;
	private String errorCode;
	private String nsid;
	private String username;
	
	FlickrSaxHandler() {
		super(Element.class, Element.IGNORED);
	}

	@Override
	protected void openElement(Element c) throws SAXException {
		if (c == Element.rsp) {
			String stat = currentAttributes().getValue("stat");
			if (stat != null && stat.equals("fail"))
				failed = true;
			else
				failed = false;
		}
	}
	
	
	@Override
	protected void closeElement(Element c) throws SAXException {
		if (failed) {
			if (c == Element.err) {
				errorMessage = currentAttributes().getValue("msg");
				errorCode = currentAttributes().getValue("code");
			}
		} else {
			if (c == Element.user) {
				nsid = currentAttributes().getValue("nsid");
			} else if (c == Element.username) {
				username = getCurrentContent();
			}
		}
	}

	@Override 
	public void endDocument() throws SAXException {
		if (failed)
			throw new ServiceException(false, "flickr error " + errorCode + ": " + errorMessage);
	}

	public String getNsid() {
		return nsid;
	}

	public String getUsername() {
		return username;
	}
}
