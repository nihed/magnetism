package com.dumbhippo.server.impl;

import java.util.Formatter;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
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
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.SigninSystem;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.UserViewpoint;

@Stateless
public class SigninSystemBean implements SigninSystem {

	static private final Logger logger = GlobalSetup.getLogger(SigninSystemBean.class);
	
	@EJB
	private IdentitySpider identitySpider;

	@EJB
	private AccountSystem accountSystem;
	
	@EJB
	private LoginVerifier loginVerifier;
	
	@EJB
	private Configuration configuration;
	
	@EJB
	private Mailer mailer;
	
	private String getLoginLink(Resource resource) throws HumanVisibleException {
		LoginToken token = loginVerifier.getLoginToken(resource);
		return token.getAuthURL(configuration.getPropertyFatalIfUnset(HippoProperty.BASEURL));
	}
	
	public void sendSigninLinkEmail(String address) throws HumanVisibleException {
		EmailResource resource = identitySpider.lookupEmail(address);
		if (resource == null)
			throw new HumanVisibleException("That isn't an email address we know about");
		String link = getLoginLink(resource);
		MimeMessage message = mailer.createMessage(Mailer.SpecialSender.LOGIN, resource.getEmail());
		
		StringBuilder bodyText = new StringBuilder();
		XmlBuilder bodyHtml = new XmlBuilder();
		
		bodyText.append("\n");
		bodyText.append("Go to: " + link + "\n");
		bodyText.append("\n");
		
		bodyHtml.appendHtmlHead("");
		bodyHtml.append("<body>\n");
		bodyHtml.appendTextNode("a", "Click here to sign in", "href", link);
		bodyHtml.append("</body>\n</html>\n");
		
		mailer.setMessageContent(message, "Sign in to Mugshot", bodyText.toString(), bodyHtml.toString());
		mailer.sendMessage(message);
	}
	
	public String getSigninLinkAim(String address) throws HumanVisibleException {
		AimResource resource = identitySpider.lookupAim(address);
		if (resource == null)
			throw new HumanVisibleException("That isn't an AIM screen name we know about");
		
		String link = getLoginLink(resource);
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
		

	public void sendRepairLink(User user) throws HumanVisibleException {		
		PersonView personView = identitySpider.getPersonView(new UserViewpoint(user), user, PersonViewExtra.PRIMARY_EMAIL);
		Resource resource = personView.getPrimaryResource();
		if (resource == null || !(resource instanceof EmailResource))
			return;
		
		// RETURN UNCONDITIONALLY FOR NOW (we don't want to send out repairs accidentally)
		if (resource != null)
			return;
		
		EmailResource emailResource = (EmailResource)resource;
		
		String baseLink = getLoginLink(emailResource);
		String link = baseLink + "&next=repair";
		MimeMessage message = mailer.createMessage(Mailer.SpecialSender.LOGIN, emailResource.getEmail());
		
		StringBuilder bodyText = new StringBuilder();
		XmlBuilder bodyHtml = new XmlBuilder();
		
		Formatter textFormatter = new Formatter(bodyText);
		textFormatter.format(REPAIR_TEXT, link);
		
		bodyHtml.appendHtmlHead("");
		
		Formatter htmlFormatter = new Formatter(bodyHtml);
		htmlFormatter.format(REPAIR_HTML, link);
		
		bodyHtml.append("</body>\n</html>\n");
		
		mailer.setMessageContent(message, "We messed up with Mugshot", bodyText.toString(), bodyHtml.toString());
		mailer.sendMessage(message);
	}

	public Client authenticatePassword(String address, String password, String clientIdentifier) throws HumanVisibleException {
		Resource resource;
		
		boolean noAuthentication = configuration.getProperty(HippoProperty.DISABLE_AUTHENTICATION).equals("true"); 
		if (noAuthentication) {
			logger.warn("Not requiring authentication for address {}", address);
		}
		
		try {
			if (address.contains("@")) {
				resource = identitySpider.getEmail(address);
			} else {
				resource = identitySpider.getAim(address);
			}
		} catch (ValidationException e) {
			throw new HumanVisibleException("Invalid address '" + address + "': " + e.getMessage());
		}
		
		if (resource == null)
			throw new HumanVisibleException("You entered '" + address + "', we don't know about that email address or AIM screen name");
		
		Account account;
		
		User user = identitySpider.lookupUserByResource(SystemViewpoint.getInstance(), resource);
		if (user == null && noAuthentication) {
			logger.warn("Creating new account for resource: {}", resource);
			account = accountSystem.createAccountFromResource(resource);
			user = account.getOwner();
		} else if (user == null) {
			throw new HumanVisibleException("Hmm, there doesn't seem to be an account with the "
					+ ((resource instanceof EmailResource) ? " email address '" : " screen name '") + address + "'");
		} else {
			account = accountSystem.lookupAccountByUser(user);
			assert account != null;
		}
		
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
