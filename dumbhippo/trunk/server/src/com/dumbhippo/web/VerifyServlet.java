package com.dumbhippo.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.LoginToken;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.ResourceClaimToken;
import com.dumbhippo.persistence.ToggleNoMailToken;
import com.dumbhippo.persistence.Token;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.LoginVerifier;
import com.dumbhippo.server.NoMailSystem;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.TokenExpiredException;
import com.dumbhippo.server.TokenSystem;
import com.dumbhippo.server.TokenUnknownException;

public class VerifyServlet extends AbstractServlet {
	private static final Log logger = GlobalSetup.getLog(VerifyServlet.class);
	
	static final long serialVersionUID = 1;

	private void doInvitationToken(HttpServletRequest request, HttpServletResponse response, InvitationToken invite) throws HttpException, ServletException, IOException {
		
		logger.debug("Processing invitation token " + invite);
		
		boolean disable = false;
		String disableParam = request.getParameter("disable");
		if (disableParam != null && disableParam.equals("true")) {
			disable = true;
		}
		
		// these two are used if we got here via RedirectServlet
		String urlParam = request.getParameter("url");
		String viewedPostId = request.getParameter("viewedPostId");
		
		InvitationSystem invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		
		User user = null;
		
		if (invite.isViewed()) {
			// have been to the link before, nothing to do unless we need to disable
			if (disable) {
				SigninBean signin = SigninBean.getForRequest(request);
				if (signin.isValid()) {
					user = signin.getUser();
					IdentitySpider spider = WebEJBUtil.defaultLookup(IdentitySpider.class);
					spider.setAccountDisabled(user, true);
					// now on to /welcome as normal
				} else {
					// just send them to the /account page where they can disable, not
					// worth some complicated solution; this will require a signin first
					redirectToNextPage(request, response, "/account", null);
				}
			}
		} else {
			// first time we've gone to the invite link
			Pair<Client,User> result = invitationSystem.viewInvitation(invite, SigninBean.computeClientIdentifier(request), disable);
			user = result.getSecond();
			SigninBean.setCookie(response, user.getId(), result.getFirst().getAuthKey());
		}
		
		if (viewedPostId != null && user != null) {
			PostingBoard postingBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
			postingBoard.postViewedBy(viewedPostId, user);
		}
		
		if (urlParam != null) {
			Configuration config = WebEJBUtil.defaultLookup(Configuration.class);
			
			// if the redirect url is to ourself (normally a group page) then 
			// we want to tell the page we're sending to that it's from here
			if (urlParam.startsWith(config.getPropertyFatalIfUnset(HippoProperty.BASEURL))) {
				try {
					URL url = new URL(urlParam);
					if (url.getQuery() == null)
						urlParam = urlParam + "?fromInvite=true";
					else
						urlParam = urlParam + "&fromInvite=true";
				} catch (MalformedURLException e) {
					// eh, just proceed, the redirect will fail maybe
					logger.debug("bad url redirect after invite: " + urlParam);
				}
			}
			
			response.sendRedirect(urlParam);
		} else {
			// this forwards to we-miss-you.jsp if the account is disabled
			redirectToNextPage(request, response, "/welcome", null);
		}
	}
	
	private void doResourceClaimToken(HttpServletRequest request, HttpServletResponse response, ResourceClaimToken token) throws HumanVisibleException, ServletException, IOException {
		
		logger.debug("Processing resource claim token " + token);
		
		ClaimVerifier verifier = WebEJBUtil.defaultLookup(ClaimVerifier.class);
		
		verifier.verify(null, token, null);
		redirectToNextPage(request, response, "/home", "Added address '" + token.getResource().getHumanReadableString() + "' to your account.");
	}

	private void doLoginToken(HttpServletRequest request, HttpServletResponse response, LoginToken token) throws HumanVisibleException, ServletException, IOException {
		
		logger.debug("Processing login token " + token);
		
		LoginVerifier verifier = WebEJBUtil.defaultLookup(LoginVerifier.class);		
		
		Pair<Client, Person> result;
		result = verifier.signIn(token, SigninBean.computeClientIdentifier(request));
		SigninBean.setCookie(response, result.getSecond().getId(), result.getFirst().getAuthKey());
		redirectToNextPage(request, response, "/home", null);
	}
	
	private void doToggleNoMailToken(HttpServletRequest request, HttpServletResponse response, ToggleNoMailToken token) throws HumanVisibleException, ServletException, IOException {
		logger.debug("Processing toggle email token " + token);
		
		NoMailSystem.Action action = NoMailSystem.Action.TOGGLE_MAIL;
		String actionParam = request.getParameter("action");
		if (actionParam != null) {
			if (actionParam.equals("enable"))
				action = NoMailSystem.Action.WANTS_MAIL;
			else if (actionParam.equals("disable"))
				action = NoMailSystem.Action.NO_MAIL_PLEASE;
			else
				logger.warn("Unknown action on email toggle '" + actionParam + "'");
		}
		NoMailSystem noMail = WebEJBUtil.defaultLookup(NoMailSystem.class);
		
		noMail.processAction(token.getEmail(), action);
		
		// store it in the session so we'll make a reasonable effort to 
		// not forget it; it's essential to re-set this every time 
		// so it doesn't get a stale auth key or enabled/disabled flag
		HttpSession session = request.getSession();
		session.setAttribute("dumbhippo.toggleNoMailToken", token);
		
		redirectToNextPage(request, response, "/mail", null);
	}
	
	@Override
	protected void wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws IOException, HumanVisibleException, HttpException, ServletException {
		String authKey = request.getParameter("authKey");
		if (authKey != null)
			authKey = authKey.trim();
		
		if (authKey == null || authKey.equals("")) 
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Authentication key not provided");
		
		TokenSystem tokenSystem = WebEJBUtil.defaultLookup(TokenSystem.class);
		Token token;
		try {
			token = tokenSystem.getTokenByKey(authKey);
		} catch (TokenExpiredException e) {
			logger.trace(e);
			if (e.getTokenClass() == InvitationToken.class)
				throw new HumanVisibleException("Your invitation to DumbHippo has expired! Ask the person who sent you this to invite you again.");
			else if (e.getTokenClass() == LoginToken.class)
				throw new HumanVisibleException("The sign-in link you followed has expired. You'll need to send a new one.").setHtmlSuggestion("<a href=\"/signin\">Go here</a>");
			else if (e.getTokenClass() == ResourceClaimToken.class)
				throw new HumanVisibleException("This verification link has expired. You'll need to add this address again.").setHtmlSuggestion("<a href=\"/account\">Go here</a>");
			else if (e.getTokenClass() == ToggleNoMailToken.class)
				throw new HumanVisibleException("For your security, the verification link for enabling or disabling email has expired after " + ToggleNoMailToken.getExpirationInDays() 
						+ " days. If you ever get mail from us again, it will have a new link valid for " + ToggleNoMailToken.getExpirationInDays() + " days.");
			else
				throw new HumanVisibleException("The link you followed has expired. You'll need to send a new one.").setHtmlSuggestion("<a href=\"/main\">Main</a>");
		} catch (TokenUnknownException e) {
			logger.trace(e);
			throw new HumanVisibleException("The link you followed is no longer valid.").setHtmlSuggestion("<a href=\"/main\">Main</a>");
		}
		
		assert token != null;
		
		if (token instanceof InvitationToken) {
			doInvitationToken(request, response, (InvitationToken) token);
		} else if (token instanceof LoginToken) {
			doLoginToken(request, response, (LoginToken) token);
		} else if (token instanceof ResourceClaimToken) {
			doResourceClaimToken(request, response, (ResourceClaimToken) token);
		} else if (token instanceof ToggleNoMailToken) {
			doToggleNoMailToken(request, response, (ToggleNoMailToken) token);
		} else {
			// missing handling of some token subclass
			throw new RuntimeException("VerifyServlet not handling token type " + token.getClass().getName());
		}
	}
}
