package com.dumbhippo.web.servlets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

import com.dumbhippo.ExternalAccountCategory;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookSystemException;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.web.WebEJBUtil;
import com.facebook.api.FacebookParam;
import com.facebook.api.FacebookSignatureUtil;

public class FacebookServlet extends AbstractServlet {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FacebookServlet.class);
	
	static final long serialVersionUID = 1;
	
	private Configuration config;
	
	@Override
	public void init() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
	}	
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws IOException, HumanVisibleException, HttpException, ServletException, RetryException {
		logger.debug("full request is: {}", request.toString());
		logger.debug("context params are:");
        for (Object o : request.getParameterMap().entrySet()) {
        	@SuppressWarnings("unchecked")
            Map.Entry<String, String[]> mapEntry = (Map.Entry<String, String[]>)o;
            logger.debug("{} = {}", mapEntry.getKey(), mapEntry.getValue()[0]);
        }
        logRequest(request, "POST");
      
        @SuppressWarnings("unchecked")
        Map<String, CharSequence> facebookParams = FacebookSignatureUtil.extractFacebookParamsFromArray(request.getParameterMap());
        String secret = null;
        try {
        	secret = config.getPropertyNoDefault(HippoProperty.FACEBOOK_SECRET).trim();
			if (secret.length() == 0)
				secret = null;				
		} catch (PropertyNotFoundException e) {
			secret = null;
		}
		
		String errorMessage = null;
		User user = null;
		UserViewpoint userViewpoint = null;
		if (secret == null) {
			errorMessage = "We could not verify Facebook information due to a missing secret key we should share with Facebook.";   
			logger.warn("Facebook secret is not set, can't verify requests from Facebook.");
		} else {        
	        boolean signatureValid = FacebookSignatureUtil.verifySignature(facebookParams, secret);
	        if (!signatureValid) {
				errorMessage = "We could not verify Facebook information because the signature supplied for Facebook parameters was not valid.";           	
	        } else if (facebookParams.get(FacebookParam.ADDED.toString()).toString().equals("1")) {
	        	// get the user who owns the related FacebookResource
	            String sessionKey = facebookParams.get(FacebookParam.SESSION_KEY.toString()).toString();
	            String facebookUserId = facebookParams.get(FacebookParam.USER.toString()).toString(); 
	        	IdentitySpider identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
	        	try {
	        	    user = identitySpider.lookupUserByFacebookUserId(SystemViewpoint.getInstance(), facebookUserId);
    	            FacebookTracker facebookTracker = WebEJBUtil.defaultLookup(FacebookTracker.class);
    	            try {
    	                if (user != null) {
		    	            userViewpoint = new UserViewpoint(user, Site.MUGSHOT);
		    	        	// TODO: can change this into updateExistingFacebookAccount
		    	            facebookTracker.updateOrCreateFacebookAccount(userViewpoint, sessionKey, facebookUserId, true);		    	            
			            } else {
		    			    // need to create a new user based on the Facebook user id
			        	    user = facebookTracker.createNewUserWithFacebookAccount(sessionKey, facebookUserId, true);
			            }
    	            } catch (FacebookSystemException e) {
                        errorMessage = e.getMessage();		
    	            }
		        } catch (NotFoundException e) {
		        	// nothing to do
		        	// TODO: check in which case NotFoundException is thrown as opposed to the user being null
		        }
	        }
		}
		
		// this returns some code in FBML we'll return for our app page on Facebook
		// it intentionally points to my test server for now
		XmlBuilder xml = new XmlBuilder();		

        xml.appendTextNode("fb:header", "Musgshot");
        xml.appendTextNode("div", "Mugshot allows you and your friends to see your activity from lots of other sites on the internet and automatically puts that in your profile and news feed.",
                           "style", "margin-left:45px; margin-bottom:10px;");
		if (user != null && errorMessage == null) {
			// check if there are mugshot params, process them, and display an appropriate message
	        @SuppressWarnings("unchecked")
	        Map<ExternalAccountType, CharSequence> mugshotParams = extractMugshotParamsFromArray(request.getParameterMap());
	        ExternalAccountSystem externalAccounts = WebEJBUtil.defaultLookup(ExternalAccountSystem.class);
	        HttpMethods httpMethods =  WebEJBUtil.defaultLookup(HttpMethods.class);
    		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    		factory.setNamespaceAware(true);
    		List<ExternalAccountType> accountsSetSuccessful = new ArrayList<ExternalAccountType>();
    		List<ExternalAccountType> accountsRemoved = new ArrayList<ExternalAccountType>();
    		Map<ExternalAccountType, String> accountsWithNotes = new HashMap<ExternalAccountType, String>();
    		Map<ExternalAccountType, String> accountsSetFailed = new HashMap<ExternalAccountType, String>();    		
	        for (Map.Entry<ExternalAccountType, CharSequence> entry : mugshotParams.entrySet()) {          
	        	String entryValue = entry.getValue().toString().trim(); 
			    if (entryValue.length() > 0) {
			    	logger.debug("processing entry {} for {}", entryValue, entry.getKey());
			    	try {
			    		// we could check if the account already exists that has the same info set and is loved,
			    		// but since we might be updating certain things when the user resets the info (like looking up
			    		// all Amazon wish lists again), let's just reset all the accounts		
			    		// if it seems too slow for a user who has a lot of accounts and is just changing one, 
			    		// we can change this
				    	if (entry.getKey().equals(ExternalAccountType.FLICKR)) {
				    		XmlBuilder xmlForFlickr = new XmlBuilder();
				    		httpMethods.doFindFlickrAccount(xmlForFlickr, userViewpoint, entryValue);
				    		Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlForFlickr.getBytes()));
				    		XPath xpath = XPathFactory.newInstance().newXPath();
				    		String nsid = ((Node)xpath.evaluate("/flickrUser/nsid", doc, XPathConstants.NODE)).getTextContent();
				    		logger.debug("Got nsid {} when setting Flickr account", nsid);
				    		httpMethods.doSetFlickrAccount(new XmlBuilder(), userViewpoint, nsid, entryValue);
				    		accountsSetSuccessful.add(ExternalAccountType.FLICKR);
				    	} else {
				    		Method setAccount = httpMethods.getClass().getMethod("doSet" + entry.getKey().getDomNodeIdName() + "Account",
				    				                                             new Class[] {XmlBuilder.class, UserViewpoint.class, String.class});	
				    		XmlBuilder resultXml = new XmlBuilder();
				    		setAccount.invoke(httpMethods, new Object[] {resultXml, userViewpoint, entryValue});
				    		// we have messages telling the user about certain limitations of their account
				    		// for MySpace, Twitter, Reddit, and Amazon
				    		accountsSetSuccessful.add(entry.getKey());
				    		try {
				    		    Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(resultXml.getBytes()));
				    		    XPath xpath = XPathFactory.newInstance().newXPath();
				    		    Node node = (Node)xpath.evaluate("/message", doc, XPathConstants.NODE);
				    		    if (node != null) {
				    		        String message = node.getTextContent();
				    		        if (message.trim().length() > 0) {
				    		    	    accountsWithNotes.put(entry.getKey(), message);
				    		        }
				    		    }
				    		} catch (XPathExpressionException e) {
				    			logger.error("Error getting a message about an external account for " + entry.getKey() + " with value " + entryValue, e);
					        	// let's not bother the user with this, since the account must have been set succesfully
					        }
				    	}
			    	} catch (XmlMethodException e) {
			    		// in all cases except for the Flickr one, if the XmlMethodException will be thrown, it will be
			    		// wrapped inside an InvokationTargetException below because of our use of reflection
			    		logger.warn("Error updating external account for " + entry.getKey() + " with value " + entryValue, e);
			    		accountsSetFailed.put(entry.getKey(), e.getMessage());		    		
			    	} catch (ParserConfigurationException e) {
			    		logger.error("Error updating external account for " + entry.getKey() + " with value " + entryValue, e);
			    		accountsSetFailed.put(entry.getKey(), e.getMessage());
			        } catch (SAXException e) {
			    		logger.error("Error updating external account for " + entry.getKey() + " with value " + entryValue, e);
			    		accountsSetFailed.put(entry.getKey(), e.getMessage());
			        } catch (XPathExpressionException e) {
			    		logger.error("Error updating external account for " + entry.getKey() + " with value " + entryValue, e);
			    		accountsSetFailed.put(entry.getKey(), e.getMessage());
			        } catch (NoSuchMethodException e) {
			    		logger.error("Error updating external account for " + entry.getKey() + " with value " + entryValue, e);
			    		accountsSetFailed.put(entry.getKey(), e.getMessage());
			        } catch (InvocationTargetException e) {
			    		logger.warn("Error updating external account for " + entry.getKey() + " with value " + entryValue, e);
			    		if (e.getCause() != null)
			    		    accountsSetFailed.put(entry.getKey(), e.getCause().getMessage());
			    		else 
			    			accountsSetFailed.put(entry.getKey(), e.getMessage());
			        } catch (IllegalAccessException e) {
		    		    logger.error("Error updating external account for " + entry.getKey() + " with value " + entryValue, e);
			    		accountsSetFailed.put(entry.getKey(), e.getMessage());
			        }
			    } else {
			    	try {
			    	    // do not unset "hate" because we don't have that concept on Facebook application page
			    		// interface, so "hated" accounts are not populated with values to begin with; only unset "love"
			    	    ExternalAccount externalAccount = externalAccounts.lookupExternalAccount(userViewpoint, user, entry.getKey());
			    	    if (externalAccount.getSentiment().equals(Sentiment.LOVE)) {
			    	    	externalAccounts.setSentiment(externalAccount, Sentiment.INDIFFERENT);
			    	    	accountsRemoved.add(entry.getKey());
			    	    }
			    	} catch (NotFoundException e) {
			    		// this account did not exist, nothing to do
			    	}
			    }
		    	
		    }
	        
			if (accountsSetSuccessful.size() > 0 || accountsRemoved.size() > 0) {
				StringBuilder accountsSetSuccessfulBuilder = new StringBuilder();
				StringBuilder accountsRemovedBuilder = new StringBuilder();
				String singularOrPlural = "";
				xml.openElement("fb:success");
				xml.appendTextNode("fb:message", "Success");
				xml.openElement("ul");
				for (ExternalAccountType accountType : accountsSetSuccessful) {
					accountsSetSuccessfulBuilder.append(accountType.getSiteName() + ", ");
				}				
				if (accountsSetSuccessfulBuilder.length() > 2) {
					if (accountsSetSuccessful.size() > 1)
						singularOrPlural = " accounts were";
					else 
						singularOrPlural = " account was";	
							
		    	    xml.appendTextNode("li", accountsSetSuccessfulBuilder.substring(0, accountsSetSuccessfulBuilder.length()-2) + singularOrPlural + " set successfully.");		
				}
				
				for (ExternalAccountType accountType : accountsRemoved) {
					accountsRemovedBuilder.append(accountType.getSiteName() + ", ");
				}				
				if (accountsRemovedBuilder.length() > 2) {
					if (accountsRemoved.size() > 1)
						singularOrPlural = " accounts were";
					else 
						singularOrPlural = " account was";	
		    	    xml.appendTextNode("li", accountsRemovedBuilder.substring(0, accountsRemovedBuilder.length()-2) + singularOrPlural + " removed successfully.");		
				}				
		    	xml.closeElement();
		    	xml.closeElement();
			}
			
			if (accountsWithNotes.size() > 0) {
				xml.openElement("fb:success");
				xml.appendTextNode("fb:message", "Please Note");
				xml.openElement("ul");
				for (Map.Entry<ExternalAccountType, String> entry : accountsWithNotes.entrySet()) {
				    xml.appendTextNode("li", entry.getValue());
				}
				xml.closeElement();
				xml.closeElement();
			}

			if (accountsSetFailed.size() > 0) {
				String singularOrPlural = " Account Was";
				if (accountsSetFailed.size() > 1)
					singularOrPlural = " Accounts Were";
				
				xml.openElement("fb:error");
				xml.appendTextNode("fb:message", "The Following" + singularOrPlural + " Not Set");
				xml.openElement("ul");				
				for (Map.Entry<ExternalAccountType, String> entry : accountsSetFailed.entrySet()) {
					logger.debug("key {} value {}", entry.getKey().getSiteName(), entry.getValue());
				    xml.appendTextNode("li", entry.getKey().getSiteName() + ": " + entry.getValue());
				}
				xml.closeElement();
				xml.closeElement();
			}
			
			xml.appendTextNode("span", "Updates to the information below will be reflected in ",
					           "style", "margin-left:15px;");
		    xml.appendTextNode("a", "your Mugshot account", "href",
				               "http://dogfood.mugshot.org/person?who=" + user.getId(), "target", "_blank");
		    xml.append(".");
		    ExternalAccountCategory currentCategory = null;
		    xml.openElement("fb:editor", "action", "", "width", "300", "labelwidth", "120");
		    for (ExternalAccountView externalAccount : getSupportedAccounts(user)) {
		    	if (currentCategory == null || !currentCategory.equals(externalAccount.getExternalAccountType().getCategory())) {
				    currentCategory = externalAccount.getExternalAccountType().getCategory();
		    		xml.openElement("fb:editor-custom");
				    xml.appendTextNode("h3", currentCategory.getCategoryName(), "style", "margin-left:-230px;" );		    	
				    xml.closeElement();
		    	}
			    xml.openElement("fb:editor-custom", "label", externalAccount.getSiteName());
			    
			    if (externalAccount.getExternalAccount() != null && externalAccount.getExternalAccount().isLovedAndEnabled()) {
			        xml.appendEmptyNode("input", "name", "mugshot_" + externalAccount.getExternalAccountType().name(), "value", externalAccount.getExternalAccount().getAccountInfo());
			    } else {
			    	xml.appendEmptyNode("input", "name", "mugshot_" + externalAccount.getExternalAccountType().name());
			    }
			    xml.appendEmptyNode("br");
			    
			    if (externalAccount.isInfoTypeProvidedBySite()) {
			        xml.append("Enter your ");
			        xml.appendTextNode("a", externalAccount.getSiteName(), 
			        		           "href", externalAccount.getExternalAccountType().getSiteLink(), 
			        		           "target", "_blank");
			        xml.append(" " + externalAccount.getSiteUserInfoType());
				    if (externalAccount.getExternalAccountType().getHelpUrl().trim().length() > 0) {
				    	xml.append(" (");
				        xml.appendTextNode("a", "help me find it", 
		        		           "href", externalAccount.getExternalAccountType().getHelpUrl().trim(), 
		        		           "target", "_blank");			    	
				        xml.append(")");
				    }
				    xml.append(".");
			    } else {
			        xml.append("Enter your " + externalAccount.getSiteUserInfoType() + " ");
			        xml.appendTextNode("a", externalAccount.getSiteName(), 
			        		           "href", externalAccount.getExternalAccountType().getSiteLink(), 
			        		           "target", "_blank");
			        xml.append(" account.");			    	
			    }
			    
			    if (externalAccount.getExternalAccountType().getSupportType().trim().length() > 0) {
			    	xml.append(" Your activity will be updated when you " + externalAccount.getExternalAccountType().getSupportType() + ".");
			    } else {
			    	xml.append(" A link to this account will be included in your profile.");
			    }
			    
			    xml.closeElement(); // fb:editor-custom	
		    }
		    xml.openElement("fb:editor-buttonset");
		    xml.appendEmptyNode("fb:editor-button", "value", "Update Info!");
		    xml.appendEmptyNode("fb:editor-cancel");
		    xml.closeElement(); // fb:editor-buttonset
		    xml.closeElement(); // fb:editor 		    
		} else {
		    xml.append("You need to be ");
		    xml.appendTextNode("a", "logged in to Mugshot", "href",
				    "http://dogfood.mugshot.org/account", "target", "_blank");
	    	xml.append(" to be able to verify your Mugshot account.");
		    xml.openElement("form", "action", "http://dogfood.mugshot.org/facebook-add", "target", "_blank", "method", "GET");
		    xml.appendEmptyNode("input", "type", "submit", "value", "Verify My Mugshot Account");
		    xml.closeElement();
		}
		
		response.setContentType("text/html");
		response.getOutputStream().write(xml.getBytes());
		
		return null;
	}	
		
	/**
	 * Returns a list of supported account views.
	 * If the user is not null, the ExternalAccount information for the
	 * user will be filled in for the account types for which the user has accounts.
	 */
	private List<ExternalAccountView> getSupportedAccounts(User user) {
		List<ExternalAccountView> supportedAccounts = new ArrayList<ExternalAccountView>(); 
		ExternalAccountSystem externalAccounts = WebEJBUtil.defaultLookup(ExternalAccountSystem.class);
		for (ExternalAccountType type : ExternalAccountType.alphabetizedValuesByCategory()) {
			if ((type.isSupported() || type.equals(ExternalAccountType.BLOG)) && 
			    !type.getCategory().equals(ExternalAccountCategory.NOT_CATEGORIZED)) {
				if (user != null) {
					try {
					    ExternalAccount externalAccount = 
					    	externalAccounts.lookupExternalAccount(new UserViewpoint(user, Site.MUGSHOT), user, type);
					    supportedAccounts.add(new ExternalAccountView(externalAccount));
					} catch (NotFoundException e) {
						supportedAccounts.add(new ExternalAccountView(type));
					}
				} else {
					supportedAccounts.add(new ExternalAccountView(type));
				}
			}
		}
		return supportedAccounts;
	}
	
	@Override
	protected boolean isReadWrite(HttpServletRequest request) {
		// The method is GET, since we need links that the user can just click upon,
		// but they have side effects. This is OK since the links are unique, so 
		// caching won't happen.
		
		return true;
	}
	

	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		return true;
	}
	
	private static Map<ExternalAccountType, CharSequence> extractMugshotParamsFromArray(Map<CharSequence, CharSequence[]> reqParams) {
	    if (null == reqParams)
	        return null;
	    String mugshotParamPart = "mugshot_";
	    Map<ExternalAccountType, CharSequence> result = new HashMap<ExternalAccountType, CharSequence>(reqParams.size());
	    for (Map.Entry<CharSequence, CharSequence[]> entry : reqParams.entrySet()) {
	        String key = entry.getKey().toString();
	        if (key.startsWith(mugshotParamPart)) {
	          // we want to preserve the parameter even if entry.getValue()[0] is empty, because that might mean we want to
	          // unset the external account information
	          result.put(ExternalAccountType.valueOf(key.substring(mugshotParamPart.length())), entry.getValue()[0]);
	        }  
	    }
	    return result;
	}    
}
