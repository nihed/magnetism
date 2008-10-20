package com.dumbhippo.jive;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xmpp.packet.IQ;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.OnlineAccountType;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.OnlineDesktopSystem;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;


@IQHandler(namespace=AccountsIQHandler.ACCOUNTS_NAMESPACE)
public class AccountsIQHandler extends AnnotatedIQHandler {
	static final String ACCOUNTS_NAMESPACE = "http://mugshot.org/p/accounts";
	
	private static final Logger logger = GlobalSetup.getLogger(AccountsIQHandler.class);	
	
	protected AccountsIQHandler() {
		super("Accounts IQ Handler");
	}	
	
	// TODO: this can return an ExternalAccountDMO, but for that we need to get an ExternalAccount
	// back from a set method we use from HttpMethods, which would require removing throwing an error
	// when checking getReturnType() in HttpMethodsServlet2. Another option is to include an ExternalAccount
	// id in the XmlBuilder. (Though ideally we would move all the logic that happens in set methods in 
	// HttpMethods to the ExernalAccountSystem and use that.)
    // We could then add feedback message as a field in ExternalAccount and make it available as part of the
	// ExternalAccountDMO. (We now omit the feedback messages that can occur when we set Twitter, Reddit,
	// MySpace or Amazon. In any case, these messages are only relevant when the user has Mugshot enabled.)
	@IQMethod(name="addOnlineAccount", type=IQ.Type.set)
	@IQParams({"accountType", "username"})
	public void addOnlineAccount(UserViewpoint viewpoint, String accountType, String username) throws IQException {
		logger.debug("inside addOnlineAccount");

		// Because "google" type is an e-mail address, we need to treat it in a completely 
		// different way from other account types.
		if (accountType.equals("google")) {
		    ClaimVerifier claimVerifier = EJBUtil.defaultLookup(ClaimVerifier.class);
		    OnlineDesktopSystem onlineDesktopSystem = EJBUtil.defaultLookup(OnlineDesktopSystem.class);
		    IdentitySpider identitySpider = EJBUtil.defaultLookup(IdentitySpider.class);
		    try {
			    claimVerifier.sendClaimVerifierLinkEmail(viewpoint, viewpoint.getViewer(), username);
			    // should pass in null for the user, because otherwise run in into a check
			    // that it is only possible to set Google state for e-mails the user owns
			    onlineDesktopSystem.setGoogleServicedEmail(SystemViewpoint.getInstance(), null, identitySpider.getEmail(username), true);
			    return;
		    } catch (HumanVisibleException e) {
		    	throw IQException.createBadRequest(e.getMessage());
		    } catch (RetryException e) {
		    	 throw IQException.createBadRequest("Could not save an account on the server.");
		    } catch (ValidationException e) {
		    	throw IQException.createBadRequest(e.getMessage());
		    }
		}		
		
		ExternalAccountSystem externalAccounts = EJBUtil.defaultLookup(ExternalAccountSystem.class);
		HttpMethods httpMethods = EJBUtil.defaultLookup(HttpMethods.class);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		OnlineAccountType onlineAccountType;		
		try {
			onlineAccountType = externalAccounts.lookupOnlineAccountTypeForName(accountType);
		} catch (NotFoundException e) {
			throw IQException.createBadRequest("Unknown account type " + accountType);
		}
		
		if (onlineAccountType.getAccountType() != null) {		
			try {
				if (onlineAccountType.getAccountType().equals(ExternalAccountType.FLICKR)) {
		    		XmlBuilder xmlForFlickr = new XmlBuilder();
		    		xmlForFlickr.openElement("result");
		    		httpMethods.doFindFlickrAccount(xmlForFlickr, viewpoint, username);
		    		xmlForFlickr.closeElement(); // result
		    		Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlForFlickr.getBytes()));
		    		XPath xpath = XPathFactory.newInstance().newXPath();
		    		String nsid = ((Node)xpath.evaluate("/result/flickrUser/nsid", doc, XPathConstants.NODE)).getTextContent();
		    		httpMethods.doSetFlickrAccount(new XmlBuilder(), viewpoint, "", nsid, username);
		    		// return "";
		    	} else {
		    		Method setAccount = httpMethods.getClass().getMethod("doSet" + onlineAccountType.getAccountType().getDomNodeIdName() + "Account",
		    				                                             new Class[] {XmlBuilder.class, UserViewpoint.class, String.class, String.class});	
		    		XmlBuilder resultXml = new XmlBuilder();
		    		resultXml.openElement("result");
		    		setAccount.invoke(httpMethods, new Object[] {resultXml, viewpoint, "", username});
		    		resultXml.closeElement(); // result
		    		// we have messages telling the user about certain limitations of their account
		    		// for MySpace, Twitter, Reddit, and Amazon
		    		if (resultXml.getBytes().length > 0) {
			    		try {
			    		    Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(resultXml.getBytes()));
			    		    XPath xpath = XPathFactory.newInstance().newXPath();
			    		    Node node = (Node)xpath.evaluate("/result/message", doc, XPathConstants.NODE);
			    		    if (node != null) {
			    		        String message = node.getTextContent();
			    		        if (message.trim().length() > 0) {
			    		    	    // return message;
			    		        }
			    		    }
			    		} catch (XPathExpressionException e) {
			    			logger.error("Error getting a message about an external account for " + onlineAccountType.getAccountType() + " with value " + username, e);
				        	// the account must have been set succesfully anyway
			    			// return "";
				        }
		    	    }
		    		// return "";	    		
		    	}
    	    } catch (XmlMethodException e) {
    		    // in all cases except for the Flickr one, if the XmlMethodException will be thrown, it will be
    		    // wrapped inside an InvokationTargetException below because of our use of reflection
    		    logger.warn("Error updating external account for " + onlineAccountType.getAccountType() + " with value " + username, e);
    		    throw IQException.createBadRequest(e.getMessage());	   
    	    } catch (IOException e) {
    		    logger.error("Error updating external account for " + onlineAccountType.getAccountType() + " with value " + username, e);
    		    throw IQException.createBadRequest("Could not save an account on the server.");    
    	    } catch (ParserConfigurationException e) {
    	    	logger.error("Error updating external account for " + onlineAccountType.getAccountType() + " with value " + username, e);
    		    throw IQException.createBadRequest("Could not save an account on the server.");
            } catch (SAXException e) {
    		    logger.error("Error updating external account for " + onlineAccountType.getAccountType() + " with value " + username, e);
    		    throw IQException.createBadRequest("Could not save an account on the server.");
            } catch (XPathExpressionException e) {
    		    logger.error("Error updating external account for " + onlineAccountType.getAccountType() + " with value " + username, e);
    		    throw IQException.createBadRequest("Could not save an account on the server.");
            } catch (NoSuchMethodException e) {
    		    logger.error("Error updating external account for " + onlineAccountType.getAccountType() + " with value " + username, e);
    		    throw IQException.createBadRequest("Could not save an account on the server.");
            } catch (InvocationTargetException e) {
    		    logger.warn("Error updating external account for " + onlineAccountType.getAccountType() + " with value " + username, e);
    		    if (e.getCause() != null) {
    			    // this is the error we'll get if the submitted username is invalid
    		    	throw IQException.createBadRequest(e.getCause().getMessage());
    		    } else  {
    		    	throw IQException.createBadRequest("Could not save an account on the server.");
    		    }
            } catch (IllegalAccessException e) {
		        logger.error("Error updating external account for " + onlineAccountType.getAccountType() + " with value " + username, e);
		        throw IQException.createBadRequest("Could not save an account on the server.");
            }
  	    } else {
  	    	try {
	    	    httpMethods.doSetOnlineAccountValue(new XmlBuilder(), viewpoint, accountType, "", username);
	    	    // return "";
  	    	 } catch (XmlMethodException e) {
     		    logger.warn("Error updating external account for " + accountType + " with value " + username, e);
     		    throw IQException.createBadRequest(e.getMessage());	    		
     	    }    
	    }
	}
}
