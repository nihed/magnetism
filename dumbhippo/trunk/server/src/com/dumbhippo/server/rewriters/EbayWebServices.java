package com.dumbhippo.server.rewriters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;

class EbayWebServices {
	static private final Log logger = GlobalSetup.getLog(EbayWebServices.class);
	
	private SAXParserFactory saxFactory;
	
	private String devId;
	private String appId;
	private String certId;
	
	EbayWebServices(String devId, String appId, String certId) {
		this.devId = devId;
		this.appId = appId;
		this.certId = certId;
	}
	
	private SAXParser newSAXParser() {
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
	
	/**
	 * Makes a request to eBay, jumping through all their weird hoops.
	 * 
	 * @param callName the request name
	 * @param username username of a user you made up just to do auth calls
	 * @param password password of said made-up user
	 * @param userToken required for all calls except auth calls, token of an actual user
	 * @param requestBody the xml for the body
	 * @return the input stream or null
	 */
	private InputStream ebayRequest(String callName, String username, String password,
			String userToken, String requestBody) {
		URL url;
		try {
			// real API URL:    https://api.ebay.com/ws/api.dll
			// sandbox API URL: https://api.sandbox.ebay.com/ws/api.dll
			url = new URL("https://api.sandbox.ebay.com/ws/api.dll");
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(1000 * 6); // don't block very long
			connection.setRequestMethod("POST");
			connection.setAllowUserInteraction(false);
			connection.setDoOutput(true);
			connection.addRequestProperty("X-EBAY-API-COMPATIBILITY-LEVEL", "427");
			connection.addRequestProperty("X-EBAY-API-SESSION-CERTIFICATE", 
					devId + ";" + appId + ";" + certId);
			connection.addRequestProperty("X-EBAY-API-DEV-NAME", devId);
			connection.addRequestProperty("X-EBAY-API-APP-NAME", appId);
			connection.addRequestProperty("X-EBAY-API-CERT-NAME", certId);
			connection.addRequestProperty("X-EBAY-API-CALL-NAME", callName);
			connection.addRequestProperty("X-EBAY-API-SITEID", "0");
			connection.addRequestProperty("Content-Type", "text/xml");
			
			XmlBuilder xml = new XmlBuilder();
			xml.appendStandaloneFragmentHeader();
			xml.openElement(callName + "Request",
					"xmlns", "urn:ebay:apis:eBLBaseComponents");
			xml.openElement("RequesterCredentials");
			if (userToken != null) {
				xml.appendTextNode("eBayAuthToken", userToken);
			} else {
				if (username == null || password == null)
					throw new IllegalArgumentException("missing username/password/token");
				xml.appendTextNode("Username", username);
				xml.appendTextNode("Password", password);
			}
			xml.closeElement();
			if (requestBody != null)
				xml.append(requestBody);
			xml.closeElement();
			xml.append("\n");

			System.out.println(connection.getRequestProperties());
			System.out.println(xml.toString());
			
			byte[] contentBytes = xml.getBytes();
			
			connection.addRequestProperty("Content-Length", Integer.toString(contentBytes.length));
			
			logger.debug("Sending call " + callName + " to " + url);
			OutputStream os = connection.getOutputStream();
			os.write(contentBytes);
			os.flush();
			os.close();
		
			logger.debug("Reading reply");
			// we block at this point to get response code...
			logger.debug("Result code " + connection.getResponseCode());
			logger.debug("Result message " + connection.getResponseMessage());
			logger.debug("Content length " + connection.getContentLength());
			logger.debug("Content type " + connection.getContentType());
			return connection.getInputStream();
		} catch (IOException e) {
			logger.warn("exception in ebay web services request", e);
			return null;
		}
	}
	
	EbaySaxHandler ebayParsedRequest(String callName, String username, String password, String userToken) {
		InputStream is = ebayRequest(callName, username, password, userToken, null);
		if (is == null)
			return null;
		
		SAXParser parser = newSAXParser();
		EbaySaxHandler handler = new EbaySaxHandler();
		
		try {
			parser.parse(is, handler);
		} catch (SAXException e) {
			logger.trace(e);
			return null;
		} catch (IOException e) {
			logger.trace(e);
			return null;
		}
		
		logger.debug("eBay request complete");
		return handler;		
	}
	
	String getRuName(String username, String password) {
		EbaySaxHandler handler =
			ebayParsedRequest("SetRuName",
								username, password, null);
		return null;
	}
	
	EbayItemData frobate() {
		EbaySaxHandler handler = ebayParsedRequest("GeteBayOfficialTime",
				null, null, null);
		return handler;
	}
	
	static public final void main(String[] args) {
		EbayWebServices webServices =
			new EbayWebServices("",
					"",
					"");
		String ruName = webServices.getRuName("", "");
		/*EbayItemData item = webServices.frobate();
		if (item != null)
			System.out.println("Picture url is " + item.getPictureUrl());
		else
			System.out.println("failed to load item");
			*/
	}
}
