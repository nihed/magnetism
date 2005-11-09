package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.LoginToken;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.ResourceClaimToken;
import com.dumbhippo.persistence.Token;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.ClaimVerifierException;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.LoginVerifier;
import com.dumbhippo.server.LoginVerifierException;
import com.dumbhippo.server.TokenSystem;

public class VerifyServlet extends AbstractServlet {
	private static final Log logger = GlobalSetup.getLog(VerifyServlet.class);
	
	static final long serialVersionUID = 1;

	private void doInvitationToken(HttpServletRequest request, HttpServletResponse response, InvitationToken invite) throws HttpException, ServletException, IOException {
		
		logger.debug("Processing invitation token " + invite);
		
		InvitationSystem invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		
		Pair<Client,Person> result = invitationSystem.viewInvitation(invite, SigninBean.computeClientIdentifier(request));
		SigninBean.setCookie(response, result.getSecond().getId(), result.getFirst().getAuthKey());
		redirectToNextPage(request, response, "/welcome", null);
	}
	
	private void doResourceClaimToken(HttpServletRequest request, HttpServletResponse response, ResourceClaimToken token) throws ErrorPageException, ServletException, IOException {
		
		logger.debug("Processing resource claim token " + token);
		
		ClaimVerifier verifier = WebEJBUtil.defaultLookup(ClaimVerifier.class);
		
		try {
			verifier.verify(null, token, null);
		} catch (ClaimVerifierException e) {
			throw new ErrorPageException(e.getMessage());
		}
		redirectToNextPage(request, response, "/home", "Added address '" + token.getResource().getHumanReadableString() + "' to your account.");
	}

	private void doLoginToken(HttpServletRequest request, HttpServletResponse response, LoginToken token) throws ErrorPageException, ServletException, IOException {
		
		logger.debug("Processing login token " + token);
		
		LoginVerifier verifier = WebEJBUtil.defaultLookup(LoginVerifier.class);		
		
		Pair<Client, Person> result;
		try {
			result = verifier.signIn(token, SigninBean.computeClientIdentifier(request));
		} catch (LoginVerifierException e) {
			throw new ErrorPageException(e.getMessage());
		}
		SigninBean.setCookie(response, result.getSecond().getId(), result.getFirst().getAuthKey());
		redirectToNextPage(request, response, "/home", null);
	}
	
	@Override
	protected void wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ErrorPageException, HttpException, ServletException {
		String authKey = request.getParameter("authKey");
		if (authKey != null)
			authKey = authKey.trim();
		
		if (authKey == null || authKey.equals("")) 
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Authentication key not provided");
		
		TokenSystem tokenSystem = WebEJBUtil.defaultLookup(TokenSystem.class);
		Token token = tokenSystem.lookupTokenByKey(authKey);
		
		if (token instanceof InvitationToken) {
			doInvitationToken(request, response, (InvitationToken) token);
		} else if (token instanceof LoginToken) {
			doLoginToken(request, response, (LoginToken) token);
		} else if (token instanceof ResourceClaimToken) {
			doResourceClaimToken(request, response, (ResourceClaimToken) token);
		} else {
			// token == null or we aren't handling a token subclass
			throw new ErrorPageException("The link you followed has expired. You'll need to send a new one.");
		}	
	}
}
