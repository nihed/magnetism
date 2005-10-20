package com.dumbhippo.web;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.web.CookieAuthentication.NotLoggedInException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

public class SigninBean {

	private static final Log logger = GlobalSetup.getLog(SigninBean.class);
	
	private Person user;
	
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
	}
		
	public static String computeClientIdentifier() {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletRequest req = (HttpServletRequest) ctx.getRequest();
		
		StringBuilder ret = new StringBuilder();
		ret.append(req.getRemoteAddr());
		String agent = req.getHeader("user-agent");
		if (agent != null) {
			ret.append('/');
			ret.append(agent);
		}
		
		return ret.toString();
	}

	public static void setCookie(String personId, String authKey) {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletResponse response = (HttpServletResponse) ctx.getResponse();
		LoginCookie loginCookie = new LoginCookie(personId, authKey);
		response.addCookie(loginCookie.getCookie());
		logger.debug("Set cookie for personId = " + personId + " authKey = " + authKey);
	}

	public static void unsetCookie() {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletResponse response = (HttpServletResponse) ctx.getResponse();
		response.addCookie(LoginCookie.newDeleteCookie());
		logger.debug("Unset auth cookie");
	}

	public boolean isValid() {
		return user != null;
	}
	
	public Person getUser() {
		return user;
	}
	
	public String doLogout() {
		unsetCookie();
		
		// FIXME we need to drop the Client object when we do this,
		// both to save our own disk space, and in case someone stole the 
		// cookie.
		
		return "main";
	}
}
