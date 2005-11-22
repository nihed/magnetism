package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.InvitationSystem;

public class SendInviteServlet extends AbstractServlet {
	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(SendInviteServlet.class);
	
	static final long serialVersionUID = 1;

	private void doSendInvite(HttpServletRequest request, HttpServletResponse response)
	throws HttpException, HumanVisibleException, IOException, ServletException {
		User user = doLogin(request, response, false);
		if (user == null)
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Not logged in");
		
		String fullName = request.getParameter("fullName");
		if (fullName != null)
			fullName = fullName.trim();

		String email = request.getParameter("email");
		if (email != null)
			email = email.trim();
		
		// the error page redirect kind of sucks, but if we do inline javascript 
		// validation it would only happen in weird manual-url-typing cases
		if (fullName == null || fullName.equals("") || email == null || email.equals("")) 
			throw new HumanVisibleException("Missing either a name or email address");
		
		InvitationSystem invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		
		// one last check, because createEmailInvitation doesn't check it
		if (invitationSystem.getInvitations(user) < 1) {
			throw new HumanVisibleException("You can't invite anyone else for now");
		}
		
		String note = invitationSystem.sendEmailInvitation(user, email);
		
		request.setAttribute("fullName", fullName);
		request.setAttribute("email", email);
		request.setAttribute("remaining", invitationSystem.getInvitations(user));
		request.setAttribute("note", note);
		request.getRequestDispatcher("/invitesent").forward(request, response);
	}
	
	@Override
	protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException, HttpException, HumanVisibleException {
		doSendInvite(request, response);
	}
}
