package com.dumbhippo.web;

import java.io.Serializable;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.TestGlue;
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
	
	/**
	 * Should only be called by JSF.
	 */
	public SigninBean() {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletRequest req = (HttpServletRequest) ctx.getRequest();
		try {
			user = CookieAuthentication.authenticate(req);
		} catch (BadTastingException e) {
			user = null;
		} catch (NotLoggedInException e) {
			user = null;
		}
		// We store ourselves twice in the session so that
		// the XML-RPC server can access the object under this name
		// too
		HttpSession session = (HttpSession) ctx.getSession(true);
		session.setAttribute("dumbhippo.signin", this);
		logger.debug("storing signin in session");
	}

	public static SigninBean getFromHttpSession(HttpSession sess) {
		if (sess == null)
			return null;
		return (SigninBean) sess.getAttribute("dumbhippo.signin");
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
	
	public String doLogout() {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletResponse response = (HttpServletResponse) ctx.getResponse();		
		unsetCookie(response);	
		HttpSession session = (HttpSession) ctx.getSession(false);
		if (session != null)
			session.invalidate();		
		
		// FIXME we need to drop the Client object when we do this,
		// both to save our own disk space, and in case someone stole the 
		// cookie.
		
		return "main";
	}
}
