package com.dumbhippo.web;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.web.CookieAuthentication.NotLoggedInException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

/**
 * An object that caches the currently logged in user, stored in JSF
 * and the HTTP session.
 * 
 * @author walters
 *
 */
public class SigninBean implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final Log logger = GlobalSetup.getLog(SigninBean.class);
	
	private Person user;
	
	private SigninBean(HttpServletRequest request) {
		try {
			user = CookieAuthentication.authenticate(request);
		} catch (BadTastingException e) {
			user = null;
		} catch (NotLoggedInException e) {
			user = null;
		}
		
		request.getSession().setAttribute("dumbhippo.signin", this);
		logger.debug("storing SigninBean in session, valid = " + isValid());
	}

	public static SigninBean getForRequest(HttpServletRequest request) {
		SigninBean result = null;
		
		if (request == null)
			throw new NullPointerException("null request");
		
		try {
			result = (SigninBean) request.getSession().getAttribute("dumbhippo.signin");
		} catch (ClassCastException e) {
			logger.debug("Value of dumbhippo.signin wasn't a SigninBean");
			result = null;
		}
		
		if (result == null)
			result = new SigninBean(request);
				
		return result;
	}
		
	public static String computeClientIdentifier(HttpServletRequest request) {
		StringBuilder ret = new StringBuilder();
		ret.append(request.getRemoteAddr());
		String agent = request.getHeader("user-agent");
		if (agent != null) {
			ret.append('/');
			ret.append(agent);
		}
		
		return ret.toString();
	}

	public static void setCookie(HttpServletResponse response, String personId, String authKey) {
		LoginCookie loginCookie = new LoginCookie(personId, authKey);
		response.addCookie(loginCookie.getCookie());
		logger.debug("Set cookie for personId = " + personId + " authKey = " + authKey);
	}

	public static void unsetCookie(HttpServletResponse response) {
		response.addCookie(LoginCookie.newDeleteCookie());
		logger.debug("Unset auth cookie");
	}

	public boolean isValid() {
		return user != null;
	}
	
	public Person getUser() {
		if (user == null) {
			throw new RuntimeException("Login required");
		}
		return user;
	}
}
