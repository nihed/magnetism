package com.dumbhippo.server.downloads;

import java.util.regex.Pattern;

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
	
	private DownloadConfiguration configuration;
	private DownloadPlatform currentPlatform;
	private DownloadDistribution[] currentDistributions;
	
	public enum Element {
		downloads,
		platform,
		distribution,
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
			
			currentPlatform = new DownloadPlatform();
			currentPlatform.setName(name);
			currentPlatform.setVersion(version);
			currentPlatform.setMinimum(minimum);
			currentPlatform.setDate(date);
		
			break;
		}
		case distribution:
		{
			if (parent() != Element.platform)
				throw new SAXParseException("<distribution/> can only occur inside <platform/>", getDocumentLocator());

			Attributes attributes = currentAttributes();
			String name = attributes.getValue("name"); 
			String release = attributes.getValue("release");
			
			// osVersion can be a list like '5,6', which means add one set of these
			// downloads for version 5, and one for version 6. The resulting downloads
			// will have different URLs if they contain %{osVersion} substitutions.
			String osVersionString = attributes.getValue("osVersion");
			String[] osVersions;
			if (osVersionString != null)
				osVersions = SEPARATOR_REGEX.split(osVersionString);
			else
				osVersions = new String[] { null };
			
			String osVersionPatternString = attributes.getValue("osVersionPattern");
			String[] osVersionPatterns;
			if (osVersionPatternString != null) {
				osVersionPatterns = SEPARATOR_REGEX.split(osVersionPatternString);
				if (osVersionPatterns.length != osVersions.length) {
					logger.debug("==== {} {}", osVersionString, osVersionPatternString);
					throw new SAXParseException("osVersion and osVersionPattern elements must have the same number of elements", getDocumentLocator());
				}
			} else {
				osVersionPatterns = new String[osVersions.length];
			}
			
			currentDistributions = new DownloadDistribution[osVersions.length];
			for (int i = 0; i < osVersions.length; i++) {
				currentDistributions[i] = new DownloadDistribution(currentPlatform);
				currentDistributions[i].setName(name);
				currentDistributions[i].setOsVersion(osVersions[i]);
				currentDistributions[i].setOsVersionPattern(osVersionPatterns[i]);
				currentDistributions[i].setRelease(release);
			}
			
			break;
		}
		case download:
			if (parent() != Element.distribution)
				throw new SAXParseException("<download/> can only occur inside <distribution/>", getDocumentLocator());
			
			break;
		}
	};

	static final private Pattern SEPARATOR_REGEX = Pattern.compile("\\s*,\\s*");
	
	@Override
	protected void closeElement(Element element) throws SAXException {
		switch(element) {
		case downloads:
			break;
			
		case platform:
			logger.debug("Adding DownloadPlatform {}", currentPlatform.getName());
			configuration.addPlatform(currentPlatform);
			currentPlatform = null;
			break;
			
		case distribution:
		{
			for (DownloadDistribution distribution : currentDistributions) {
				currentPlatform.addDistribution(distribution);
				logger.debug("Adding DownloadDistribution {}/{}", distribution.getName(), distribution.getOsVersion());
			}
			
			currentDistributions = null;
			break;
		}
			
		case download:
			Attributes attributes = currentAttributes();
			String url = attributes.getValue("url");

			// We use separate attributes in the XML: type='source' or architecture='i386', but
			// internally we overload both into the same architecture field internally. 
			
			String architectureString = attributes.getValue("architecture");
			String typeString = attributes.getValue("type");
			if (architectureString != null) {
				 if (typeString != null && !typeString.equals("binary"))
					 throw new SAXParseException("Architecture is only valid for a type of binary", getDocumentLocator());

				 if (architectureString.equalsIgnoreCase("source"))
					 throw new SAXParseException("'source' is not a valid architecture; use type='source' instead", getDocumentLocator()); 
			}
			if (typeString != null) {
				if (typeString.equals("binary")) {
					// Default
				} else if (typeString.equals("source")) {
					architectureString = "source";
				} else {
					throw new SAXParseException("type attribute must be one of 'binary' or 'source'", getDocumentLocator());
				}
			}
			
			// We allow a comma-separated list of architectures and explode than into a separate
			// download per architecture.
			String[] architectures;
			if (architectureString != null)
				architectures = SEPARATOR_REGEX.split(architectureString);
			else
				architectures = new String[] { null };

			for (DownloadDistribution distribution : currentDistributions) {
				for (String architecture : architectures) {
					Download download = new Download(distribution);
					download.setArchitecture(architecture);
					
					if (url != null) {
						try {
							download.setUrl(url);
						} catch (ValidationException e) {
							throw new SAXParseException("url attribute failed validation: " + e.getMessage(), getDocumentLocator());
						}
					}
				
					distribution.addDownload(download);
				}
			}
			
			break;
		}
	};
}
