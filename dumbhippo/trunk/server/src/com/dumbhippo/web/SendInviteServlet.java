package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.jboss.util.NotImplementedException;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Invitation;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.InvitationSystem;

public class SendInviteServlet extends AbstractServlet {
	private static final Log logger = GlobalSetup.getLog(SendInviteServlet.class);
	
	static final long serialVersionUID = 1;

	private Invitation doSendInvite(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException{
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
		
		Invitation invitation = invitationSystem.createEmailInvitation(user, email);
		invitationSystem.sendEmailNotification(invitation, user);
		
		return invitation;
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		
		logRequest(request, "GET");
		
		try {
			Invitation invitation = doSendInvite(request, response);
			
			request.setAttribute("authKey", invitation.getAuthKey());
			request.getRequestDispatcher("/invitesent").forward(request, response);
		} catch (HttpException e) {
			logger.debug(e);
			e.send(response);
		}		
	}
}
