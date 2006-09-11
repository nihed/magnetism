package com.dumbhippo.server.impl;

import java.util.concurrent.Callable;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.botcom.BotTaskMessage;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.ResourceClaimToken;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.AimQueueSender;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class ClaimVerifierBean implements ClaimVerifier {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(ClaimVerifier.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PersonViewer personViewer;
	
	@EJB
	private TransactionRunner runner;

	@EJB
	private Configuration configuration;
	
	@EJB
	private Mailer mailer;
	
	@EJB
	private AimQueueSender aimQueueSender;
	
	private ResourceClaimToken getToken(final User user, final Resource resource) {	
		if (user == null && resource == null)
			throw new IllegalArgumentException("one of user/resource has to be non-null");
		
		try {
			return runner.runTaskThrowingConstraintViolation(new Callable<ResourceClaimToken>() {
				
				public ResourceClaimToken call() {
					Query q;
					
					q = em.createQuery("from ResourceClaimToken t where t.user = :user and t.resource = :resource");
					q.setParameter("user", user);
					q.setParameter("resource", resource);
					
					ResourceClaimToken token;
					try {
						token = (ResourceClaimToken) q.getSingleResult();
						if (token.isExpired()) {
							em.remove(token);
							throw new NoResultException("found expired token, making a new one");
						}
					} catch (NoResultException e) {
						token = new ResourceClaimToken(user, resource);
						em.persist(token);
					}
					
					return token;
				}
				
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
	}
	
	public String getAuthKey(final User user, final Resource resource) {
		return getToken(user, resource).getAuthKey();
	}
	
	private String getClaimVerifierLink(User user, Resource resource) throws HumanVisibleException {
		ResourceClaimToken token = getToken(user, resource);
		return token.getAuthURL(configuration.getPropertyFatalIfUnset(HippoProperty.BASEURL));
	}
	
	public void sendClaimVerifierLink(UserViewpoint viewpoint, User user, String address) throws HumanVisibleException {
		if (!viewpoint.isOfUser(user)) {
			throw new HumanVisibleException("You aren't signed in as the person you want to add an address for");
		}
		if (address.contains("@")) {
			EmailResource resource;
			try {
				resource = identitySpider.getEmail(address);
			} catch (ValidationException e) {
				throw new HumanVisibleException("That isn't a valid email address (" + e.getMessage() + ")");
			}
			String link = getClaimVerifierLink(user, resource);
			MimeMessage message = mailer.createMessage(Mailer.SpecialSender.VERIFIER, resource.getEmail());
			
			StringBuilder bodyText = new StringBuilder();
			XmlBuilder bodyHtml = new XmlBuilder();
			
			bodyText.append("\n");
			bodyText.append("Click this link to add '" + resource.getEmail() + "' to your account: " + link + "\n");
			bodyText.append("\n");
			
			bodyHtml.appendHtmlHead("");
			bodyHtml.append("<body>\n");
			bodyHtml.appendTextNode("a", "Click here to add '" + resource.getEmail() + "' to your account", "href", link);
			bodyHtml.append("</body>\n</html>\n");
			
			mailer.setMessageContent(message, "Add address '" + resource.getEmail() + "' to your Mugshot account",
					bodyText.toString(), bodyHtml.toString());
			mailer.sendMessage(message);
		} else {
			AimResource resource;
			try {
				resource = identitySpider.getAim(address);
			} catch (ValidationException e) {
				throw new HumanVisibleException(e.getMessage());
			}
			String link = getClaimVerifierLink(user, resource);
			XmlBuilder bodyHtml = new XmlBuilder();
			bodyHtml.appendTextNode("a", "Click to add '" + resource.getScreenName() + "' to your Mugshot account", "href", link);
			bodyHtml.appendTextNode("p", "NOTE: anyone with access to this AIM account will be able to log in to your Mugshot account.");
			
			BotTaskMessage message = new BotTaskMessage(null, resource.getScreenName(), bodyHtml.toString());
			aimQueueSender.sendMessage(message);
		}
	}
	
	public void verify(User user, ResourceClaimToken token, Resource resource) throws HumanVisibleException { 
		if (user != null) {
			if (!user.equals(token.getUser())) {
				Viewpoint viewpoint = new UserViewpoint(user);
				PersonView self = personViewer.getPersonView(viewpoint, user);
				PersonView other = personViewer.getPersonView(viewpoint, token.getUser());
				throw new HumanVisibleException("You are signed in as " + self.getName() 
						+ " but trying to change the account " + other.getName());
			}
		} else {
			user = token.getUser();
		}

		assert user != null;
		
		if (resource != null) {
			Resource tokenResource = token.getResource();
			if (tokenResource != null && !tokenResource.equals(resource)) {
				throw new HumanVisibleException(tokenResource.getHumanReadableString() + " should match " + resource.getHumanReadableString());
			}
		} else {
			resource = token.getResource();
		}
		
		if (resource == null) {
			// this should be a RuntimeException I guess since for a given resource type, we should guarantee
			// that we either have a trusted sender or that we recorded the resource prior to sending
			throw new HumanVisibleException("Something went wrong; could not figure out what address you are trying to add");
		}
		
		identitySpider.addVerifiedOwnershipClaim(user, resource);
	}
}
