package com.dumbhippo.web;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.dumbhippo.web.EjbLink.NotLoggedInException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

public class SigninBean {

	private static Logger logger = Logger.getLogger(SigninBean.class);
	
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
	}

	public SigninBean() {
		EjbLink.injectFromFacesContext(this, Scope.NONE);
	}
	
	public boolean isValid() {
		return ejb.checkLoginFromFacesContext(this);
	}
	
	public String getLoggedInAs() {
		if (!ejb.checkLoginFromFacesContext(this))
			return null;
		return ejb.getLoggedInUser();
	}
}
