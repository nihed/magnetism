package com.dumbhippo.web.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.SigninSystem;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.WebEJBUtil;

public class SigninServlet extends AbstractServlet {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(SigninServlet.class);

	private static final long serialVersionUID = 1L;

	private SigninSystem signinSystem;
	
	@Override
	public void init() {
		signinSystem = WebEJBUtil.defaultLookup(SigninSystem.class);
	}
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException, HttpException, HumanVisibleException {

		String address = request.getParameter("address");
		if (address != null) {
			address = address.trim();
			if (address.length() == 0)
				address = null;
		}
		String password = request.getParameter("password");
		if (password != null) {
			password = password.trim();
			if (password.length() == 0)
				password = null;
		}
		String next = request.getParameter("next");
		
		if (address == null) {
			String link = "/who-are-you";
			if (next != null)
				link = link + "?next=" + next;
			throw new HumanVisibleException("Please enter an email address you use with your Mugshot account").setHtmlSuggestion("<a href=\"" + link + "\">Go back</a>");
		}
		
		if (password == null)
			password = ""; // if you click send password but don't put one in
		
		Client client = signinSystem.authenticatePassword(address, password, SigninBean.computeClientIdentifier(request));

		HttpSession sess = request.getSession(false);
		if (sess != null)
			sess.invalidate();

		String defaultNext = SigninBean.initializeAuthentication(request, response, client);
		
		if (next == null)
			next = defaultNext;

		return redirectToNextPage(request, response, next, null);
	}

	@Override
	protected boolean requiresTransaction() {
		return true;
	}
}
