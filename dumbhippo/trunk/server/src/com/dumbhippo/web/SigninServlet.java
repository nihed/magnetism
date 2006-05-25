package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.SigninSystem;

public class SigninServlet extends AbstractServlet {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(SigninServlet.class);

	private static final long serialVersionUID = 1L;

	private AccountSystem accountSystem;
	private SigninSystem signinSystem;
	private Configuration config;
	
	@Override
	public void init() {
		accountSystem = WebEJBUtil.defaultLookup(AccountSystem.class);
		signinSystem = WebEJBUtil.defaultLookup(SigninSystem.class);
		config = WebEJBUtil.defaultLookup(Configuration.class);		
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
			throw new HumanVisibleException("Please enter an email or AIM address you use with your Mugshot account").setHtmlSuggestion("<a href=\"/who-are-you\">Go back</a>");
		}
		
		if (password == null)
			password = ""; // if you click send password but don't put one in
		
		Pair<Client,User> result = signinSystem.authenticatePassword(address, password, SigninBean.computeClientIdentifier(request));
		String host = config.getBaseUrl().getHost();
		LoginCookie loginCookie = new LoginCookie(host, result.getSecond().getId(), result.getFirst().getAuthKey());
		response.addCookie(loginCookie.getCookie());
		HttpSession sess = request.getSession(false);
		if (sess != null)
			sess.invalidate();
		
		if (next == null) {
			Account account = accountSystem.lookupAccountByUser(result.getSecond());
		
			if (account.isDisabled())
				next = "/we-miss-youE";
			else if (!account.getHasAcceptedTerms())
				next = "/download";
			else
				next = "/";
		}

		return redirectToNextPage(request, response, next, null);
	}

	@Override
	protected boolean requiresTransaction() {
		return true;
	}
}
