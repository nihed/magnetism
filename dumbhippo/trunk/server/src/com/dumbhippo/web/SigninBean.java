package com.dumbhippo.web;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;

public class SigninBean {

	private static final Log logger = GlobalSetup.getLog(SigninBean.class);
	
	@Inject
	private EjbLink ejb;
	
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
	
	public SigninBean() {
		logger.debug("Constructing " + getClass().getCanonicalName());
		EjbLink.injectFromFacesContext(this, Scope.NONE);
	}
	
	public boolean isValid() {
		logger.debug("isValid() logged in user = " + ejb.getLoggedInUser());
		
		boolean loggedIn = ejb.checkLoginFromFacesContext(this);
		
		logger.debug("isValid() = " + loggedIn);
		
		return loggedIn;
	}
	
	public String getLoggedInAs() {
		if (!ejb.checkLoginFromFacesContext(this))
			return null;
		return ejb.getLoggedInUser();
	}
	
	public String doLogout() {
		unsetCookie();
		
		// FIXME we need to drop the Client object when we do this,
		// both to save our own disk space, and in case someone stole the 
		// cookie.
		
		return "main";
	}
}
