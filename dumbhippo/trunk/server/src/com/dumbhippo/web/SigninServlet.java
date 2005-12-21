package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.SigninSystem;

public class SigninServlet extends AbstractServlet {

	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(SigninServlet.class);

	private static final long serialVersionUID = 1L;

	private SigninSystem signinSystem;
	
	@Override
	public void init() {
		signinSystem = WebEJBUtil.defaultLookup(SigninSystem.class);
	}
	
	@Override
	protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException, HttpException, HumanVisibleException {

		String address = request.getParameter("address");
		if (address != null)
			address = address.trim();
		if (address.length() == 0)
			address = null;
		String password = request.getParameter("password");
		if (password != null)
			password = password.trim();
		if (password.length() == 0)
			password = null;
		boolean sendlink = request.getParameter("sendlink") != null;
		boolean checkpassword = request.getParameter("checkpassword") != null;

		String next = request.getParameter("next");
		if (next == null)
			next = "/home";
		
		if (address == null) {
			throw new HumanVisibleException("Please enter an email or AIM address you use with your DumbHippo account").setHtmlSuggestion("<a href=\"/who-are-you\">Go back</a>");
		}
		
		if (!(sendlink || checkpassword) || (sendlink && checkpassword)) {
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Bad request asked to both send link and check password");
		}
		
		// If the person clicks a button, then we'll definitely have the right sendlink or checkpassword 
		// setting. But if the person just hits enter, the browser picks one randomly (well I think it 
		// picks sendlink, but no guarantees). That then breaks us. For now we always go into 
		// check password mode if you put a password in the box, even if you click send link. FIXME
		
		if (checkpassword || password != null) {
			Pair<Client,User> result = signinSystem.authenticatePassword(address, password, SigninBean.computeClientIdentifier(request));
			LoginCookie loginCookie = new LoginCookie(result.getSecond().getId(), result.getFirst().getAuthKey());
			response.addCookie(loginCookie.getCookie());
			HttpSession sess = request.getSession(false);
			if (sess != null)
				sess.invalidate();
			
			redirectToNextPage(request, response, next, null);
		} else {
			try {
				signinSystem.sendSigninLink(address);
			} catch (HumanVisibleException e) {
				if (e.getHtmlSuggestion() == null)
					e.setHtmlSuggestion("<a href=\"/who-are-you\">Try again</a>");
				throw e;
			}
			request.setAttribute("address", address);
			request.getRequestDispatcher("/signinsent").forward(request, response);
		}
	}
}
