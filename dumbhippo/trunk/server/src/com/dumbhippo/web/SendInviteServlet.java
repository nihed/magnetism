package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.InvitationSystem;

public class SendInviteServlet extends AbstractServlet {
	private static final Log logger = GlobalSetup.getLog(SendInviteServlet.class);
	
	static final long serialVersionUID = 1;

	private void doSendInvite(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException, ServletException{
		Person user = doLogin(request, response, false);
		if (user == null)
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Not logged in");
		
		String fullName = request.getParameter("fullName");
		if (fullName != null)
			fullName = fullName.trim();

		String email = request.getParameter("email");
		if (email != null)
			email = email.trim();
		
		// FIXME: We shouldn't throw here, we should redirect back with a validation message
		if (fullName == null || fullName.equals("") || email == null || email.equals("")) 
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Name and email must be provided");
		
		InvitationSystem invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		
		InvitationToken invitation = invitationSystem.createEmailInvitation(user, email);
		invitationSystem.sendEmailNotification(invitation, user);
		
		request.setAttribute("fullName", fullName);
		request.setAttribute("email", email);
		request.setAttribute("authKey", invitation.getAuthKey());
		request.getRequestDispatcher("/invitesent").forward(request, response);
	}
	
	@Override
	protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException, HttpException, ErrorPageException {
		doSendInvite(request, response);
	}
}
