package com.dumbhippo.server.impl;

import java.util.Formatter;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.LoginToken;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.LoginVerifier;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.SigninSystem;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;

@Stateless
public class SigninSystemBean implements SigninSystem {

	static private final Logger logger = GlobalSetup.getLogger(SigninSystemBean.class);
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PersonViewer personViewer;

	@EJB
	private AccountSystem accountSystem;
	
	@EJB
	private LoginVerifier loginVerifier;
	
	@EJB
	private Configuration configuration;
	
	@EJB
	private Mailer mailer;
	
	private String getLoginLink(Viewpoint viewpoint, Resource resource) throws HumanVisibleException, RetryException {
		LoginToken token = loginVerifier.getLoginToken(resource);
		return token.getAuthURL(configuration.getBaseUrl(viewpoint));
	}
	
	public void sendSigninLinkEmail(Viewpoint viewpoint, String address) throws HumanVisibleException, RetryException {
		EmailResource resource;
		try {
			resource = identitySpider.lookupEmail(address);
		} catch (NotFoundException e) {
			throw new HumanVisibleException("This isn't an email address we know about.");
		}
		String link = getLoginLink(viewpoint, resource);
		MimeMessage message = mailer.createMessage(viewpoint, Mailer.SpecialSender.LOGIN, resource.getEmail());
		
		StringBuilder bodyText = new StringBuilder();
		XmlBuilder bodyHtml = new XmlBuilder();
		
		bodyText.append("\n");
		bodyText.append("Go to: " + link + "\n");
		bodyText.append("\n");
		
		bodyHtml.appendHtmlHead("");
		bodyHtml.append("<body>\n");
		bodyHtml.appendTextNode("a", "Click here to sign in", "href", link);
		bodyHtml.append("</body>\n</html>\n");
		
		mailer.setMessageContent(message, viewpoint.getSite(), "Sign in to " + viewpoint.getSite().getSiteName(), bodyText.toString(), bodyHtml.toString(), false);
		mailer.sendMessage(message);
	}
	
	public String getSigninLinkAim(Viewpoint viewpoint, String address) throws HumanVisibleException, RetryException {
		AimResource resource;
		try {
			resource = identitySpider.lookupAim(address);
		} catch (NotFoundException e) {
			throw new HumanVisibleException("This isn't an AIM screen name we know about.");
		}
		
		String link = getLoginLink(viewpoint, resource);
		XmlBuilder bodyHtml = new XmlBuilder();
		bodyHtml.appendTextNode("a", "Click to sign in", "href", link);
		
		return bodyHtml.toString();
	}
		
	final static String REPAIR_TEXT = 
		"There are some bumps along the road for most development projects.\n" + 
		"We just hit a good sized pothole. Due to some recent bugs on our.\n" + 
		"part, some of our users may have gotten logged out of the system\n" +
		"and may not be running our software any more. We're not positive\n" +
		"what users were affected, but are sending you this mail since you\n" +
		"haven't signed on to the system since last Thursday when we had\n" +
		"the problems.\n" +
		"\n" +
		"The following link will:\n" +
		" - sign you back into the system\n" +
		" - let you download the latest version of the our software with\n" +
		"   important bug fixes\n" +
		"\n" +
		"%s\n" +
		"\n" +
		"Caution: this is a one-time link and needs to be opened in Internet\n" +
		"  Explorer on Windows to work. If you normally use a different web\n" +
		"  browser, cut-and-paste the link into Internet Explorer. If you need\n" +
		"  help, please mail feedback@mugshot.org or reply to this mail\n" +
		"\n" +
		"We apologize for the inconvenience, and hope you'll keep trying out\n" +
		"our stuff.\n" +
		"\n" +
		"Owen Taylor\n" +
		"Mugshot Team Member\n" +
		"";
	
	final static String REPAIR_HTML =
		"<p>" +
		"There are some bumps along the road for most development projects.\n" + 
		"We just hit a good sized pothole. Due to some recent bugs on our.\n" + 
		"part, some of our users may have gotten logged out of the system\n" +
		"and may not be running our software any more. We're not positive\n" +
		"what users were affected, but are sending you this mail since you\n" +
		"haven't signed on to the system since last Thursday when we had\n" +
		"the problems.\n" +
		"</p><p>\n" +
		"The following link will:\n" +
		"</p><p>\n" +
		"<ul>" +
		"<li>sign you back into the system</li>\n" +
		"<li>let you download the latest version of the our software with important bug fixes</li>\n" +
		"</ul>\n" +
		"</p><p>\n" +
		"<a href='%s'>Click here to sign in</a>\n" +
		"</p></>\n" +
		"Caution: this is a one-time link and needs to be opened in Internet\n" +
		"   Explorer on Windows to work. If you normally use a different web\n" +
		"   browser, cut-and-paste the link into Internet Explorer. If you need\n" +
		"  help, please mail feedback@mugshot.org or reply to this mail\n" +
		"</p><p>\n" +
		"We apologize for the inconvenience, and hope you'll keep trying out\n" +
		"our stuff.\n" +
		"</p><p>\n" +
		"Owen Taylor<br/>\n" +
		"Mugshot Team Member\n" +
		"</p>\n";
		

	public void sendRepairLink(Viewpoint viewpoint, User user) throws HumanVisibleException, RetryException {		
		PersonView personView = personViewer.getPersonView(viewpoint, user);
		Resource resource = personView.getPrimaryResource();
		if (resource == null || !(resource instanceof EmailResource))
			return;
		
		// RETURN UNCONDITIONALLY FOR NOW (we don't want to send out repairs accidentally)
		if (resource != null)
			return;
		
		EmailResource emailResource = (EmailResource)resource;
		
		String baseLink = getLoginLink(viewpoint, emailResource);
		String link = baseLink + "&next=repair";
		MimeMessage message = mailer.createMessage(viewpoint, Mailer.SpecialSender.LOGIN, emailResource.getEmail());
		
		StringBuilder bodyText = new StringBuilder();
		XmlBuilder bodyHtml = new XmlBuilder();
		
		Formatter textFormatter = new Formatter(bodyText);
		textFormatter.format(REPAIR_TEXT, link);
		
		bodyHtml.appendHtmlHead("");
		
		Formatter htmlFormatter = new Formatter(bodyHtml);
		htmlFormatter.format(REPAIR_HTML, link);
		
		bodyHtml.append("</body>\n</html>\n");
		
		mailer.setMessageContent(message, viewpoint.getSite(), 
				"We messed up with " + viewpoint.getSite().getSiteName(), bodyText.toString(), bodyHtml.toString(), false);
		mailer.sendMessage(message);
	}

	public Client authenticatePassword(Site site, Guid userGuid, String password, String clientIdentifier) throws HumanVisibleException {
		User user = identitySpider.lookupUser(userGuid);
		if (user == null)
			throw new HumanVisibleException("No such user " + userGuid);
		
		return authenticatePassword(user, password, clientIdentifier);
	}
		
	public Client authenticatePassword(Site site, String address, String password, String clientIdentifier) throws HumanVisibleException {
		Resource resource;
		
		boolean noAuthentication = configuration.getProperty(HippoProperty.DISABLE_AUTHENTICATION).equals("true"); 
		if (noAuthentication) {
			logger.warn("Not requiring authentication for address {}", address);
		}

		if (noAuthentication) {
			// be sure the email/aim exists, if not authenticating, this is 
			// how you can create an account implicitly. If we are authenticating
			// we don't want people to be able to put a bunch of bogus email and aim 
			// addresses in the db.
			// This probably doesn't quite work since the newly-created Email/AimResource
			// won't be in the current transaction, but since this is a debug feature, 
			// just try again and it will work on attempt 2.
			try {
				if (address.contains("@")) {
					identitySpider.getEmail(address);
				} else {
					identitySpider.getAim(address);
				}
			} catch (RetryException e) {
				logger.warn("Failed to implicitly create email/aim resource", e);
				// will fail down below when lookupEmail/lookupAim fails
			} catch (ValidationException e) {
				throw new HumanVisibleException("'" + address + "' is not a valid email or AIM address");
			}
		}
		
		try {
			if (address.contains("@")) {
				resource = identitySpider.lookupEmail(address);
			} else {
				resource = identitySpider.lookupAim(address);
			}
		} catch (NotFoundException e) {
			throw new HumanVisibleException("You entered '" + address + "', we don't know about that email address or AIM screen name");
		}
		
		User user = identitySpider.lookupUserByResource(SystemViewpoint.getInstance(), resource);
		if (user == null && noAuthentication) {
			logger.warn("Creating new account for resource: {}", resource);
			Account account = accountSystem.createAccountFromResource(resource, site.getAccountType());
			user = account.getOwner();
		}
		
		if (user == null) {
			throw new HumanVisibleException("Hmm, there doesn't seem to be an account with the "
					+ ((resource instanceof EmailResource) ? " email address '" : " screen name '") + address + "'");
		} else {
			return authenticatePassword(user, password, clientIdentifier);
		}
	}
	
	private Client authenticatePassword(User user, String password, String clientIdentifier) throws HumanVisibleException {
		boolean noAuthentication = configuration.getProperty(HippoProperty.DISABLE_AUTHENTICATION).equals("true");
		
		Account account = user.getAccount();
		
		if (account.checkPassword(password) || noAuthentication) {
			return accountSystem.authorizeNewClient(account, clientIdentifier);
		} else {
			throw new HumanVisibleException("You seem to have mistyped your password; or maybe you haven't "
					+ "set a password yet? Try sending yourself a sign-in link if you can't remember your password.");
		}
	}

	public void setPassword(User user, String password) throws HumanVisibleException {
		// password may be null
		
		if (password != null && password.length() < 4) {
			// last-ditch check for reasonable password, a full check would go 
			// elsewhere
			throw new HumanVisibleException("That password is too short, anyone could guess it");
		}
		
		// be sure we get a new account so it's attached
		Account account = accountSystem.lookupAccountByUser(user);
		account.setPasswordPlainText(password);
	}
}
