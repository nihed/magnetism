package com.dumbhippo.web;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.server.InvitationSystem;

public class VerifyServlet extends AbstractServlet {
	private static final Log logger = GlobalSetup.getLog(VerifyServlet.class);
	
	static final long serialVersionUID = 1;

	void doVerify(HttpServletRequest request, HttpServletResponse response) throws HttpException {
		String authKey = request.getParameter("authKey");
		if (authKey != null)
			authKey = authKey.trim();
		
		if (authKey == null || authKey.equals("")) 
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Authentication key not provided");
		
		InvitationSystem invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);		
		InvitationToken invite = invitationSystem.lookupInvitationByKey(authKey);
		
		Collection<String> inviterNames;
		if (invite != null)
			inviterNames = invitationSystem.getInviterNames(invite);
		else
			inviterNames = null;

		if (inviterNames == null)  
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Authentication key verification failed");
		
		Client client = invitationSystem.viewInvitation(invite, SigninBean.computeClientIdentifier(request));
		SigninBean.setCookie(response, invite.getResultingPerson().getId(), client.getAuthKey());
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		
		logRequest(request, "GET");
	
		try {
			doVerify(request, response);
						
			String url = response.encodeRedirectURL("/welcome");
			response.sendRedirect(url);
		} catch (HttpException e) {
			logger.debug(e);
			e.send(response);			
		}
	}
}
