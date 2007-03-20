package com.dumbhippo.server.downloads;

import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.ValidationException;

public class DownloadSaxHandler extends EnumSaxHandler<DownloadSaxHandler.Element> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(DownloadSaxHandler.class);
	
	DownloadConfiguration configuration;
	DownloadPlatform currentPlatform;
	
	public enum Element {
		downloads,
		platform,
		download
	}
	
	public DownloadSaxHandler(DownloadConfiguration configuration) {
		super(Element.class);
		
		this.configuration = configuration;
	}

	@Override
	protected void openElement(Element element) throws SAXException {
		switch(element) {
		case downloads:
			if (parent() != null)
				throw new SAXParseException("<downloads/> can only occur at the toplevel", getDocumentLocator());
			break;
		case platform:
		{
			if (parent() != Element.downloads)
				throw new SAXParseException("<platform/> can only occur inside <downloads/>", getDocumentLocator());
			
			Attributes attributes = currentAttributes();
			String name = attributes.getValue("name"); 
			String version = attributes.getValue("version");
			String minimum = attributes.getValue("minimum");
			String date = attributes.getValue("date");
			if (name == null || version == null || minimum == null)
				throw new SAXParseException("name, version, and minimum attributes are mandatory for <platform/>", getDocumentLocator());
			
			currentPlatform = new DownloadPlatform(name, version, date, minimum);
			break;
		}
		case download:
			if (parent() != Element.platform)
				throw new SAXParseException("<download/> can only occur inside <platform/>", getDocumentLocator());
			break;
		}
	};

	@Override
	protected void closeElement(Element element) throws SAXException {
		switch(element) {
		case downloads:
			break;
		case platform:
			currentPlatform = null;
			break;
		case download:
			Attributes attributes = currentAttributes();
			String distribution = attributes.getValue("distribution"); 
			String osVersion = attributes.getValue("osVersion");
			String architecture = attributes.getValue("architecture");
			String release = attributes.getValue("release");
			String url = attributes.getValue("url");
			
			Download download = new Download(currentPlatform, distribution, osVersion, architecture, release);
			if (url != null) {
				try {
					download.setUrl(url);
				} catch (ValidationException e) {
					throw new SAXParseException("url attribute failed validation: " + e.getMessage(), getDocumentLocator());
				}
			}
			
			configuration.addDownload(download);
			break;
		}
	};
}
