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
import com.dumbhippo.Pair;
import com.dumbhippo.Site;
import com.dumbhippo.StringUtils;
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
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;
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
	
	// we get a GET request when a user logs in to Facebook and we get a callback with the authentication token
	@Override
	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws IOException, HumanVisibleException, HttpException, ServletException, RetryException {
		String facebookAuthToken = request.getParameter("auth_token");
		if (facebookAuthToken != null)
			facebookAuthToken = facebookAuthToken.trim();
		
		if (facebookAuthToken == null || facebookAuthToken.equals("")) 
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Facebook auth_token not provided");

		String next = request.getParameter("next");
		String redirectUrl = "/";
		if (next != null && !next.equals("home"))
			redirectUrl = redirectUrl + next;
		
		redirectUrl = redirectUrl + "?auth_token=" + facebookAuthToken;
		FacebookTracker facebookTracker = WebEJBUtil.defaultLookup(FacebookTracker.class);
		SigninBean signin = SigninBean.getForRequest(request);
		
		if (!(signin instanceof UserSigninBean))
			throw new RuntimeException("this operation requires checking signin.valid first to be sure a user is signed in");
		
    	try {
    	    // request a session key for the signed in user and set it in the database 
    	    facebookTracker.updateOrCreateFacebookAccount(((UserSigninBean)signin).getViewpoint(), facebookAuthToken);
    	} catch (FacebookSystemException e) {
            redirectUrl = redirectUrl + "&error_message=" + e.getMessage();		
    	}
		response.sendRedirect(redirectUrl);
		return null;
	}
	
	// we get a POST request when our application page is loaded on Facebook
	// input values from the form on that page are passed in along with the usual Facebook parameters when the from has
	// been submitted (the action for the form is to reload the application page on Facebook, but the submitted values
	// are passed along with the request for page content from us)
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
        String apiKey = null;
        String secret = null;
        try {
        	apiKey = config.getPropertyNoDefault(HippoProperty.FACEBOOK_API_KEY).trim();
			if (apiKey.length() == 0)
				apiKey = null;	
        	secret = config.getPropertyNoDefault(HippoProperty.FACEBOOK_SECRET).trim();      	
			if (secret.length() == 0)
				secret = null;			
		} catch (PropertyNotFoundException e) {
			secret = null;
		}
		
		String errorMessage = null;
		User user = null;
		UserViewpoint userViewpoint = null;
		if (apiKey == null) {
			errorMessage = "We could not verify Facebook information due to a missing api key we should have for our Facebook application.";   
			logger.warn("Facebook api key is not set, can't make requests to Facebook.");
		} else if (secret == null) {
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
	            FacebookTracker facebookTracker = WebEJBUtil.defaultLookup(FacebookTracker.class);

	            try {
	        	    user = identitySpider.lookupUserByFacebookUserId(SystemViewpoint.getInstance(), facebookUserId);
    	            try {
    	                if (user != null) {
		    	            userViewpoint = new UserViewpoint(user, Site.MUGSHOT);
		    	        	// TODO: can change this into updateExistingFacebookAccount
		    	            facebookTracker.updateOrCreateFacebookAccount(userViewpoint, sessionKey, facebookUserId, true);		    	            
			            }
    	            } catch (FacebookSystemException e) {
                        errorMessage = e.getMessage();		
    	            }
		        } catch (NotFoundException e) {
		        	// this means we did not have a resource for this Facebook user id in the system
		        	// nothing to do here, but we will try to create a user with this resource below     	
		        }
		        
		        if (user == null) {
		        	try {
		                // need to create a new user based on the Facebook user id
        	            user = facebookTracker.createNewUserWithFacebookAccount(sessionKey, facebookUserId, true);
		        	} catch (FacebookSystemException e) {
                        errorMessage = e.getMessage();		
    	            }
		        }
	        }
		}
		
		// this returns some code in FBML we'll return for our app page on Facebook
		// it intentionally points to my test server for now
		XmlBuilder xml = new XmlBuilder();	
		String baseUrl = config.getBaseUrlMugshot().toExternalForm();

        xml.appendTextNode("fb:header", "Mugshot");
        xml.appendTextNode("div", "Mugshot allows you and your friends to see your activity from lots of other sites on the internet in a single place in your profile.",
                           "style", "margin-left:22px;margin-bottom:10px;font-weight:bold;");
		if (user != null && errorMessage == null) {
			// check if there are mugshot params, process them, and display an appropriate message
	        @SuppressWarnings("unchecked")
	        Pair<Map<ExternalAccountType, CharSequence>, CharSequence> mugshotParamsPair = extractMugshotParamsFromArray(request.getParameterMap());
	        Map<ExternalAccountType, CharSequence> mugshotParams = mugshotParamsPair.getFirst();
	        String tabValue = mugshotParamsPair.getSecond().toString();
	        Boolean inviteSelected = tabValue.equalsIgnoreCase("invite");
	        xml.openElement("fb:tabs");
	        xml.appendEmptyNode("fb:tab-item", "href", "http://apps.facebook.com/mugshot?mugshot_tab=home", "title", "Edit Accounts", "selected", Boolean.toString(!inviteSelected.booleanValue()));
	        xml.appendEmptyNode("fb:tab-item", "href", "http://apps.facebook.com/mugshot?mugshot_tab=invite", "title", "Invite Friends", "selected", inviteSelected.toString());
            xml.closeElement();
	        if (inviteSelected.booleanValue()) {
	            FacebookTracker facebookTracker = WebEJBUtil.defaultLookup(FacebookTracker.class);
	        	xml.openElement("fb:request-form", "type", "Mugshot", "content",
	        			        "Mugshot allows you to display updates about your Netflix movies, Amazon reviews and wish list items, Digg, Delicious, and Google Reader shared items, Flickr, Picasa, and YouTube uploads, Twitter and blog entries, music and other services in a single place in your profile." +
	        			        "<fb:req-choice url='http://www.facebook.com/add.php?api_key=" + apiKey + "' label='Add Application!'/>",
	        			        "invite", "true", "method", "POST");
	        	xml.appendEmptyNode("fb:multi-friend-selector", "actiontext", "Whom would you like to invite to use Mugshot?", "showborder", "false", "rows", "4", "bypass", "Cancel",
	        			             "exclude_ids", StringUtils.join(facebookTracker.getFriendAppUsers(user), ","));
	        	xml.closeElement();		        
	        } else {
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
					    		xmlForFlickr.openElement("result");
					    		httpMethods.doFindFlickrAccount(xmlForFlickr, userViewpoint, entryValue);
					    		xmlForFlickr.closeElement(); // result
					    		Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlForFlickr.getBytes()));
					    		XPath xpath = XPathFactory.newInstance().newXPath();
					    		String nsid = ((Node)xpath.evaluate("/result/flickrUser/nsid", doc, XPathConstants.NODE)).getTextContent();
					    		logger.debug("Got nsid {} when setting Flickr account", nsid);
					    		httpMethods.doSetFlickrAccount(new XmlBuilder(), userViewpoint, nsid, entryValue);
					    		accountsSetSuccessful.add(ExternalAccountType.FLICKR);
					    	} else {
					    		Method setAccount = httpMethods.getClass().getMethod("doSet" + entry.getKey().getDomNodeIdName() + "Account",
					    				                                             new Class[] {XmlBuilder.class, UserViewpoint.class, String.class});	
					    		XmlBuilder resultXml = new XmlBuilder();
					    		resultXml.openElement("result");
					    		setAccount.invoke(httpMethods, new Object[] {resultXml, userViewpoint, entryValue});
					    		resultXml.closeElement(); // result
					    		// we have messages telling the user about certain limitations of their account
					    		// for MySpace, Twitter, Reddit, and Amazon
					    		accountsSetSuccessful.add(entry.getKey());
					    		if (resultXml.getBytes().length > 0) {
						    		try {
						    		    Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(resultXml.getBytes()));
						    		    XPath xpath = XPathFactory.newInstance().newXPath();
						    		    Node node = (Node)xpath.evaluate("/result/message", doc, XPathConstants.NODE);
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
					xml.openElement("fb:explanation");
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
	
				String floatStyle = "";
				String labelWidth = "180";
				String leftSideWidth = "width:490px;";
				String categoryNameLeftMargin = "margin-left:0px;";
				if (user.getAccount().isPublicPage()) {
				    xml.appendTextNode("span", "Updates to the information below will be reflected in ",
					    	           "style", "margin-left:22px;");
			        xml.appendTextNode("a", "your Mugshot account", "href",
					                   baseUrl + "/person?who=" + user.getId(), "target", "_blank");
			        xml.append(".");
			    } else {
				    xml.appendTextNode("span", "Fill in the information for accounts you want to display updates from.",
			    	                   "style", "margin-left:22px;");		
				    floatStyle="float:left;";
				    labelWidth="120";
				    leftSideWidth = "width:430px;";
				    categoryNameLeftMargin = "margin-left:0px;";
			    }
			    ExternalAccountCategory currentCategory = null;
			    boolean hadInitialInfo = false;
			    xml.openElement("div", "style", "position:relative;" + leftSideWidth + floatStyle);
			    xml.openElement("fb:editor", "action", "", "width", "310", "labelwidth", labelWidth);
			    for (ExternalAccountView externalAccount : getSupportedAccounts(user)) {
			    	if (currentCategory == null || !currentCategory.equals(externalAccount.getExternalAccountType().getCategory())) {
					    currentCategory = externalAccount.getExternalAccountType().getCategory();
			    		xml.openElement("fb:editor-custom");
					    xml.appendTextNode("h3", currentCategory.getCategoryName(), "style", categoryNameLeftMargin);		    	
					    xml.closeElement();
			    	}
				    xml.openElement("fb:editor-custom", "label", externalAccount.getSiteName());
				    
				    if (externalAccount.getExternalAccount() != null && externalAccount.getExternalAccount().isLovedAndEnabled()) {
				        xml.appendEmptyNode("input", "name", "mugshot_" + externalAccount.getExternalAccountType().name(), "value", externalAccount.getExternalAccount().getAccountInfo());
				        hadInitialInfo = true;
				    } else {
				    	xml.appendEmptyNode("input", "name", "mugshot_" + externalAccount.getExternalAccountType().name());
				    }
				    
				    xml.openElement("div", "style", "color:#666666;");			    
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
				    xml.closeElement(); // div
				    
				    xml.closeElement(); // fb:editor-custom	
			    }
			    xml.openElement("fb:editor-buttonset");
			    if (hadInitialInfo)
			        xml.appendEmptyNode("fb:editor-button", "value", "Update Info!");
			    else
			    	xml.appendEmptyNode("fb:editor-button", "value", "Submit Info!");
			    xml.appendEmptyNode("fb:editor-cancel");
			    xml.closeElement(); // fb:editor-buttonset
			    xml.closeElement(); // fb:editor 		    
			    xml.closeElement(); // div with the form
			    
			    if (!user.getAccount().isPublicPage()) {
			    	xml.openElement("div", "style", "width:184px;float:left;color:#333333;background-color:#EDF2F3;border-style:solid;border-width:1px;border-color:#C2D1D4;margin-top:34px;padding:8px;");
			    	xml.openElement("span", "style", "font-weight:bold;");
			    	xml.append("Do you already have a Mugshot account?");
			    	xml.closeElement();
				    xml.append(" Don't fill in this stuff, just verify" +
				    		   " your Mugshot account by following this link.");
				    xml.openElement("form", "action", baseUrl + "/facebook-add", "target", "_blank", "method", "GET");
				    // there didn't seem to be a way to get buttons in fb:editor to open in a new window, which is what we want here, so we are using 
				    // our own form and buttons 
				    // original top and left border color on facebook is #D8DFEA, but it looks too light to me
				    String buttonStyle = "background-color:#3B5998;color:#ffffff;border-width:1px;padding-top:2px;padding-bottom:2px;padding-right:6px;padding-left:6px;margin-top:8px;margin-bottom:8px;border-top-color:#728199;border-left-color:#728199;border-right-color:#0E1F5B;border-bottom-color:#0E1F5B;";
				    xml.appendEmptyNode("input", "type", "submit", "value", "Verify My Mugshot Account", "style", buttonStyle);
				    xml.closeElement();		
				    // divider line
			    	xml.openElement("div", "style", "height:1px;background-color:#C2D1D4;margin-top:8px;margin-bottom:8px;");
				    xml.closeElement();		
			    	xml.openElement("span", "style", "font-weight:bold;");
			    	xml.append("Want to create a Mugshot account?");
			    	xml.closeElement();
				    xml.append(" It's free and easy and helps you see all your friends' activities in one place, share links, and read feeds in a social setting.");
		            xml.openElement("form", "action", baseUrl + "/facebook-signin", "target", "_blank", "method", "GET");
		            xml.appendEmptyNode("input", "type", "submit", "value", "Create My Mugshot Account", "style", buttonStyle);
		            xml.closeElement();	
			    	xml.closeElement();
			    }
	        }
		} else {
			if (errorMessage == null)
				errorMessage = "We could not get an existing or create a new user.";
			logger.error("Displaying a really bad error message on Facebook: {}", errorMessage);
			xml.openElement("fb:error");
			xml.appendTextNode("fb:message", "Getting Mugshot Information Failed");			
			xml.append(errorMessage);
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
	
	private static Pair<Map<ExternalAccountType, CharSequence>, CharSequence> extractMugshotParamsFromArray(Map<CharSequence, CharSequence[]> reqParams) {
	    if (null == reqParams)
	        return null;
	    String mugshotParamPart = "mugshot_";
	    Map<ExternalAccountType, CharSequence> result = new HashMap<ExternalAccountType, CharSequence>(reqParams.size());
	    CharSequence tabValue = "home";
	    for (Map.Entry<CharSequence, CharSequence[]> entry : reqParams.entrySet()) {
	        String key = entry.getKey().toString();
	        if (key.startsWith(mugshotParamPart)) {
	        	String param = key.substring(mugshotParamPart.length());
	          	if (param.equals("tab"))
	          		tabValue = entry.getValue()[0];
	            // we want to preserve the parameter even if entry.getValue()[0] is empty, because that might mean we want to
	            // unset the external account information
	            result.put(ExternalAccountType.valueOf(param), entry.getValue()[0]);
	        }  
	    }
	    return new Pair<Map<ExternalAccountType, CharSequence>, CharSequence>(result, tabValue);
	}    
}
