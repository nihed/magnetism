package com.dumbhippo.web.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.WebEJBUtil;

public class SendInviteServlet extends AbstractServlet {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(SendInviteServlet.class);
	
	static final long serialVersionUID = 1;

	private void doSendInvite(HttpServletRequest request, HttpServletResponse response)
	throws HttpException, HumanVisibleException, IOException, ServletException, RetryException {
		User user = doLogin(request);
		
		String message = request.getParameter("message");
		if (message != null)
			message = message.trim() + "\n";

		String subject = request.getParameter("subject");
		if (subject != null)
			subject = subject.trim();
		
		String email = request.getParameter("email");
		if (email != null)
			email = email.trim();
		
		if (email == null) 
			throw new HumanVisibleException("Missing email address");
		
		InvitationSystem invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		
		SigninBean signin = SigninBean.getForRequest(request);
		if (!signin.isValid())
			throw new HumanVisibleException("It looks like you aren't logged in");
		
		UserViewpoint viewpoint = (UserViewpoint) signin.getViewpoint();
		
		// we no longer need to check if the user has an invitation voucher to spend, 
		// because invitationSystem will take care of it
		String note;
		try {
			note = invitationSystem.sendEmailInvitation(viewpoint, null, email, subject, message);
		} catch (ValidationException e) {
			// the error page redirect kind of sucks, but if we do inline javascript 
			// validation it would only happen in weird manual-url-typing cases
			throw new HumanVisibleException("Invalid email address (" + e.getMessage() + ")");
		}
		
		request.setAttribute("email", email);
		request.setAttribute("remaining", invitationSystem.getInvitations(SystemViewpoint.getInstance(), user));
		request.setAttribute("note", note);
	}
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException, HttpException, HumanVisibleException, RetryException {
		doSendInvite(request, response);
		return "/invitesent";
	}

	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		return true;
	}
}
