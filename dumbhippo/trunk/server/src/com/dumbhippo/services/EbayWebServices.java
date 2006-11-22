package com.dumbhippo.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.URLUtils;
import com.dumbhippo.XmlBuilder;

public class EbayWebServices {
	static private final Logger logger = GlobalSetup.getLogger(EbayWebServices.class);
	
	private SAXParserFactory saxFactory;
	
	private String devId;
	private String appId;
	private String certId;
	private int timeoutMilliseconds;
	private String username;
	private String password;
	private String userToken;
	
	EbayWebServices(String devId, String appId, String certId, int timeoutMilliseconds,
			String username, String password, String userToken) {
		this.devId = devId;
		this.appId = appId;
		this.certId = certId;
		this.timeoutMilliseconds = timeoutMilliseconds;
		this.username = username;
		this.password = password;
		this.userToken = userToken;
	}
	
	private SAXParser newSAXParser() {
		if (saxFactory == null)
			saxFactory = SAXParserFactory.newInstance();
		try {
			return saxFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			logger.error("creating ebay sax parser {}", e.getMessage());
			throw new RuntimeException(e);
		} catch (SAXException e) {
			logger.error("creating ebay sax parser {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Makes a request to eBay, jumping through all their weird hoops.
	 * 
	 * @param callName the request name
	 * @param requestBody the xml for the body
	 * @return the input stream or null
	 */
	private InputStream ebayRequest(String callName, String requestBody) {
		URL url;
		try {
			// real API URL:    https://api.ebay.com/ws/api.dll
			// sandbox API URL: https://api.sandbox.ebay.com/ws/api.dll
			url = new URL("https://api.sandbox.ebay.com/ws/api.dll");
			HttpsURLConnection connection = (HttpsURLConnection) URLUtils.openConnection(url);
			connection.setConnectTimeout(timeoutMilliseconds);
			connection.setReadTimeout(timeoutMilliseconds);
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
			
			//logger.debug("Sending call {} to {}", callName, url);
			OutputStream os = connection.getOutputStream();
			os.write(contentBytes);
			os.flush();
			os.close();
		
			//logger.debug("Reading reply");
			// we block at this point to get response code...
			//logger.debug("Result code " + connection.getResponseCode());
			//logger.debug("Result message " + connection.getResponseMessage());
			//logger.debug("Content length " + connection.getContentLength());
			//logger.debug("Content type " + connection.getContentType());
			return connection.getInputStream();
		} catch (IOException e) {
			logger.warn("exception in ebay web services request {}", e.getMessage());
			return null;
		}
	}
	
	EbaySaxHandler ebayParsedRequest(String callName, String requestBody) {
		InputStream is = ebayRequest(callName, requestBody);
		if (is == null)
			return null;
		
		SAXParser parser = newSAXParser();
		EbaySaxHandler handler = new EbaySaxHandler();
		
		try {
			parser.parse(is, handler);
		} catch (SAXException e) {
			logger.error("Failed to parse ebay return stuff: {}", e.getMessage());
			return null;
		} catch (IOException e) {
			logger.warn("io error talking to ebay: {}", e.getMessage());
			return null;
		}
		
		//logger.debug("eBay request complete");
		return handler;		
	}
		
	Date geteBayOfficialTime() {
		EbaySaxHandler handler = ebayParsedRequest("GeteBayOfficialTime", null);
		Date ret = null;
		if (handler != null)
			ret = handler.getTimestamp();
		
		if (ret != null) {
			// sanity-check this thing to catch timezone, 
			// daylight savings, server misconfiguration mistakes
			long t = ret.getTime();
			long now = System.currentTimeMillis();
			
			if (Math.abs(t - now) > (1000 * 60 * 59)) {
				logger.error("eBay is more than an hour out of sync with our time; something is going very wrong");
				logger.error("eBay time is: {}", ret);
				logger.error("our time is: {}", new Date(now));
				ret = null;
			}
		}
		
		return ret;
	}
	
	static public final void main(String[] args) {
		EbayWebServices webServices =
			new EbayWebServices("",
					"",
					"", 10000,
					null, null, 
					"");
		Date eBayTime = webServices.geteBayOfficialTime();
		if (eBayTime != null)
			System.out.println("eBay time is: " + eBayTime);
		else
			System.out.println("failed to load eBay time");
	}
}
