package com.dumbhippo.hungry.util;

import java.io.IOException;
import java.net.MalformedURLException;

import net.sourceforge.jwebunit.WebTester;

import org.xml.sax.SAXException;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebClient;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class WebServices {
	
	private String baseurl;
	private WebClient webClient;
	
	public WebServices(WebClient webClient, String baseurl) {
		this.webClient = webClient;
		this.baseurl = baseurl;
	}
	
	public WebServices() {
		baseurl = Config.getDefault().getValue(ConfigValue.BASEURL);
		webClient = new WebConversation();
	}

	public WebServices(WebTester tester) {
		baseurl = Config.getDefault().getValue(ConfigValue.BASEURL);
		webClient = tester.getTestContext().getWebClient();
	}
	
	private void marshalParameters(WebRequest req, String... parameters) {
		if ((parameters.length % 2) != 0)
    		throw new IllegalArgumentException("parameters come in key-value pairs");
    	
		for (int i = 0; i < parameters.length; i += 2) {
    		req.setParameter(parameters[i], parameters[i+1]);
    	}		
	}
	
	private String fullUrl(String prefix, String relativeUrl) {
		if (!relativeUrl.startsWith("/"))
			throw new IllegalArgumentException("relative url should start with / " + relativeUrl);
		
		return baseurl + prefix + relativeUrl;
	}
	
	private WebResponse doRequest(WebRequest req, String requiredResponseType) throws WebServicesException {
		WebResponse response;
		try {
			response = webClient.sendRequest(req);
		} catch (MalformedURLException e) {
			throw new WebServicesException("error sending request", e);
		} catch (IOException e) {
			throw new WebServicesException("error sending request", e);
		} catch (SAXException e) {
			throw new WebServicesException("error sending request", e);
		}

		if (response.getResponseCode() != 200)
			throw new WebServicesException(req + ": HTTP response code " + response.getResponseCode() + ": " + response.getResponseMessage());
		
		if (requiredResponseType != null) {
			if (!response.getContentType().equals(requiredResponseType)) {
				throw new WebServicesException("Wrong content type " + response.getContentType() + " expected " + requiredResponseType); 
			}
		}
		
		return response;
	}
	
	public void doPOST(String relativeUrl, String... parameters) throws WebServicesException {
		PostMethodWebRequest req = new PostMethodWebRequest(fullUrl("/action", relativeUrl));
    	marshalParameters(req, parameters);
    	
    	doRequest(req, null);
	}
	
	public String getTextPOST(String relativeUrl, String... parameters) throws WebServicesException {
    	PostMethodWebRequest req = new PostMethodWebRequest(fullUrl("/text", relativeUrl));
    	marshalParameters(req, parameters);
    	
		WebResponse response;
		response = doRequest(req, "text/plain");
		
		try {
			return response.getText();
		} catch (IOException e) {
			throw new WebServicesException("error reading response", e);
		}
	}
	
	public String getXmlPOST(String relativeUrl, String... parameters) throws WebServicesException {
    	PostMethodWebRequest req = new PostMethodWebRequest(fullUrl("/xml", relativeUrl));
    	marshalParameters(req, parameters);
    	
		WebResponse response;
		response = doRequest(req, "text/xml");
		
		try {
			return response.getText();
		} catch (IOException e) {
			throw new WebServicesException("error reading response", e);
		}
	}
}
