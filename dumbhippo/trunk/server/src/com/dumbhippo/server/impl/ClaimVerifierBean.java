package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.botcom.BotTaskMessage;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.ResourceClaimToken;
import com.dumbhippo.persistence.SubscriptionStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.persistence.XmppResource;
import com.dumbhippo.server.AimQueueSender;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.XmppMessageSender;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxCallable;
import com.dumbhippo.tx.TxUtils;

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
	private Configuration configuration;
	
	@EJB
	private Mailer mailer;
	
	@EJB
	private AimQueueSender aimQueueSender;
	
	@EJB
	private XmppMessageSender xmppMessageSender;
	
	private ResourceClaimToken getToken(final User user, final Resource resource) throws RetryException {	
		if (user == null && resource == null)
			throw new IllegalArgumentException("one of user/resource has to be non-null");
		
		return TxUtils.runNeedsRetry(new TxCallable<ResourceClaimToken>() {
			
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
						em.flush();
						throw new NoResultException("found expired token, making a new one");
					}
				} catch (NoResultException e) {
					token = new ResourceClaimToken(user, resource);
					em.persist(token);
				}
				
				return token;
			}
			
		});
	}
	
	private List<ResourceClaimToken> getOutstandingTokens(final Resource resource)  {
		// If the resource has already been claimed, there can be no outstanding tokens
		if (resource.getAccountClaim() != null)
			return Collections.emptyList();
		
		Query q;
		
		q = em.createQuery("SELECT t FROM ResourceClaimToken t WHERE t.resource = :resource");
		q.setParameter("resource", resource);

		List<ResourceClaimToken> results = new ArrayList<ResourceClaimToken>();
		for (Object o : q.getResultList()) {
			ResourceClaimToken token = (ResourceClaimToken)o;
			if (token.isValid()) 
				results.add(token);
		}
		
		return results;
	}
	
	public void cancelClaimToken(User user, Resource resource) {
		Query q = em.createQuery("SELECT t FROM ResourceClaimToken t WHERE t.user = :user AND t.resource = :resource");
		q.setParameter("user", user);
		q.setParameter("resource", resource);
		
		for (Object o : q.getResultList()) {
			ResourceClaimToken token = (ResourceClaimToken)o;
			token.setDeleted(true);
		}
	}
	
	public <T extends Resource> List<T> getPendingClaimedResources(User user, Class<T> klass) {
		Query q = em.createQuery("SELECT t FROM ResourceClaimToken t WHERE t.user = :user");
		q.setParameter("user", user);
		
		List<Resource> results = new ArrayList<Resource>();
		for (Object o : q.getResultList()) {
			ResourceClaimToken token = (ResourceClaimToken)o;
			if (token.getResource() != null &&
				token.getResource().getAccountClaim() == null &&
				klass.isAssignableFrom(token.getResource().getClass())) {
				if (token.isValid())
					results.add(token.getResource());
			}
		}
		
		return TypeUtils.castList(klass, results); 
	}
	
	public String getAuthKey(final User user, final Resource resource) throws RetryException {
		return getToken(user, resource).getAuthKey();
	}
	
	private String getClaimVerifierLink(Viewpoint viewpoint, User user, Resource resource) throws HumanVisibleException, RetryException {
		ResourceClaimToken token = getToken(user, resource);
		return token.getAuthURL(configuration.getBaseUrl(viewpoint));
	}
	
	public void sendClaimVerifierLinkEmail(UserViewpoint viewpoint, User user, String address) throws HumanVisibleException, RetryException {
		if (!viewpoint.isOfUser(user)) {
			throw new HumanVisibleException("You aren't signed in as the person you want to add an address for");
		}
		
		EmailResource resource;
		try {
			resource = identitySpider.getEmail(address);
		} catch (ValidationException e) {
			throw new HumanVisibleException("That isn't a valid email address (" + e.getMessage() + ")");
		}
		String link = getClaimVerifierLink(viewpoint, user, resource);
		MimeMessage message = mailer.createMessage(viewpoint, Mailer.SpecialSender.VERIFIER, resource.getEmail());
		
		StringBuilder bodyText = new StringBuilder();
		XmlBuilder bodyHtml = new XmlBuilder();
		
		bodyText.append("\n");
		bodyText.append("Click this link to add '" + resource.getEmail() + "' to your account: " + link + "\n");
		bodyText.append("\n");
		
		bodyHtml.appendHtmlHead("");
		bodyHtml.append("<body>\n");
		bodyHtml.appendTextNode("a", "Click here to add '" + resource.getEmail() + "' to your account", "href", link);
		bodyHtml.append("</body>\n</html>\n");
		
		mailer.setMessageContent(message, viewpoint.getSite(),
				"Add address '" + resource.getEmail() + "' to your " + viewpoint.getSite().getSiteName() + " account",
				bodyText.toString(), bodyHtml.toString(), false);
		mailer.sendMessage(message);
	}
	
	public void sendClaimVerifierLinkAim(UserViewpoint viewpoint, User user, String address) throws HumanVisibleException, RetryException {
		if (!viewpoint.isOfUser(user)) {
			throw new HumanVisibleException("You aren't signed in as the person you want to add an address for");
		}

		AimResource resource;
		try {
			resource = identitySpider.getAim(address);
		} catch (ValidationException e) {
			throw new HumanVisibleException(e.getMessage());
		}
		String link = getClaimVerifierLink(viewpoint, user, resource);
		XmlBuilder bodyHtml = new XmlBuilder();
		bodyHtml.appendTextNode("a", "Click to add '" + resource.getScreenName() + "' to your Mugshot account", "href", link);
		bodyHtml.appendTextNode("p", "NOTE: anyone with access to this AIM account will be able to log in to your Mugshot account.");
		
		BotTaskMessage message = new BotTaskMessage(null, resource.getScreenName(), bodyHtml.toString());
		aimQueueSender.sendMessage(message);
	}
	
	public void sendClaimVerifierLinkXmpp(UserViewpoint viewpoint, User user, String address) throws HumanVisibleException, RetryException {
		if (!viewpoint.isOfUser(user)) {
			throw new HumanVisibleException("You aren't signed in as the person you want to add an address for");
		}

		XmppResource resource;
		try {
			resource = identitySpider.getXmpp(address);
		} catch (ValidationException e) {
			throw new HumanVisibleException("That isn't a valid email address (" + e.getMessage() + ")");
		}

		ResourceClaimToken token = getToken(user, resource);
		String adminJid = configuration.getAdminJid(viewpoint);
		
		SubscriptionStatus status = xmppMessageSender.getSubscriptionStatus(adminJid, resource);
		if (!status.isSubscribedTo())
			xmppMessageSender.sendAdminPresence(resource.getJid(), adminJid, "subscribe");
		else
			sendXmppLink(viewpoint.getSite(), token);
	}
	

	public void verify(Viewpoint viewpoint, ResourceClaimToken token, Resource resource) throws HumanVisibleException { 
		User user;
		if (viewpoint instanceof UserViewpoint) {
			UserViewpoint userViewpoint = (UserViewpoint) viewpoint;
			if (!viewpoint.isOfUser(token.getUser())) {
				PersonView self = personViewer.getPersonView(viewpoint, userViewpoint.getViewer());
				PersonView other = personViewer.getPersonView(viewpoint, token.getUser());
				throw new HumanVisibleException("You are signed in as " + self.getName() 
						+ " but trying to change the account " + other.getName());
			}
			user = userViewpoint.getViewer();
		} else if (viewpoint instanceof SystemViewpoint){
			user = token.getUser();
		} else {
			user = null;
		}

		if (user == null)
			throw new RuntimeException("Invalid viewpoint passed to verify()");
		
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

	private void sendXmppLink(Site site, ResourceClaimToken token) {
		XmppResource resource = (XmppResource)token.getResource();
		StringBuilder sb = new StringBuilder();
		
		// We include the account's email (which is verified data) in the 
		// message to make it hard for someone else to fool you into adding your IM account
		// into their mugshot account
		String email = personViewer.getPersonView(SystemViewpoint.getInstance(), token.getUser()).getEmail().getEmail();
		
		sb.append("Click this link to add this IM account to the Mugshot account for ");
		sb.append(token.getUser().getNickname());
		sb.append("<");
		sb.append(email);
		sb.append(">;\n");
		sb.append(token.getAuthURL(configuration.getBaseUrl(site)) + "\n");
		
		xmppMessageSender.sendAdminMessage(resource.getJid(), configuration.getAdminJid(site), sb.toString());
	}

	public void sendQueuedXmppLinks(String friendedJid, XmppResource fromResource) {
		for (ResourceClaimToken token : getOutstandingTokens(fromResource)) {
			if (token.getResource() instanceof XmppResource) {
				logger.debug("Have an ResourceClaimToken for {} for user {} sending a claim link",
						     fromResource, token.getUser());				
				sendXmppLink(configuration.siteFromAdminJid(friendedJid), token);
			}
		}
	}
}
