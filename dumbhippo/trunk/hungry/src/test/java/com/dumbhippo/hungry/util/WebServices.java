package com.dumbhippo.hungry.util;

import java.io.IOException;
import java.net.MalformedURLException;

import org.xml.sax.SAXException;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebClient;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

public class WebServices {
	
	private String baseurl;
	
	public WebServices() {
		baseurl = Config.getDefault().getValue(ConfigValue.BASEURL);
	}
	
	private WebClient newConversation() {	
    	WebClient wc = new WebConversation();
    	
    	return wc;
	}
	
	public String getTextPOST(String relativeUrl, String... parameters) throws WebServicesException {
		
		if (!relativeUrl.startsWith("/"))
			throw new IllegalArgumentException("relative url should start with / " + relativeUrl);
		
		if ((parameters.length % 2) != 0)
    		throw new IllegalArgumentException("parameters come in key-value pairs");
		
		WebClient wc = newConversation();
    	 
    	PostMethodWebRequest req = new PostMethodWebRequest(baseurl + "/text" + relativeUrl);
    	
    	for (int i = 0; i < parameters.length; i += 2) {
    		req.setParameter(parameters[i], parameters[i+1]);
    	}
    	
		WebResponse response;
		try {
			response = wc.sendRequest(req);
		} catch (MalformedURLException e) {
			throw new WebServicesException("error sending request", e);
		} catch (IOException e) {
			throw new WebServicesException("error sending request", e);
		} catch (SAXException e) {
			throw new WebServicesException("error sending request", e);
		}
		
		if (!response.getContentType().equals("text/plain")) {
			throw new WebServicesException("Wrong content type " + response.getContentType()); 
		}
		
		try {
			return response.getText();
		} catch (IOException e) {
			throw new WebServicesException("error reading response", e);
		}
	}
	
}
