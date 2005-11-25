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
		synchronized (AmazonRewriter.class) {
			if (saxFactory == null)
				saxFactory = SAXParserFactory.newInstance();
		}
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
	
	private InputStream ebayRequest(String callName) {
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
			xml.appendTextNode("eBayAuthToken", "foo");
			xml.closeElement();
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
			logger.debug("result code " + connection.getResponseCode());
			logger.debug("result message " + connection.getResponseMessage());
			logger.debug("Content length " + connection.getContentLength());
			logger.debug("Content type " + connection.getContentType());
			return connection.getInputStream();
		} catch (IOException e) {
			logger.warn("exception in ebay web services request", e);
			return null;
		}
	}
	
	EbayItemData frobate() {
		InputStream is = ebayRequest("GeteBayOfficialTime");
		if (is == null)
			return null;
		
		SAXParser parser = newSAXParser();
		EbaySaxHandler handler = new EbaySaxHandler();
		
		try {
			parser.parse(is, handler);
		} catch (SAXException e) {
			logger.trace(e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			logger.trace(e);
			throw new RuntimeException(e);
		}
		
		/*
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		logger.debug("eBay request complete");
		return handler;
	}
	
	static public final void main(String[] args) {
		EbayWebServices webServices = new EbayWebServices("","","");
		webServices.frobate();
	}
}
